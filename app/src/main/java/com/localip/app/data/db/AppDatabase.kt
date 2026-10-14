package com.localip.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConnRecord::class, SpeedRecord::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun records(): RecordDao
    abstract fun speeds(): SpeedDao

    companion object {

        /** v1 → v2：新增测速记录表。写成迁移而不是破坏性重建，避免清掉用户已有的连接记录。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `speed_record` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`host` TEXT NOT NULL, " +
                        "`port` INTEGER NOT NULL, " +
                        "`protocol` TEXT NOT NULL, " +
                        "`direction` TEXT NOT NULL, " +
                        "`streams` INTEGER NOT NULL, " +
                        "`mbps` REAL NOT NULL, " +
                        "`retransmits` INTEGER NOT NULL, " +
                        "`jitterMs` REAL, " +
                        "`lostPackets` INTEGER NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "localip.db",
            ).addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
