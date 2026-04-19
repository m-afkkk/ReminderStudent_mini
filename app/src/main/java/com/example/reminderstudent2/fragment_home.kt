package com.example.reminderstudent2

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlin.math.roundToInt

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var store: LocalDataStore
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val uiHandler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val root = view ?: return
            bindStats(root)
            bindTodaySubjects(root)
            uiHandler.postDelayed(this, 1000L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = LocalDataStore.getInstance(requireContext())
        bindHeader(view)
        bindStats(view)
        bindTodaySubjects(view)
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(ticker)
    }

    private fun bindHeader(view: View) {
        val greetingText = view.findViewById<TextView>(R.id.tv_greeting)
        val dayText = view.findViewById<TextView>(R.id.tv_day)
        val userName = store.getUserName().orEmpty().ifBlank { "عبدالله" }
        greetingText.text = "أهلاً، $userName"
        dayText.text = arabicDayName(currentDayOfWeek())
        fetchFirebaseUserName(greetingText)
    }

    private fun fetchFirebaseUserName(greetingText: TextView) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val remoteName = snapshot.getString("fullName")
                    ?: snapshot.getString("name")
                    ?: auth.currentUser?.displayName
                    ?: auth.currentUser?.email?.substringBefore("@")
                if (!remoteName.isNullOrBlank()) {
                    store.saveUserName(remoteName)
                    greetingText.text = "أهلاً، $remoteName"
                }
            }
    }

    private fun bindStats(view: View) {
        val todaySubjects = store.getSubjectsForDay(currentDayOfWeek())
        view.findViewById<TextView>(R.id.tv_subjects_today_value).text = todaySubjects.size.toString()
        view.findViewById<TextView>(R.id.tv_upcoming_reminders_value).text = store.getTodayReminderCount().toString()
        view.findViewById<TextView>(R.id.tv_attendance_value).text = "${store.getAttendancePercent()}%"
    }

    private fun bindTodaySubjects(view: View) {
        val nowMinutes = currentMinutes()
        val subjects = store.getSubjectsForDay(currentDayOfWeek())
            .sortedBy { subjectSortScore(it.startTime, nowMinutes) }
            .take(3)
        val cards = listOf(
            SubjectCardRefs(
                root = view.findViewById(R.id.card_math),
                title = view.findViewById(R.id.tv_card_math_title),
                badge = view.findViewById(R.id.tv_card_math_badge),
                meta = view.findViewById(R.id.tv_card_math_meta),
                progress = view.findViewById(R.id.progress_card_math)
            ),
            SubjectCardRefs(
                root = view.findViewById(R.id.card_java),
                title = view.findViewById(R.id.tv_card_java_title),
                badge = view.findViewById(R.id.tv_card_java_badge),
                meta = view.findViewById(R.id.tv_card_java_meta),
                progress = view.findViewById(R.id.progress_card_java)
            ),
            SubjectCardRefs(
                root = view.findViewById(R.id.card_db),
                title = view.findViewById(R.id.tv_card_db_title),
                badge = view.findViewById(R.id.tv_card_db_badge),
                meta = view.findViewById(R.id.tv_card_db_meta),
                progress = view.findViewById(R.id.progress_card_db)
            )
        )

        cards.forEachIndexed { index, refs ->
            val item = subjects.getOrNull(index)
            if (item == null) {
                refs.root.visibility = View.VISIBLE
                refs.title.text = "لا توجد مادة"
                refs.badge.text = "--:--"
                refs.meta.text = "الساعة --:-- — قاعة -"
                refs.progress.progress = 0
                refs.progress.progressTintList = ColorStateList.valueOf(Color.RED)
            } else {
                refs.root.visibility = View.VISIBLE
                refs.title.text = item.name
                refs.meta.text = "الساعة ${toArabicTime(item.startTime)} — قاعة ${item.room}"
                refs.badge.text = buildCountdownText(item.startTime)
                refs.progress.progress = calculateCountdownProgress(item.startTime)
                refs.progress.progressTintList = ColorStateList.valueOf(Color.RED)
            }
        }
    }

    private fun buildCountdownText(startTime: String): String {
        val startMinutes = parseTimeMinutes(startTime) ?: return toArabicTime(startTime)
        val nowMinutes = currentMinutes()
        val remainingSeconds = (startMinutes - nowMinutes) * 60 - Calendar.getInstance().get(Calendar.SECOND)
        return if (nowMinutes < startMinutes) {
            "متبقي ${formatDuration(remainingSeconds.coerceAtLeast(0))}"
        } else if (nowMinutes == startMinutes) {
            "تبدأ الآن"
        } else {
            "بدأت"
        }
    }

    private fun calculateCountdownProgress(startTime: String): Int {
        val now = currentMinutes()
        val start = parseTimeMinutes(startTime) ?: return 0
        if (now >= start) return 0

        val minutesLeft = (start - now).coerceAtLeast(0)
        val maxWindow = 180f // 3 hours countdown window
        return ((minutesLeft / maxWindow) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun parseTimeMinutes(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour == 24 && minute == 0) return 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun currentMinutes(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun toArabicTime(value: String): String {
        val minutes = parseTimeMinutes(value) ?: return value
        val hour24 = minutes / 60
        val minute = minutes % 60
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        val suffix = if (hour24 < 12) "ص" else "م"
        return String.format("%d:%02d %s", hour12, minute, suffix)
    }

    private fun subjectSortScore(startTime: String, nowMinutes: Int): Int {
        val start = parseTimeMinutes(startTime) ?: return Int.MAX_VALUE
        val diff = start - nowMinutes
        return if (diff >= 0) diff else 10_000 + kotlin.math.abs(diff)
    }

    private fun formatDuration(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    private fun arabicDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SATURDAY -> "السبت"
            Calendar.SUNDAY -> "الأحد"
            Calendar.MONDAY -> "الاثنين"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.THURSDAY -> "الخميس"
            Calendar.FRIDAY -> "الجمعة"
            else -> ""
        }
    }
}

private data class SubjectCardRefs(
    val root: LinearLayout,
    val title: TextView,
    val badge: TextView,
    val meta: TextView,
    val progress: ProgressBar
)