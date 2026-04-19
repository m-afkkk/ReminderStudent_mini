package com.example.reminderstudent2

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UploadFragment : Fragment(R.layout.fragment_upload) {

    private lateinit var store: LocalDataStore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = LocalDataStore.getInstance(requireContext())

        val subjectName = view.findViewById<EditText>(R.id.et_subject_name)
        val startTime = view.findViewById<EditText>(R.id.et_start_time)
        val endTime = view.findViewById<EditText>(R.id.et_end_time)
        val room = view.findViewById<EditText>(R.id.et_room)
        val saveButton = view.findViewById<MaterialButton>(R.id.btn_save_subject)

        saveButton.setOnClickListener {
            val nameValue = subjectName.text.toString().trim()
            val startRaw = startTime.text.toString().trim()
            val endRaw = endTime.text.toString().trim()
            val roomValue = room.text.toString().trim()

            if (nameValue.isEmpty() || startRaw.isEmpty() || endRaw.isEmpty()) {
                Toast.makeText(requireContext(), "عبئ اسم المادة ووقتها", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidTime(startRaw) || !isValidTime(endRaw)) {
                Toast.makeText(requireContext(), "الوقت يجب أن يكون بصيغة HH:mm مثل 10:00", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val startValue = normalizeInputTime(startRaw)
            val endValue = normalizeInputTime(endRaw)

            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            store.addSubject(
                name = nameValue,
                dayOfWeek = today,
                startTime = startValue,
                endTime = endValue,
                room = roomValue
            )

            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            store.addReminder(
                title = "تذكير مادة $nameValue",
                body = "المحاضرة الساعة $startValue — قاعة ${roomValue.ifBlank { "-" }}",
                triggerDate = todayDate,
                triggerTime = startValue
            )

            subjectName.text?.clear()
            startTime.text?.clear()
            endTime.text?.clear()
            room.text?.clear()
            Toast.makeText(requireContext(), "تمت إضافة المادة وستظهر في الرئيسية", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidTime(value: String): Boolean {
        if (value == "24:00") return true
        return Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matches(value)
    }

    private fun normalizeInputTime(value: String): String {
        return if (value == "24:00") "00:00" else value
    }
}

