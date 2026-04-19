package com.example.reminderstudent2

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {

    private lateinit var store: LocalDataStore
    private lateinit var subjectsContainer: LinearLayout
    private var selectedDay: Int = Calendar.SATURDAY
    private lateinit var dayViews: List<Pair<TextView, Int>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = LocalDataStore.getInstance(requireContext())
        subjectsContainer = view.findViewById(R.id.subjects_container)

        dayViews = listOf(
            view.findViewById<TextView>(R.id.tv_sat) to Calendar.SATURDAY,
            view.findViewById<TextView>(R.id.tv_sun) to Calendar.SUNDAY,
            view.findViewById<TextView>(R.id.tv_mon) to Calendar.MONDAY,
            view.findViewById<TextView>(R.id.tv_tue) to Calendar.TUESDAY,
            view.findViewById<TextView>(R.id.tv_wed) to Calendar.WEDNESDAY,
            view.findViewById<TextView>(R.id.tv_thu) to Calendar.THURSDAY,
            view.findViewById<TextView>(R.id.tv_fri) to Calendar.FRIDAY
        )

        selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        setupDayChips()
        selectDay(selectedDay)

        view.findViewById<MaterialButton>(R.id.btn_add_subject).setOnClickListener {
            showAddSubjectDialog()
        }
    }

    private fun setupDayChips() {
        dayViews.forEach { (textView, day) ->
            textView.setOnClickListener { selectDay(day) }
        }
    }

    private fun selectDay(day: Int) {
        selectedDay = day
        dayViews.forEach { (chip, chipDay) ->
            val selected = chipDay == day
            chip.background = ContextCompat.getDrawable(
                requireContext(),
                if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.text_secondary
                )
            )
        }
        renderSubjectsForSelectedDay()
    }

    private fun renderSubjectsForSelectedDay() {
        subjectsContainer.removeAllViews()
        val subjects = store.getSubjectsForDay(selectedDay)
        if (subjects.isEmpty()) {
            subjectsContainer.addView(buildEmptyView())
            return
        }
        subjects.forEach { subject ->
            subjectsContainer.addView(buildSubjectCard(subject))
        }
    }

    private fun buildEmptyView(): TextView {
        return TextView(requireContext()).apply {
            text = "لا توجد مواد لهذا اليوم"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
            val marginTop = dp(24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = marginTop
            }
        }
    }

    private fun buildSubjectCard(subject: SubjectItem): LinearLayout {
        val context = requireContext()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.card_surface))
            setPadding(dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        }

        val title = TextView(context).apply {
            text = subject.name
            textSize = 18f
            gravity = android.view.Gravity.END
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.palette_navy))
        }

        val meta = TextView(context).apply {
            text = "الساعة ${toArabicTime(subject.startTime)} - ${toArabicTime(subject.endTime)} — قاعة ${subject.room.ifBlank { "-" }}"
            textSize = 14f
            gravity = android.view.Gravity.END
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, dp(8), 0, 0)
        }

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            setPadding(0, dp(8), 0, 0)
        }

        val editBtn = TextView(context).apply {
            text = "تعديل"
            textSize = 12f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip_selected)
            setOnClickListener { showEditSubjectDialog(subject) }
        }

        val deleteBtn = TextView(context).apply {
            text = "حذف"
            textSize = 12f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip_selected)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginStart = dp(6)
            layoutParams = lp
            setOnClickListener { confirmDeleteSubject(subject.id) }
        }

        actions.addView(editBtn)
        actions.addView(deleteBtn)

        card.addView(title)
        card.addView(meta)
        card.addView(actions)
        return card
    }

    private fun showAddSubjectDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
        }

        val nameInput = EditText(context).apply { hint = "اسم المادة" }
        val startInput = EditText(context).apply { hint = "وقت البداية HH:mm" }
        val endInput = EditText(context).apply { hint = "وقت النهاية HH:mm" }
        val roomInput = EditText(context).apply { hint = "القاعة" }

        listOf(nameInput, startInput, endInput, roomInput).forEach { editText ->
            container.addView(editText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
        }

        AlertDialog.Builder(context)
            .setTitle("إضافة مادة")
            .setView(container)
            .setPositiveButton("حفظ") { _, _ ->
                val name = nameInput.text.toString().trim()
                val startRaw = startInput.text.toString().trim()
                val endRaw = endInput.text.toString().trim()
                val room = roomInput.text.toString().trim()
                if (name.isEmpty() || startRaw.isEmpty() || endRaw.isEmpty()) {
                    Toast.makeText(context, "الرجاء تعبئة اسم المادة والوقت", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!isValidTime(startRaw) || !isValidTime(endRaw)) {
                    Toast.makeText(context, "اكتب الوقت بصيغة HH:mm مثل 10:30", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val start = normalizeInputTime(startRaw)
                val end = normalizeInputTime(endRaw)

                store.addSubject(
                    name = name,
                    dayOfWeek = selectedDay,
                    startTime = start,
                    endTime = end,
                    room = room
                )
                store.addReminder(
                    title = "تذكير مادة $name",
                    body = "المحاضرة الساعة $start — قاعة ${room.ifBlank { "-" }}",
                    triggerDate = dateForSelectedDay(),
                    triggerTime = start
                )
                renderSubjectsForSelectedDay()
                Toast.makeText(context, "تمت الإضافة", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showEditSubjectDialog(subject: SubjectItem) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
        }

        val nameInput = EditText(context).apply {
            hint = "اسم المادة"
            setText(subject.name)
        }
        val startInput = EditText(context).apply {
            hint = "وقت البداية HH:mm"
            setText(subject.startTime)
        }
        val endInput = EditText(context).apply {
            hint = "وقت النهاية HH:mm"
            setText(subject.endTime)
        }
        val roomInput = EditText(context).apply {
            hint = "القاعة"
            setText(subject.room)
        }

        listOf(nameInput, startInput, endInput, roomInput).forEach { editText ->
            container.addView(
                editText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            )
        }

        AlertDialog.Builder(context)
            .setTitle("تعديل المادة")
            .setView(container)
            .setPositiveButton("حفظ") { _, _ ->
                val name = nameInput.text.toString().trim()
                val startRaw = startInput.text.toString().trim()
                val endRaw = endInput.text.toString().trim()
                val room = roomInput.text.toString().trim()

                if (name.isEmpty() || startRaw.isEmpty() || endRaw.isEmpty()) {
                    Toast.makeText(context, "الرجاء تعبئة اسم المادة والوقت", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!isValidTime(startRaw) || !isValidTime(endRaw)) {
                    Toast.makeText(context, "اكتب الوقت بصيغة HH:mm مثل 10:30", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val start = normalizeInputTime(startRaw)
                val end = normalizeInputTime(endRaw)

                val updated = store.updateSubject(subject.id, name, start, end, room)
                if (updated) {
                    renderSubjectsForSelectedDay()
                    Toast.makeText(context, "تم التعديل", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر تعديل المادة", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeleteSubject(subjectId: Long) {
        val context = requireContext()
        AlertDialog.Builder(context)
            .setTitle("حذف المادة")
            .setMessage("هل أنت متأكد من حذف هذه المادة؟")
            .setPositiveButton("حذف") { _, _ ->
                val deleted = store.deleteSubject(subjectId)
                if (deleted) {
                    renderSubjectsForSelectedDay()
                    Toast.makeText(context, "تم الحذف", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر حذف المادة", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun isValidTime(value: String): Boolean {
        if (value == "24:00") return true
        return Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matches(value)
    }

    private fun normalizeInputTime(value: String): String {
        return if (value == "24:00") "00:00" else value
    }

    private fun dateForSelectedDay(): String {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var diff = selectedDay - today
        if (diff < 0) diff += 7
        cal.add(Calendar.DAY_OF_YEAR, diff)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun toArabicTime(value: String): String {
        val parts = value.split(":")
        if (parts.size != 2) return value
        val hour = parts[0].toIntOrNull() ?: return value
        val minute = parts[1].toIntOrNull() ?: return value
        val h12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        val suffix = if (hour < 12) "ص" else "م"
        return String.format("%d:%02d %s", h12, minute, suffix)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
