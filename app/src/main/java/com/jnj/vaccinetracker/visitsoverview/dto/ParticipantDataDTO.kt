package com.jnj.vaccinetracker.visitsoverview.dto


import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.util.DateUtil
import com.soywiz.klock.Date
import com.soywiz.klock.DateFormat

data class ParticipantDataDTO(
    val participantId: String,
    val fullName: String,
    val startDatetime: DateEntity,
    val motherName: String,
    val registrationDate: Date
) {
    val formattedStartDateTime: String get() = DateUtil.convertDateToString(startDatetime, DateFormat.FORMAT_DATE.toString())

}
