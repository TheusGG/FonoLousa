package com.fonolousa.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "item_progress",
    indices = [
        Index(value = ["categoryId", "level"]),
        Index(value = ["isFavorite", "updatedAt"])
    ]
)
data class ItemProgressEntity(
    @PrimaryKey val itemKey: String,
    val categoryId: String,
    val level: Int,
    val itemId: String,
    val word: String,
    val views: Int = 0,
    val plays: Int = 0,
    val isFavorite: Boolean = false,
    val lastSeenAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "session_events",
    indices = [
        Index(value = ["createdAt"])
    ]
)
data class SessionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: String,
    val categoryId: String,
    val level: Int,
    val itemId: String,
    val word: String,
    val eventType: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "clinical_results",
    indices = [
        Index(value = ["childName"]),
        Index(value = ["sessionId"]),
        Index(value = ["activity", "createdAt"])
    ]
)
data class ClinicalResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: String,
    val childName: String = "Crianca",
    val activity: String,
    val categoryId: String,
    val level: Int,
    val itemId: String,
    val word: String,
    val isCorrect: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FonoLocalDao {
    @Query("SELECT * FROM item_progress")
    fun observeProgress(): Flow<List<ItemProgressEntity>>

    @Query("SELECT * FROM item_progress WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<ItemProgressEntity>>

    @Query("SELECT * FROM session_events ORDER BY createdAt DESC LIMIT 120")
    fun observeRecentEvents(): Flow<List<SessionEventEntity>>

    @Query("SELECT * FROM clinical_results ORDER BY createdAt DESC")
    fun observeClinicalResults(): Flow<List<ClinicalResultEntity>>

    @Query("SELECT * FROM item_progress WHERE itemKey = :itemKey LIMIT 1")
    suspend fun getProgress(itemKey: String): ItemProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ItemProgressEntity)

    @Insert
    suspend fun insertEvent(event: SessionEventEntity)

    @Insert
    suspend fun insertClinicalResult(result: ClinicalResultEntity)

    @Query("UPDATE clinical_results SET isCorrect = :isCorrect WHERE id = :id")
    suspend fun updateClinicalResult(id: Long, isCorrect: Boolean)

    @Query("DELETE FROM clinical_results WHERE id = :id")
    suspend fun deleteClinicalResult(id: Long)

    @Query("DELETE FROM clinical_results WHERE id IN (:ids)")
    suspend fun deleteClinicalResults(ids: List<Long>)
}

@Database(
    entities = [ItemProgressEntity::class, SessionEventEntity::class, ClinicalResultEntity::class],
    version = 4,
    exportSchema = false
)
abstract class FonoLocalDatabase : RoomDatabase() {
    abstract fun dao(): FonoLocalDao

    companion object {
        @Volatile
        private var instance: FonoLocalDatabase? = null

        fun get(context: Context): FonoLocalDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FonoLocalDatabase::class.java,
                    "fonolousa-local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS clinical_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        activity TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        level INTEGER NOT NULL,
                        itemId TEXT NOT NULL,
                        word TEXT NOT NULL,
                        isCorrect INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE clinical_results ADD COLUMN childName TEXT NOT NULL DEFAULT 'Crianca'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_progress_categoryId_level ON item_progress(categoryId, level)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_progress_isFavorite_updatedAt ON item_progress(isFavorite, updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_events_createdAt ON session_events(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_results_childName ON clinical_results(childName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_results_sessionId ON clinical_results(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_results_activity_createdAt ON clinical_results(activity, createdAt)")
            }
        }
    }
}
