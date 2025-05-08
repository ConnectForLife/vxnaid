package com.jnj.vaccinetracker.register.dialogs

import com.soywiz.klock.DateTime

class VisitDateValidatorImpl : HistoricalVisitDateDialog.VisitDateValidator {
    private val usedDatesByVisitType = mutableMapOf<String, MutableSet<DateTime>>()

    override fun isDateAlreadyUsed(date: DateTime, visitType: String): Boolean {
        return usedDatesByVisitType.any { (type, dates) ->
            type != visitType && dates.contains(date)
        }
    }

    override fun isVisitAlreadyFilled(visitType: String): Boolean {
        return usedDatesByVisitType.containsKey(visitType)
    }

    fun addVisitDate(date: DateTime, visitType: String) {
        usedDatesByVisitType.getOrPut(visitType) { mutableSetOf() }.add(date)
    }
}