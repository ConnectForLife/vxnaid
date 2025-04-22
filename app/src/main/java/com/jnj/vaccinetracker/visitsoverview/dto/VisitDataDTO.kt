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
        get() = attributes["Visit type Vxnaid"] ?: ""

}
