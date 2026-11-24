package org.ledor.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

const val LOCAL_LIST_ID = "local"

object ListKind { const val LOCAL = "LOCAL"; const val CLOUD = "CLOUD" }
object ListRole { const val OWNER = "OWNER"; const val READER = "READER"; const val EDITOR = "EDITOR" }
object ListState { const val ACTIVE = "ACTIVE"; const val ACCESS_LOST = "ACCESS_LOST" }
object SyncState { const val SYNCED = "SYNCED"; const val PENDING_UPSERT = "PENDING_UPSERT"; const val PENDING_DELETE = "PENDING_DELETE" }

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    val role: String,
    val state: String = ListState.ACTIVE,
    @ColumnInfo(name = "read_pin") val readPin: String? = null,
    @ColumnInfo(name = "master_pin") val masterPin: String? = null,
    @ColumnInfo(name = "access_token") val accessToken: String? = null,
    @ColumnInfo(name = "last_synced_rev") val lastSyncedRev: Long = 0,
    @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long? = null,
    @ColumnInfo(name = "notify_enabled") val notifyEnabled: Boolean = true,
    @ColumnInfo(name = "color_index") val colorIndex: Int = 0,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
    val canEdit get() = state == ListState.ACTIVE && (role == ListRole.OWNER || role == ListRole.EDITOR)
    val isCloud get() = kind == ListKind.CLOUD
}

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(entity = ListEntity::class, parentColumns = ["id"], childColumns = ["list_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("list_id")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id") val listId: String,
    val rev: Long = 0,
    @ColumnInfo(name = "sync_state") val syncState: String = SyncState.SYNCED,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    val title: String,
    val type: String,
    @ColumnInfo(name = "custom_label") val customLabel: String?,
    val recurring: Boolean,
    @ColumnInfo(name = "observe_by") val observeBy: String,
    @ColumnInfo(name = "input_calendar") val inputCalendar: String,
    @ColumnInfo(name = "g_year") val gYear: Int?,
    @ColumnInfo(name = "g_month") val gMonth: Int?,
    @ColumnInfo(name = "g_day") val gDay: Int?,
    @ColumnInfo(name = "after_sunset") val afterSunset: Boolean,
    @ColumnInfo(name = "h_year") val hYear: Int?,
    @ColumnInfo(name = "h_month") val hMonth: String?,
    @ColumnInfo(name = "h_day") val hDay: Int?,
    val note: String?,
)

data class ListWithCount(@Embedded val list: ListEntity, @ColumnInfo(name = "event_count") val eventCount: Int)

@Dao
interface ListDao {
    @Query("SELECT l.*, (SELECT COUNT(*) FROM events e WHERE e.list_id = l.id AND e.sync_state != 'PENDING_DELETE') AS event_count FROM lists l ORDER BY l.sort_order, l.created_at")
    fun observeWithCounts(): Flow<List<ListWithCount>>

    @Query("SELECT * FROM lists ORDER BY sort_order, created_at")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists ORDER BY sort_order, created_at")
    suspend fun getAll(): List<ListEntity>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun get(id: String): ListEntity?

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observe(id: String): Flow<ListEntity?>

    @Upsert suspend fun upsert(list: ListEntity)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM lists")
    suspend fun maxSortOrder(): Int
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE sync_state != 'PENDING_DELETE'")
    fun observeVisible(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE sync_state != 'PENDING_DELETE'")
    suspend fun getVisible(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE id = :id AND sync_state != 'PENDING_DELETE'")
    fun observe(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE list_id = :listId AND sync_state != 'SYNCED'")
    suspend fun pending(listId: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE list_id = :listId AND sync_state != 'PENDING_DELETE'")
    suspend fun visibleInList(listId: String): List<EventEntity>

    @Upsert suspend fun upsert(e: EventEntity)
    @Upsert suspend fun upsertAll(e: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM events WHERE list_id = :listId")
    suspend fun deleteAllInList(listId: String)

    @Query("UPDATE events SET sync_state = 'PENDING_DELETE', updated_at = :now WHERE id = :id")
    suspend fun markPendingDelete(id: String, now: Long)

    @Query("DELETE FROM events WHERE list_id = :listId AND sync_state = 'SYNCED'")
    suspend fun deleteSyncedInList(listId: String)

    @Query("DELETE FROM events WHERE list_id = :listId AND sync_state != 'SYNCED'")
    suspend fun deletePendingInList(listId: String)
}

@Database(entities = [ListEntity::class, EventEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lists(): ListDao
    abstract fun events(): EventDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "kalendar.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "INSERT INTO lists (id, kind, name, role, state, last_synced_rev, notify_enabled, color_index, sort_order, created_at) " +
                                "VALUES ('$LOCAL_LIST_ID', '${ListKind.LOCAL}', '', '${ListRole.OWNER}', '${ListState.ACTIVE}', 0, 1, 0, 0, ${System.currentTimeMillis()})",
                        )
                    }
                })
                .build()
    }
}
