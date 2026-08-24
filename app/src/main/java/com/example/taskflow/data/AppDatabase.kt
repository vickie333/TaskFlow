package com.example.taskflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, Category::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase() : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1,2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isCompleted INTEGER DEFAULT 0 NOT NULL")
            }
        }

        val MIGRATION_2_3 = object: Migration(2,3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT DEFAULT ('MEDIA') NOT NULL")
            }
        }

        val MIGRATION_3_4 = object: Migration(3,4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("ALTER TABLE tasks ADD COLUMN categoryId INTEGER REFERENCES categories(id) ON DELETE RESTRICT")
                db.execSQL("CREATE INDEX index_tasks_categoryId ON tasks(categoryId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4,5) {
            val migrationTimestamp = System.currentTimeMillis()

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE tasks_new (id TEXT PRIMARY KEY NOT NULL, description TEXT NOT NULL, isCompleted INTEGER DEFAULT 0 NOT NULL, priority TEXT DEFAULT ('MEDIA') NOT NULL, categoryId INTEGER REFERENCES categories(id) ON DELETE RESTRICT, userId TEXT DEFAULT 'USER' NOT NULL, updatedAt INTEGER NOT NULL, isSynced INTEGER DEFAULT 0 NOT NULL, pendingDelete INTEGER DEFAULT 0 NOT NULL)")
                db.execSQL("INSERT INTO tasks_new (id, description, isCompleted, priority, categoryId, userId, updatedAt, isSynced, pendingDelete) SELECT CAST(id AS TEXT), description, isCompleted, priority, categoryId, 'USER', $migrationTimestamp, 0, 0 FROM tasks")
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
                db.execSQL("CREATE INDEX index_tasks_categoryId ON tasks(categoryId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskflow-db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                INSTANCE = instance
                instance
            }
        }
    }
}