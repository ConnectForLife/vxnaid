package com.idi.vaccinetracker.common.data.database.typealiases

import java.util.*

typealias DateEntity = Date

fun dateNow(): DateEntity = Date()

fun yearNow(): Int = Calendar.getInstance().get(Calendar.YEAR)

fun getTodayMidnight(): DateEntity {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return calendar.time
}

fun addDaysToDate(date: DateEntity, daysToAdd: Int): DateEntity {
    val calendar = Calendar.getInstance().apply {
        time = date
        add(Calendar.DAY_OF_YEAR, daysToAdd)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return calendar.time
}

