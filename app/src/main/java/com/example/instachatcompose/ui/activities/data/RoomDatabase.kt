//package com.example.instachatcompose.ui.activities.data
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//
//@Database(entities = [UserEntity::class, FriendEntity::class, MessageEntity::class], version = 1)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//    abstract fun friendDao(): FriendDao
//    abstract fun messageDao(): MessageDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "instachat_db"
//                ).build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}
package com.example.instachatcompose.ui.activities.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserEntity::class, FriendEntity::class, MessageEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create new friends table with the additional userId column
                database.execSQL("""
                    CREATE TABLE friends_new (
                        friendId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        username TEXT NOT NULL,
                        profileImageUri TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE
                    )
                """)


                // Step 2: Optional - Copy old data to the new table if needed (assumes all entries belong to a single default user)
                // If no userId exists yet, this step can be omitted or you can set a default userId manually.
                // Example below assumes 'default_user' if you want to preserve old entries:
                database.execSQL("""
                    INSERT INTO friends_new (friendId, userId, username, profileImageUri, timestamp)
                    SELECT friendId, 'default_user', username, profileImageUri, timestamp FROM friends
                """)

                // Step 3: Drop the old table
                database.execSQL("DROP TABLE friends")

                // Step 4: Rename new table
                database.execSQL("ALTER TABLE friends_new RENAME TO friends")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "instachat_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

