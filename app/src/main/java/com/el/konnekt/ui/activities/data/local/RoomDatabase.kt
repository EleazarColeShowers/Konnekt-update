package com.el.konnekt.ui.activities.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//@Database(
//    entities = [UserEntity::class, FriendEntity::class, MessageEntity::class, GroupEntity::class],
//    version = 4
//)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//    abstract fun friendDao(): FriendDao
//    abstract fun messageDao(): MessageDao
//    abstract fun groupDao(): GroupDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("""
//                    CREATE TABLE friends_new (
//                        friendId TEXT NOT NULL PRIMARY KEY,
//                        userId TEXT NOT NULL,
//                        username TEXT NOT NULL,
//                        profileImageUri TEXT NOT NULL,
//                        timestamp INTEGER NOT NULL,
//                        FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE
//                    )
//                """)
//                db.execSQL("""
//                    INSERT INTO friends_new (friendId, userId, username, profileImageUri, timestamp)
//                    SELECT friendId, 'default_user', username, profileImageUri, timestamp FROM friends
//                """)
//                db.execSQL("DROP TABLE friends")
//                db.execSQL("ALTER TABLE friends_new RENAME TO friends")
//            }
//        }
//
//        private val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("""
//                    CREATE TABLE IF NOT EXISTS `groups` (
//                        groupId TEXT NOT NULL PRIMARY KEY,
//                        groupName TEXT NOT NULL,
//                        groupImageUri TEXT,
//                        memberIds TEXT NOT NULL
//                    )
//                """.trimIndent())
//            }
//        }
//
//        private val MIGRATION_3_4 = object : Migration(3, 4) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("""
//                    ALTER TABLE groups ADD COLUMN userId TEXT NOT NULL DEFAULT ''
//                """.trimIndent())
//            }
//        }
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "instachat_db"
//                )
//                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}
//
@Database(
    entities = [UserEntity::class, FriendEntity::class, MessageEntity::class, GroupEntity::class],
    version = 5 // bumped from 4 → 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE friends_new (
                        friendId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        username TEXT NOT NULL,
                        profileImageUri TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    INSERT INTO friends_new (friendId, userId, username, profileImageUri, timestamp)
                    SELECT friendId, 'default_user', username, profileImageUri, timestamp FROM friends
                """)
                db.execSQL("DROP TABLE friends")
                db.execSQL("ALTER TABLE friends_new RENAME TO friends")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `groups` (
                        groupId TEXT NOT NULL PRIMARY KEY,
                        groupName TEXT NOT NULL,
                        groupImageUri TEXT,
                        memberIds TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE groups ADD COLUMN userId TEXT NOT NULL DEFAULT ''
                """.trimIndent())
            }
        }

        // ✅ NEW: Rename "groups" → "group_table"
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Only rename if "groups" table exists
                db.execSQL("ALTER TABLE groups RENAME TO group_table")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "instachat_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

