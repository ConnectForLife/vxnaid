package com.idi.vaccinetracker.visitsoverview.dto

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.ObservationValue
import com.idi.vaccinetracker.common.domain.entities.ParticipantBase
import com.idi.vaccinetracker.common.util.DateUtil
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
}
