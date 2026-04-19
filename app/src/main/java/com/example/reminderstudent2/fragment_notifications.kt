package com.example.reminderstudent2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Calendar
import kotlin.math.abs

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private lateinit var store: LocalDataStore
    private val uiHandler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val root = view ?: return
            bindTodayNotifications(root)
            uiHandler.postDelayed(this, 1000L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = LocalDataStore.getInstance(requireContext())
        bindTodayNotifications(view)
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(ticker)
    }

    private fun bindTodayNotifications(view: View) {
        val nowMinutes = currentMinutes()
        val subjects = store.getSubjectsForDay(currentDayOfWeek())
            .sortedBy { subjectSortScore(it.startTime, nowMinutes) }
            .take(3)

        val cards = listOf(
            NotificationCardRefs(
                root = view.findViewById(R.id.notif_math),
                title = view.findViewById(R.id.tv_notif_math_title),
                body = view.findViewById(R.id.tv_notif_math_body),
                time = view.findViewById(R.id.tv_notif_math_time)
            ),
            NotificationCardRefs(
                root = view.findViewById(R.id.notif_java),
                title = view.findViewById(R.id.tv_notif_java_title),
                body = view.findViewById(R.id.tv_notif_java_body),
                time = view.findViewById(R.id.tv_notif_java_time)
            ),
            NotificationCardRefs(
                root = view.findViewById(R.id.notif_db),
                title = view.findViewById(R.id.tv_notif_db_title),
                body = view.findViewById(R.id.tv_notif_db_body),
                time = view.findViewById(R.id.tv_notif_db_time)
            )
        )

        cards.forEachIndexed { index, refs ->
            val item = subjects.getOrNull(index)
            if (item == null) {
                refs.root.alpha = 0.35f
                refs.title.text = "لا توجد مادة"
                refs.body.text = "لا يوجد تذكير لهذا اليوم"
                refs.time.text = "--:--"
            } else {
                refs.root.alpha = if (isPast(item.startTime)) 0.5f else 1f
                refs.title.text = item.name
                refs.body.text = "${buildTimeStatus(item.startTime)} — قاعة ${item.room}"
                refs.time.text = toArabicTime(item.startTime)
            }
        }
    }

    private fun buildTimeStatus(startTime: String): String {
        val start = parseTimeMinutes(startTime) ?: return "المحاضرة قريبًا"
        val now = currentMinutes()
        return when {
            now < start -> {
                val secLeft = (start - now) * 60 - Calendar.getInstance().get(Calendar.SECOND)
                "المحاضرة بعد ${formatDuration(secLeft.coerceAtLeast(0))}"
            }
            now == start -> "المحاضرة تبدأ الآن"
            else -> "تم التذكير"
        }
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

    private fun isPast(startTime: String): Boolean {
        val start = parseTimeMinutes(startTime) ?: return false
        return currentMinutes() > start
    }

    private fun subjectSortScore(startTime: String, nowMinutes: Int): Int {
        val start = parseTimeMinutes(startTime) ?: return Int.MAX_VALUE
        val diff = start - nowMinutes
        return if (diff >= 0) diff else 10_000 + abs(diff)
    }

    private fun currentMinutes(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
}

private data class NotificationCardRefs(
    val root: LinearLayout,
    val title: TextView,
    val body: TextView,
    val time: TextView
)
