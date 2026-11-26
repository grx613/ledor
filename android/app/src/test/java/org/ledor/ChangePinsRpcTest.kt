package org.ledor

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.ledor.data.remote.RpcResult
import org.ledor.data.remote.SupabaseRpc

/** B-40: приложение вызывает change_pins с нужными параметрами и понимает ответ сервера. */
class ChangePinsRpcTest {
    private var path = ""
    private var body = ""
    private fun rpc(answer: String) = SupabaseRpc("https://example.org", "key", OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        path = chain.request().url.encodedPath
        body = Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("")
            .body(answer.toResponseBody("application/json".toMediaType())).build()
    }).build())

    @Test fun sendsAllFiveParameters() = runBlocking {
        val r = rpc("""{"ok":true,"read_pin":"0770"}""").changePins("L1", "T1", "61803398", "0770", "27182818")
        assertEquals(RpcResult.Ok(Unit), r)
        assertEquals("/rest/v1/rpc/change_pins", path)
        val p = Json.parseToJsonElement(body).jsonObject.mapValues { it.value.jsonPrimitive.content }
        assertEquals(mapOf("p_list_id" to "L1", "p_token" to "T1", "p_master_pin" to "61803398", "p_new_read_pin" to "0770", "p_new_master_pin" to "27182818"), p)
    }

    @Test fun wrongMasterPinIsReported() = runBlocking {
        val r = rpc("""{"ok":false,"error":"WRONG_PIN","attempts_left":4}""").changePins("L1", "T1", "11111111", "0770", "27182818")
        assertEquals(RpcResult.Error("WRONG_PIN", attemptsLeft = 4), r)
    }
}
