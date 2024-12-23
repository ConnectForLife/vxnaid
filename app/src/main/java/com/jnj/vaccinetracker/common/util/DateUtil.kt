package com.jnj.vaccinetracker.common.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.soywiz.klock.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

class DateUtil {
    companion object {

        fun convertDateToString(date: Date, targetFormat: String): String {
            val format = SimpleDateFormat(targetFormat, Locale.getDefault())
            return format.format(date)
        }

        fun convertStringToDate(date: String, sourceFormat: String): Date? {
            val dateFormat = SimpleDateFormat(sourceFormat, Locale.getDefault())
            return dateFormat.parse(date)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getFullWeeksBetweenDateAndToday(dateString: String): Int {
            val formatter = DateTimeFormatter.ofPattern(DateFormat.FORMAT_DATE.toString())
            val startDate = LocalDate.parse(dateString, formatter)
            val endDate = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toDouble()

            return floor(daysBetween / 7).toInt()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getRoundedUpWeeksBetweenDateAndToday(dateString: String): Int {
            val formatter = DateTimeFormatter.ofPattern(DateFormat.FORMAT_DATE.toString())
            val startDate = LocalDate.parse(dateString, formatter)
            val endDate = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toDouble()

            return ceil(daysBetween / 7).toInt()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getFullWeeksBetweenDates(startDateString: String, endDateString: String): Int {
            val formatter = DateTimeFormatter.ofPattern(DateFormat.FORMAT_DATE.toString())
            val startDate = LocalDate.parse(startDateString, formatter)
            val endDate = LocalDate.parse(endDateString, formatter)
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toDouble()

            return floor(daysBetween / 7).toInt()
        }
    }
}