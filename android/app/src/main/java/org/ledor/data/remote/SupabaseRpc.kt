package org.ledor.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface RpcResult<out T> {
    data class Ok<T>(val value: T) : RpcResult<T>
    /** Бизнес-ошибка из конверта {"ok":false,"error":"…"}. */
    data class Error(val code: String, val attemptsLeft: Int? = null, val retryAfterSec: Int? = null) : RpcResult<Nothing>
    data class Failure(val kind: FailureKind) : RpcResult<Nothing>
}

enum class FailureKind { NETWORK, SERVER, NOT_CONFIGURED }

@Serializable
data class RemoteList(val id: String, val name: String, val revision: Long)

@Serializable
data class RemoteEvent(
    val id: String,
    val deleted: Boolean = false,
    val rev: Long = 0,
    val title: String = "",
    val type: String = "OTHER",
    @SerialName("custom_label") val customLabel: String? = null,
    val recurring: Boolean = true,
    @SerialName("observe_by") val observeBy: String = "HEBREW",
    @SerialName("input_calendar") val inputCalendar: String = "GREGORIAN",
    @SerialName("g_year") val gYear: Int? = null,
    @SerialName("g_month") val gMonth: Int? = null,
    @SerialName("g_day") val gDay: Int? = null,
    @SerialName("after_sunset") val afterSunset: Boolean = false,
    @SerialName("h_year") val hYear: Int? = null,
    @SerialName("h_month") val hMonth: String? = null,
    @SerialName("h_day") val hDay: Int? = null,
    val note: String? = null,
)

@Serializable
data class JoinResponse(
    val list: RemoteList,
    val role: String,
    val token: String,
    @SerialName("read_pin") val readPin: String? = null,
)

@Serializable
data class PullResponse(
    val list: RemoteList,
    val role: String,
    @SerialName("read_pin") val readPin: String? = null,
    val events: List<RemoteEvent> = emptyList(),
)

@Serializable
data class Rejected(val id: String, val reason: String)

@Serializable
data class PushResponse(val revision: Long, val accepted: List<String> = emptyList(), val rejected: List<Rejected> = emptyList())

/** Тонкий клиент к RPC-функциям Supabase (docs/04-backend-supabase.md §3, §5). */
class SupabaseRpc(private val baseUrl: String, private val apiKey: String, client: OkHttpClient? = null) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val http = client ?: OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }
    private val mediaType = "application/json".toMediaType()

    private suspend fun call(function: String, body: JsonObject): RpcResult<JsonObject> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext RpcResult.Failure(FailureKind.NOT_CONFIGURED)
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/rest/v1/rpc/" + function)
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(mediaType))
            .build()
        try {
            http.newCall(request).execute().use { resp ->
                if (resp.code != 200) return@withContext RpcResult.Failure(FailureKind.SERVER)
                val obj = runCatching { json.parseToJsonElement(resp.body.string()).jsonObject }.getOrNull()
                    ?: return@withContext RpcResult.Failure(FailureKind.SERVER)
                if (obj["ok"]?.jsonPrimitive?.booleanOrNull == true) RpcResult.Ok(obj)
                else RpcResult.Error(
                    code = obj["error"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
                    attemptsLeft = obj["attempts_left"]?.jsonPrimitive?.intOrNull,
                    retryAfterSec = obj["retry_after_sec"]?.jsonPrimitive?.intOrNull,
                )
            }
        } catch (e: IOException) {
            RpcResult.Failure(FailureKind.NETWORK)
        }
    }

    private inline fun <reified T> RpcResult<JsonObject>.decode(): RpcResult<T> = when (this) {
        is RpcResult.Ok -> runCatching { RpcResult.Ok(json.decodeFromJsonElement<T>(value)) }
            .getOrElse { RpcResult.Failure(FailureKind.SERVER) }
        is RpcResult.Error -> this
        is RpcResult.Failure -> this
    }

    private fun <T> RpcResult<JsonObject>.map(f: (JsonObject) -> T): RpcResult<T> = when (this) {
        is RpcResult.Ok -> runCatching { RpcResult.Ok(f(value)) }.getOrElse { RpcResult.Failure(FailureKind.SERVER) }
        is RpcResult.Error -> this
        is RpcResult.Failure -> this
    }

    suspend fun listExists(name: String): RpcResult<Boolean> =
        call("list_exists", buildJsonObject { put("p_name", name) }).map { it["exists"]!!.jsonPrimitive.booleanOrNull == true }

    suspend fun createList(name: String, readPin: String, masterPin: String): RpcResult<JoinResponse> =
        call("create_list", buildJsonObject {
            put("p_name", name); put("p_read_pin", readPin); put("p_master_pin", masterPin)
        }).decode()

    suspend fun joinList(name: String, pin: String): RpcResult<JoinResponse> =
        call("join_list", buildJsonObject { put("p_name", name); put("p_pin", pin) }).decode()

    suspend fun pullChanges(listId: String, token: String, since: Long): RpcResult<PullResponse> =
        call("pull_changes", buildJsonObject {
            put("p_list_id", listId); put("p_token", token); put("p_since", since)
        }).decode()

    suspend fun pushEvents(listId: String, token: String, events: List<RemoteEvent>): RpcResult<PushResponse> =
        call("push_events", buildJsonObject {
            put("p_list_id", listId); put("p_token", token)
            put("p_events", json.encodeToJsonElement<List<RemoteEvent>>(events) as JsonElement)
        }).decode()

    suspend fun leaveList(listId: String, token: String): RpcResult<Unit> =
        call("leave_list", buildJsonObject { put("p_list_id", listId); put("p_token", token) }).map { }

    /** Отзыв (B-2). meta — техданные, только если пользователь согласился их приложить. */
    suspend fun submitFeedback(message: String, contact: String?, meta: Map<String, String>?): RpcResult<Unit> =
        call("submit_feedback", buildJsonObject {
            put("p_message", message); put("p_contact", contact)
            put("p_meta", buildJsonObject { meta?.forEach { (k, v) -> put(k, v) } })
        }).map { }

    suspend fun deleteList(listId: String, token: String, masterPin: String): RpcResult<Unit> =
        call("delete_list", buildJsonObject {
            put("p_list_id", listId); put("p_token", token); put("p_master_pin", masterPin)
        }).map { }
}
