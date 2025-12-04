package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class BestContactTimePickerDialog(
    private var selectedTime: String? = null,
) : DialogFragment() {

    interface BestContactTimePickerListener {
        fun onBestContactTimePicked(time: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Parse selected time or use current time as default
        val calendar = Calendar.getInstance()
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        var minute = calendar.get(Calendar.MINUTE)

        selectedTime?.let { timeStr ->
            parseTimeString(timeStr)?.let { parsedTime ->
                hour = parsedTime.first
                minute = parsedTime.second
            }
        }

        return TimePickerDialog(
            requireContext(),
            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                val formattedTime = formatTime(selectedHour, selectedMinute)
                (parentFragment as? BestContactTimePickerListener)?.onBestContactTimePicked(formattedTime)
                    ?: (activity as? BestContactTimePickerListener)?.onBestContactTimePicked(formattedTime)
            },
            hour,
            minute,
            false // Use 12-hour format
        )
    }

    private fun parseTimeString(timeStr: String): Pair<Int, Int>? {
        return try {
            // Handle predefined time ranges like "Morning (6:00 AM - 12:00 PM)"
            val timeRegex = "(\\d{1,2}):(\\d{2})\\s*(AM|PM)".toRegex()
            val matchResult = timeRegex.find(timeStr)

            if (matchResult != null) {
                var hour = matchResult.groupValues[1].toInt()
                val minute = matchResult.groupValues[2].toInt()
                val amPm = matchResult.groupValues[3]

                if (amPm == "PM" && hour != 12) {
                    hour += 12
                } else if (amPm == "AM" && hour == 12) {
                    hour = 0
                }

                Pair(hour, minute)
            } else {
                // Try to parse as HH:MM format
                val parts = timeStr.split(":")
                if (parts.size >= 2) {
                    Pair(parts[0].toInt(), parts[1].toInt())
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }
}