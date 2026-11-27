package org.ledor.data.repo

import androidx.room.withTransaction
import org.ledor.data.local.AppDatabase
import org.ledor.data.local.EventEntity
import org.ledor.data.local.LOCAL_LIST_ID
import org.ledor.data.local.ListEntity
import org.ledor.data.local.ListKind
import org.ledor.data.local.ListRole
import org.ledor.data.local.ListState
import org.ledor.data.local.ListWithCount
import org.ledor.data.local.SyncState
import org.ledor.data.local.toDomain
import org.ledor.data.local.toEntity
import org.ledor.data.prefs.SettingsStore
import org.ledor.data.remote.JoinResponse
import org.ledor.data.remote.RemoteEvent
import org.ledor.data.remote.FailureKind
import org.ledor.data.remote.RpcResult
import org.ledor.data.remote.SupabaseRpc
import org.ledor.domain.event.Event
import org.ledor.domain.lists.ListNameRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Вызывается после любой локальной правки: перестроить будильники и (для облачных) отправить изменения. */
fun interface ChangeListener { fun onChanged(cloudListId: String?) }

class EventRepository(private val db: AppDatabase, private val listener: ChangeListener) {
    fun observeAll(): Flow<List<Event>> = db.events().observeVisible().map { l -> l.map { it.toDomain() } }
    fun observe(id: String): Flow<Event?> = db.events().observe(id).map { it?.toDomain() }
    suspend fun get(id: String): Event? = db.events().get(id)?.takeIf { it.syncState != SyncState.PENDING_DELETE }?.toDomain()
    suspend fun getAll(): List<Event> = db.events().getVisible().map { it.toDomain() }

    suspend fun save(event: Event) {
        val list = db.lists().get(event.listId) ?: error("list missing")
        require(list.canEdit)
        val existing = db.events().get(event.id)
        db.withTransaction {
            if (existing != null && existing.listId != event.listId) {
                // Перенос в другой список = удаление в старом + создание с новым id.
                deleteInternal(existing)
                db.events().upsert(event.copy(id = UUID.randomUUID().toString()).toEntity(stateFor(list)))
            } else {
                db.events().upsert(event.toEntity(stateFor(list), rev = existing?.rev ?: 0))
            }
        }
        if (existing != null && existing.listId != event.listId) notify(existing.listId)
        notify(event.listId)
    }

    suspend fun delete(id: String) {
        val e = db.events().get(id) ?: return
        deleteInternal(e)
        notify(e.listId)
    }

    private suspend fun deleteInternal(e: EventEntity) {
        val list = db.lists().get(e.listId)
        if (list == null || !list.isCloud || (e.rev == 0L && e.syncState == SyncState.PENDING_UPSERT)) db.events().delete(e.id)
        else db.events().markPendingDelete(e.id, System.currentTimeMillis())
    }

    private fun stateFor(list: ListEntity) = if (list.isCloud) SyncState.PENDING_UPSERT else SyncState.SYNCED

    private suspend fun notify(listId: String) {
        val list = db.lists().get(listId)
        listener.onChanged(if (list?.isCloud == true) listId else null)
    }
}

sealed interface JoinOutcome {
    data class Joined(val list: ListEntity, val imported: Int) : JoinOutcome
}

class ListRepository(
    private val db: AppDatabase,
    private val rpc: SupabaseRpc,
    private val sync: SyncEngine,
    private val listener: ChangeListener,
) {
    val cloudConfigured get() = rpc.isConfigured

    fun observeLists(): Flow<List<ListWithCount>> = db.lists().observeWithCounts()
    fun observeAll(): Flow<List<ListEntity>> = db.lists().observeAll()
    fun observe(id: String): Flow<ListEntity?> = db.lists().observe(id)
    suspend fun get(id: String) = db.lists().get(id)

    suspend fun checkExists(name: String): RpcResult<Boolean> = rpc.listExists(ListNameRules.clean(name))

    suspend fun create(name: String, readPin: String, masterPin: String, moveLocalEvents: Boolean): RpcResult<ListEntity> {
        return when (val r = rpc.createList(ListNameRules.clean(name), readPin, masterPin)) {
            is RpcResult.Ok -> {
                val entity = saveJoined(r.value, masterPin)
                if (moveLocalEvents) {
                    db.withTransaction {
                        for (e in db.events().visibleInList(LOCAL_LIST_ID)) {
                            db.events().delete(e.id)
                            db.events().upsert(e.copy(id = UUID.randomUUID().toString(), listId = entity.id,
                                syncState = SyncState.PENDING_UPSERT, rev = 0, updatedAt = System.currentTimeMillis()))
                        }
                    }
                    sync.syncList(entity.id)
                }
                listener.onChanged(null)
                RpcResult.Ok(entity)
            }
            is RpcResult.Error -> r
            is RpcResult.Failure -> r
        }
    }

    suspend fun join(name: String, pin: String): RpcResult<JoinOutcome> {
        return when (val r = rpc.joinList(ListNameRules.clean(name), pin)) {
            is RpcResult.Ok -> {
                val entity = saveJoined(r.value, if (r.value.role == ListRole.EDITOR) pin else null)
                sync.syncList(entity.id, full = true)
                listener.onChanged(null)
                val count = db.events().visibleInList(entity.id).size
                RpcResult.Ok(JoinOutcome.Joined(db.lists().get(entity.id) ?: entity, count))
            }
            is RpcResult.Error -> r
            is RpcResult.Failure -> r
        }
    }

    private suspend fun saveJoined(resp: JoinResponse, masterPin: String?): ListEntity {
        val existing = db.lists().get(resp.list.id)
        val entity = (existing ?: ListEntity(
            id = resp.list.id, kind = ListKind.CLOUD, name = resp.list.name, role = resp.role,
            colorIndex = (db.lists().getAll().size) % 8, sortOrder = db.lists().maxSortOrder() + 1,
        )).copy(
            name = resp.list.name,
            role = resp.role,
            state = ListState.ACTIVE,
            accessToken = resp.token,
            readPin = resp.readPin ?: existing?.readPin,
            masterPin = masterPin ?: existing?.masterPin,
        )
        db.lists().upsert(entity)
        return entity
    }

    suspend fun disconnect(listId: String) {
        val list = db.lists().get(listId) ?: return
        if (!list.isCloud) return
        list.accessToken?.let { rpc.leaveList(listId, it) }
        db.lists().delete(listId)
        listener.onChanged(null)
    }

    suspend fun deleteFromCloud(listId: String, masterPin: String): RpcResult<Unit> {
        val list = db.lists().get(listId) ?: return RpcResult.Ok(Unit)
        val token = list.accessToken ?: return RpcResult.Error("UNAUTHORIZED")
        val r = rpc.deleteList(listId, token, masterPin)
        if (r is RpcResult.Ok || (r is RpcResult.Error && r.code == "LIST_DELETED")) {
            db.lists().delete(listId)
            listener.onChanged(null)
            return RpcResult.Ok(Unit)
        }
        return r
    }

    /**
     * B-40: новые ПИН-коды. На этом телефоне они сразу сохраняются; остальные подключённые телефоны работают дальше по
     * своим токенам и получат новый ПИН-код для чтения при синхронизации.
     */
    suspend fun changePins(listId: String, currentMasterPin: String, newReadPin: String, newMasterPin: String): RpcResult<Unit> {
        val list = db.lists().get(listId) ?: return RpcResult.Error("NOT_FOUND")
        val token = list.accessToken ?: return RpcResult.Error("UNAUTHORIZED")
        val r = rpc.changePins(listId, token, currentMasterPin, newReadPin, newMasterPin)
        if (r is RpcResult.Ok) db.lists().get(listId)?.let { db.lists().upsert(it.copy(readPin = newReadPin, masterPin = newMasterPin)) }
        return r
    }

    suspend fun setNotifyEnabled(listId: String, enabled: Boolean) {
        db.lists().get(listId)?.let { db.lists().upsert(it.copy(notifyEnabled = enabled)) }
        listener.onChanged(null)
    }

    suspend fun moveLocalEventsTo(targetListId: String) {
        val target = db.lists().get(targetListId) ?: return
        if (!target.canEdit) return
        db.withTransaction {
            for (e in db.events().visibleInList(LOCAL_LIST_ID)) {
                db.events().delete(e.id)
                db.events().upsert(e.copy(id = UUID.randomUUID().toString(), listId = targetListId,
                    syncState = if (target.isCloud) SyncState.PENDING_UPSERT else SyncState.SYNCED, rev = 0))
            }
        }
        listener.onChanged(targetListId)
    }
}

data class SyncReport(val ok: Int, val failed: Int, val networkError: Boolean)

class SyncEngine(
    private val db: AppDatabase,
    private val rpc: SupabaseRpc,
    private val settings: SettingsStore,
    private val deletedNotice: (String) -> String,
    private val afterSync: suspend () -> Unit,
    /** B-40: сменили Мастер-ПИН — этот телефон больше не редактор. */
    private val demotedNotice: (String) -> String = { it },
) {
    private val mutex = Mutex()

    suspend fun syncAll(): SyncReport = mutex.withLock {
        var ok = 0; var failed = 0; var net = false
        for (l in db.lists().getAll().filter { it.isCloud }) {
            when (syncInternal(l.id, false)) {
                Outcome.OK -> ok++
                Outcome.NETWORK -> { failed++; net = true }
                Outcome.OTHER -> failed++
            }
        }
        settings.setLastSyncAttempt(System.currentTimeMillis())
        afterSync()
        SyncReport(ok, failed, net)
    }

    suspend fun syncList(listId: String, full: Boolean = false): SyncReport = mutex.withLock {
        val r = syncInternal(listId, full)
        afterSync()
        SyncReport(if (r == Outcome.OK) 1 else 0, if (r == Outcome.OK) 0 else 1, r == Outcome.NETWORK)
    }

    private enum class Outcome { OK, NETWORK, OTHER }

    private suspend fun syncInternal(listId: String, full: Boolean): Outcome {
        if (!rpc.isConfigured) return Outcome.OTHER
        var list = db.lists().get(listId) ?: return Outcome.OTHER
        val token = list.accessToken ?: return Outcome.OTHER
        if (list.state == ListState.ACCESS_LOST) return Outcome.OTHER

        // 1. Отправка
        if (list.role == ListRole.EDITOR) {
            val pending = db.events().pending(listId)
            for (chunk in pending.chunked(200)) {
                when (val r = rpc.pushEvents(listId, token, chunk.map { it.toRemote() })) {
                    is RpcResult.Ok -> {
                        val accepted = r.value.accepted.toSet()
                        for (e in chunk) {
                            if (e.id !in accepted && r.value.rejected.none { it.id == e.id }) continue
                            if (e.syncState == SyncState.PENDING_DELETE || e.id !in accepted) db.events().delete(e.id)
                            else db.events().upsert(e.copy(syncState = SyncState.SYNCED, rev = r.value.revision))
                        }
                    }
                    is RpcResult.Error -> return handleError(list, r.code)
                    is RpcResult.Failure -> return if (r.kind == FailureKind.NETWORK) Outcome.NETWORK else Outcome.OTHER
                }
            }
        }

        // 2. Получение
        val since = if (full) 0 else list.lastSyncedRev
        return when (val r = rpc.pullChanges(listId, token, since)) {
            is RpcResult.Ok -> {
                val resp = r.value
                db.withTransaction {
                    if (since == 0L) db.events().deleteSyncedInList(listId)
                    val pendingIds = db.events().pending(listId).map { it.id }.toSet()
                    for (re in resp.events) {
                        if (re.id in pendingIds) continue
                        if (re.deleted) db.events().delete(re.id)
                        else db.events().upsert(re.toEntity(listId))
                    }
                    // B-40: стал читателем — Мастер-ПИН на телефоне больше не действует, его не хранить и не предлагать.
                    val demoted = list.role == ListRole.EDITOR && resp.role == ListRole.READER
                    if (demoted) settings.pushNotice(demotedNotice(resp.list.name))
                    list = list.copy(
                        name = resp.list.name, role = resp.role, readPin = resp.readPin ?: list.readPin,
                        masterPin = if (resp.role == ListRole.READER) null else list.masterPin,
                        lastSyncedRev = resp.list.revision, lastSyncAt = System.currentTimeMillis(),
                    )
                    db.lists().upsert(list)
                }
                Outcome.OK
            }
            is RpcResult.Error -> handleError(list, r.code)
            is RpcResult.Failure -> if (r.kind == FailureKind.NETWORK) Outcome.NETWORK else Outcome.OTHER
        }
    }

    private suspend fun handleError(list: ListEntity, code: String): Outcome {
        when (code) {
            "LIST_DELETED" -> {
                db.lists().delete(list.id)
                settings.pushNotice(deletedNotice(list.name))
            }
            "UNAUTHORIZED" -> db.lists().upsert(list.copy(state = ListState.ACCESS_LOST))
            "FORBIDDEN" -> {
                db.events().deletePendingInList(list.id)
                if (list.role == ListRole.EDITOR) settings.pushNotice(demotedNotice(list.name))
                db.lists().upsert(list.copy(role = ListRole.READER, masterPin = null, lastSyncedRev = 0))
            }
        }
        return Outcome.OTHER
    }
}

private fun EventEntity.toRemote() = RemoteEvent(
    id = id, deleted = syncState == SyncState.PENDING_DELETE, rev = rev, title = title, type = type,
    customLabel = customLabel, recurring = recurring, observeBy = observeBy, inputCalendar = inputCalendar,
    gYear = gYear, gMonth = gMonth, gDay = gDay, afterSunset = afterSunset,
    hYear = hYear, hMonth = hMonth, hDay = hDay, note = note, monthly = monthly,
)

private fun RemoteEvent.toEntity(listId: String) = EventEntity(
    id = id, listId = listId, rev = rev, syncState = SyncState.SYNCED, title = title, type = type,
    customLabel = customLabel, recurring = recurring, observeBy = observeBy, inputCalendar = inputCalendar,
    gYear = gYear, gMonth = gMonth, gDay = gDay, afterSunset = afterSunset,
    hYear = hYear, hMonth = hMonth, hDay = hDay, note = note, monthly = monthly,
)
