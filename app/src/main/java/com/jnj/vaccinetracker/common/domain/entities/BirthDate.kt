package com.jnj.vaccinetracker.common.domain.entities

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import java.util.Calendar

data class BirthDate(val time: Long) {
    val year: Int
    val month: Int
    val day: Int

    init {
        // Use device local timezone so that dates returned from OpenMRS (which stores dates in
        // the facility's local timezone) are interpreted correctly. klock DateTime(time) is UTC-only
        // and would shift the day back by the UTC offset (e.g. UTC+3 causes -1 day).
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        year = cal.get(Calendar.YEAR)
        month = cal.get(Calendar.MONTH) + 1
        day = cal.get(Calendar.DAY_OF_MONTH)
    }

    fun toDateTime(): DateTime {
        return DateTime(year, month, day)
    }

    fun birthDateToString(): String {
        return toDateTime().format(DateFormat.FORMAT_DATE)
    }

    companion object {
        fun yearOfBirth(yearOfBirth: Int) = BirthDate(DateTime(yearOfBirth, 1, 1).unixMillisLong)
        fun yearOfBirth(yearOfBirth: String) = yearOfBirth(yearOfBirth.toInt())
    }
}
