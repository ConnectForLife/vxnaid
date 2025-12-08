package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class BestContactTimePickerDialog(
    private var selectedTime: String? = null,
    private var listener: BestContactTimePickerListener? = null,
) : DialogFragment() {

    interface BestContactTimePickerListener {
        fun onBestContactTimePicked(time: String)
    }

    fun setListener(listener: BestContactTimePickerListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
                showConfirmationDialog(selectedHour, selectedMinute)
            },
            hour,
            minute,
            false
        )
    }

    private fun showConfirmationDialog(selectedHour: Int, selectedMinute: Int) {
        val displayTime = formatTimeFor12Hour(selectedHour, selectedMinute)
        val formTime = formatTimeFor24Hour(selectedHour, selectedMinute)

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Time")
            .setMessage("Is $displayTime your best contact time?")
            .setPositiveButton("Yes") { _, _ ->
                listener?.onBestContactTimePicked(formTime)
                    ?: (parentFragment as? BestContactTimePickerListener)?.onBestContactTimePicked(formTime)
                    ?: (activity as? BestContactTimePickerListener)?.onBestContactTimePicked(formTime)
                dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun parseTimeString(timeStr: String): Pair<Int, Int>? {
        return try {
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

    private fun formatTimeFor12Hour(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }

    private fun formatTimeFor24Hour(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return formatTimeFor24Hour(hour, minute)
    }
}