package com.example.taskflow.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @Test fun v5MigratesSharedBlankAndUnownedCategoriesAndRoomValidatesSchema() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-${java.util.UUID.randomUUID()}"
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(5) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE tasks (id TEXT PRIMARY KEY NOT NULL, description TEXT NOT NULL, isCompleted INTEGER NOT NULL DEFAULT 0, priority TEXT NOT NULL DEFAULT 'MEDIA', categoryId INTEGER REFERENCES categories(id) ON DELETE RESTRICT, userId TEXT NOT NULL DEFAULT 'USER', updatedAt INTEGER NOT NULL, isSynced INTEGER NOT NULL DEFAULT 0, pendingDelete INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE INDEX index_tasks_categoryId ON tasks(categoryId)")
                db.execSQL("INSERT INTO categories VALUES (1, 'Shared'), (2, 'Unowned')")
                listOf("alice", "bob", "", "USER").forEachIndexed { i, uid -> db.execSQL("INSERT INTO tasks VALUES (?, ?, 0, 'ALTA', 1, ?, 50, 1, 0)", arrayOf("$i", "Task $i", uid)) }
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build())
        helper.writableDatabase
        helper.close()
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_5_6).allowMainThreadQueries().build()
        try {
            room.openHelper.writableDatabase // Room performs actual generated schema validation.
            val tasks = listOf("alice", "bob", "", "USER").flatMap { room.taskDao().getTasks(it) }
            assertEquals(4, tasks.size)
            assertEquals(4, tasks.map { it.categoryId }.distinct().size)
            tasks.forEach { task ->
                assertEquals(task.userId, room.categoryDao().getCategoryById(requireNotNull(task.categoryId))?.userId)
                assertTrue(task.updatedAt > 50)
                assertFalse(task.isSynced)
            }
            assertTrue(room.categoryDao().getCategories("USER").any { it.name == "Unowned" })
        } finally { room.close(); context.deleteDatabase(name) }
    }
}
