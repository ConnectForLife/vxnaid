package com.jnj.vaccinetracker.visitsoverview.dto

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.util.DateUtil
import com.soywiz.klock.DateFormat

data class VisitDataDTO(
    val visitUuid: String,
    val startDatetime: DateEntity,
    val attributes: Map<String, String>,
    val observations: Map<String, ObservationValue>,
    val visitType: String,
    val participant: ParticipantBase
) {
    val formattedStartDateTime: String get() = DateUtil.convertDateToString(startDatetime, DateFormat.FORMAT_DATE.toString())
    val visitype: String
    get() {
        val visitTypeVxnaid = attributes["Visit type Vxnaid"]
        if (!visitTypeVxnaid.isNullOrEmpty()) {
            return visitTypeVxnaid
        } else {
            val doseNumber = attributes["Dose number"] ?: attributes["Dose Number"] ?: ""
            return when (doseNumber) {
                "1" -> "At Birth"
                "2" -> "6 weeks"
                "3" -> "10 weeks"
                "4" -> "14 weeks"
                "5" -> "6 months"
                "6" -> "7 months"
                "7" -> "8 months"
                "8" -> "9 months"
                "9" -> "1 year"
                "10" -> "18 months"
                "11" -> "2 years"
                else -> "Unknown"
            }
        }
    }


}
