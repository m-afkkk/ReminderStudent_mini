package com.example.reminderstudent2

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubjectItem(
    val id: Long,
    val name: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val dayOfWeek: Int,
    val attendancePresent: Int,
    val attendanceTotal: Int
)

class LocalDataStore private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PROFILE (
                id INTEGER PRIMARY KEY CHECK(id = 1),
                full_name TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_SUBJECTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                day_of_week INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                room TEXT NOT NULL,
                attendance_present INTEGER NOT NULL DEFAULT 0,
                attendance_total INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_REMINDERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                trigger_date TEXT NOT NULL,
                trigger_time TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROFILE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SUBJECTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REMINDERS")
        onCreate(db)
    }

    fun saveUserName(name: String) {
        if (name.isBlank()) return
        val values = ContentValues().apply {
            put("id", 1)
            put("full_name", name.trim())
        }
        writableDatabase.insertWithOnConflict(TABLE_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUserName(): String? {
        readableDatabase.rawQuery("SELECT full_name FROM $TABLE_PROFILE WHERE id = 1", null).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    fun addSubject(
        name: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        attendancePresent: Int = 1,
        attendanceTotal: Int = 1
    ) {
        val values = ContentValues().apply {
            put("name", name.trim())
            put("day_of_week", dayOfWeek)
            put("start_time", startTime)
            put("end_time", endTime)
            put("room", room.ifBlank { "-" })
            put("attendance_present", attendancePresent)
            put("attendance_total", attendanceTotal)
        }
        writableDatabase.insert(TABLE_SUBJECTS, null, values)
    }

    fun addReminder(title: String, body: String, triggerDate: String, triggerTime: String) {
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("trigger_date", triggerDate)
            put("trigger_time", triggerTime)
        }
        writableDatabase.insert(TABLE_REMINDERS, null, values)
    }

    fun updateSubject(
        subjectId: Long,
        name: String,
        startTime: String,
        endTime: String,
        room: String
    ): Boolean {
        val values = ContentValues().apply {
            put("name", name.trim())
            put("start_time", startTime)
            put("end_time", endTime)
            put("room", room.ifBlank { "-" })
        }
        val affected = writableDatabase.update(
            TABLE_SUBJECTS,
            values,
            "id = ?",
            arrayOf(subjectId.toString())
        )
        return affected > 0
    }

    fun deleteSubject(subjectId: Long): Boolean {
        val affected = writableDatabase.delete(
            TABLE_SUBJECTS,
            "id = ?",
            arrayOf(subjectId.toString())
        )
        return affected > 0
    }

    fun getSubjectsForDay(dayOfWeek: Int): List<SubjectItem> {
        val result = mutableListOf<SubjectItem>()
        readableDatabase.rawQuery(
            """
            SELECT id, name, start_time, end_time, room, day_of_week, attendance_present, attendance_total
            FROM $TABLE_SUBJECTS
            WHERE day_of_week = ?
            ORDER BY start_time ASC
            """.trimIndent(),
            arrayOf(dayOfWeek.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    SubjectItem(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        startTime = cursor.getString(2),
                        endTime = cursor.getString(3),
                        room = cursor.getString(4),
                        dayOfWeek = cursor.getInt(5),
                        attendancePresent = cursor.getInt(6),
                        attendanceTotal = cursor.getInt(7)
                    )
                )
            }
        }
        return result
    }

    fun getTodayReminderCount(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_REMINDERS WHERE trigger_date = ?",
            arrayOf(today)
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return 0
    }

    fun getAttendancePercent(): Int {
        readableDatabase.rawQuery(
            "SELECT SUM(attendance_present), SUM(attendance_total) FROM $TABLE_SUBJECTS",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val present = cursor.getInt(0)
                val total = cursor.getInt(1)
                if (total <= 0) return 0
                return ((present.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
            }
        }
        return 0
    }

    companion object {
        private const val DATABASE_NAME = "student_local.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_PROFILE = "profile"
        private const val TABLE_SUBJECTS = "subjects"
        private const val TABLE_REMINDERS = "reminders"

        @Volatile
        private var INSTANCE: LocalDataStore? = null

        fun getInstance(context: Context): LocalDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
