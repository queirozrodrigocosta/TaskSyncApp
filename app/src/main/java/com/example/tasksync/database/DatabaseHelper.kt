package com.example.tasksync.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tasksync.models.Task
import com.example.tasksync.models.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "tasksync.db"
        private const val DATABASE_VERSION = 1

        // Tabela de usuários
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USER_NAME = "name"
        private const val COLUMN_USER_EMAIL = "email"
        private const val COLUMN_USER_PHOTO_URL = "photo_url"
        private const val COLUMN_USER_CREATED_AT = "created_at"
        private const val COLUMN_USER_UPDATED_AT = "updated_at"

        // Tabela de tarefas
        private const val TABLE_TASKS = "tasks"
        private const val COLUMN_TASK_ID = "id"
        private const val COLUMN_TASK_TITLE = "title"
        private const val COLUMN_TASK_DESCRIPTION = "description"
        private const val COLUMN_TASK_DATE = "date"
        private const val COLUMN_TASK_TIME = "time"
        private const val COLUMN_TASK_COMPLETED = "completed"
        private const val COLUMN_TASK_USER_ID = "user_id"
        private const val COLUMN_TASK_CREATED_AT = "created_at"
        private const val COLUMN_TASK_UPDATED_AT = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Criar tabela de usuários
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID TEXT PRIMARY KEY,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_USER_PHOTO_URL TEXT,
                $COLUMN_USER_CREATED_AT INTEGER NOT NULL,
                $COLUMN_USER_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        // Criar tabela de tarefas
        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_TASK_ID TEXT PRIMARY KEY,
                $COLUMN_TASK_TITLE TEXT NOT NULL,
                $COLUMN_TASK_DESCRIPTION TEXT,
                $COLUMN_TASK_DATE TEXT NOT NULL,
                $COLUMN_TASK_TIME TEXT NOT NULL,
                $COLUMN_TASK_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_TASK_USER_ID TEXT NOT NULL,
                $COLUMN_TASK_CREATED_AT INTEGER NOT NULL,
                $COLUMN_TASK_UPDATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_TASK_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createTasksTable)

        // Criar índices para melhor performance
        db.execSQL("CREATE INDEX idx_tasks_user_id ON $TABLE_TASKS($COLUMN_TASK_USER_ID)")
        db.execSQL("CREATE INDEX idx_tasks_completed ON $TABLE_TASKS($COLUMN_TASK_COMPLETED)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Métodos para usuários
    fun insertUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, user.id)
            put(COLUMN_USER_NAME, user.name)
            put(COLUMN_USER_EMAIL, user.email)
            put(COLUMN_USER_PHOTO_URL, user.photoUrl)
            put(COLUMN_USER_CREATED_AT, user.createdAt)
            put(COLUMN_USER_UPDATED_AT, user.updatedAt)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUser(userId: String): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(
                COLUMN_USER_ID, COLUMN_USER_NAME, COLUMN_USER_EMAIL,
                COLUMN_USER_PHOTO_URL, COLUMN_USER_CREATED_AT, COLUMN_USER_UPDATED_AT
            ),
            "$COLUMN_USER_ID = ?",
            arrayOf(userId),
            null, null, null
        )

        return cursor.use { c ->
            if (c.moveToFirst()) {
                User(
                    id = c.getString(c.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    name = c.getString(c.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                    email = c.getString(c.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
                    photoUrl = c.getString(c.getColumnIndexOrThrow(COLUMN_USER_PHOTO_URL)) ?: "",
                    createdAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_USER_CREATED_AT)),
                    updatedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_USER_UPDATED_AT))
                )
            } else null
        }
    }

    fun updateUser(user: User): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, user.name)
            put(COLUMN_USER_EMAIL, user.email)
            put(COLUMN_USER_PHOTO_URL, user.photoUrl)
            put(COLUMN_USER_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(TABLE_USERS, values, "$COLUMN_USER_ID = ?", arrayOf(user.id))
    }

    // Métodos para tarefas
    fun insertTask(task: Task): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TASK_ID, task.id)
            put(COLUMN_TASK_TITLE, task.title)
            put(COLUMN_TASK_DESCRIPTION, task.description)
            put(COLUMN_TASK_DATE, task.date)
            put(COLUMN_TASK_TIME, task.time)
            put(COLUMN_TASK_COMPLETED, if (task.completed) 1 else 0)
            put(COLUMN_TASK_USER_ID, task.userId)
            put(COLUMN_TASK_CREATED_AT, task.createdAt)
            put(COLUMN_TASK_UPDATED_AT, task.updatedAt)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    fun getTasks(userId: String): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            arrayOf(
                COLUMN_TASK_ID, COLUMN_TASK_TITLE, COLUMN_TASK_DESCRIPTION,
                COLUMN_TASK_DATE, COLUMN_TASK_TIME, COLUMN_TASK_COMPLETED,
                COLUMN_TASK_USER_ID, COLUMN_TASK_CREATED_AT, COLUMN_TASK_UPDATED_AT
            ),
            "$COLUMN_TASK_USER_ID = ?",
            arrayOf(userId),
            null, null, "$COLUMN_TASK_CREATED_AT DESC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                tasks.add(
                    Task(
                        id = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_ID)),
                        title = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                        description = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_DESCRIPTION)) ?: "",
                        date = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_DATE)),
                        time = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_TIME)),
                        completed = c.getInt(c.getColumnIndexOrThrow(COLUMN_TASK_COMPLETED)) == 1,
                        userId = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_USER_ID)),
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_TASK_CREATED_AT)),
                        updatedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_TASK_UPDATED_AT))
                    )
                )
            }
        }
        return tasks
    }

    fun updateTask(task: Task): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TASK_TITLE, task.title)
            put(COLUMN_TASK_DESCRIPTION, task.description)
            put(COLUMN_TASK_DATE, task.date)
            put(COLUMN_TASK_TIME, task.time)
            put(COLUMN_TASK_COMPLETED, if (task.completed) 1 else 0)
            put(COLUMN_TASK_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(TABLE_TASKS, values, "$COLUMN_TASK_ID = ?", arrayOf(task.id))
    }

    fun deleteTask(taskId: String): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_TASKS, "$COLUMN_TASK_ID = ?", arrayOf(taskId))
    }

    fun getTask(taskId: String): Task? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            arrayOf(
                COLUMN_TASK_ID, COLUMN_TASK_TITLE, COLUMN_TASK_DESCRIPTION,
                COLUMN_TASK_DATE, COLUMN_TASK_TIME, COLUMN_TASK_COMPLETED,
                COLUMN_TASK_USER_ID, COLUMN_TASK_CREATED_AT, COLUMN_TASK_UPDATED_AT
            ),
            "$COLUMN_TASK_ID = ?",
            arrayOf(taskId),
            null, null, null
        )

        return cursor.use { c ->
            if (c.moveToFirst()) {
                Task(
                    id = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_ID)),
                    title = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                    description = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_DESCRIPTION)) ?: "",
                    date = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_DATE)),
                    time = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_TIME)),
                    completed = c.getInt(c.getColumnIndexOrThrow(COLUMN_TASK_COMPLETED)) == 1,
                    userId = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_USER_ID)),
                    createdAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_TASK_CREATED_AT)),
                    updatedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_TASK_UPDATED_AT))
                )
            } else null
        }
    }

    // Método para sincronização - obter todas as tarefas para backup
    fun getAllTasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            arrayOf(
                COLUMN_TASK_ID, COLUMN_TASK_TITLE, COLUMN_TASK_DESCRIPTION,
                COLUMN_TASK_DATE, COLUMN_TASK_TIME, COLUMN_TASK_COMPLETED,
                COLUMN_TASK_USER_ID, COLUMN_TASK_CREATED_AT, COLUMN_TASK_UPDATED_AT
            ),
            null, null, null, null, "$COLUMN_TASK_CREATED_AT DESC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                tasks.add(
                    Task(
                        id = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_ID)),
                        title = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                        description = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_DESCRIPTION)) ?: "",
                        date = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_DATE)),
                        time = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_TIME)),
                        completed = c.getInt(c.getColumnIndexOrThrow(COLUMN_TASK_COMPLETED)) == 1,
                        userId = c.getString(c.getColumnIndexOrThrow(COLUMN_TASK_USER_ID)),
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_TASK_CREATED_AT)),
                        updatedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_TASK_UPDATED_AT))
                    )
                )
            }
        }
        return tasks
    }
}
