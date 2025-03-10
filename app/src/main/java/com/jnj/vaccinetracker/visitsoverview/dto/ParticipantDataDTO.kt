package com.jnj.vaccinetracker.visitsoverview.dto


import android.os.Parcelable
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.util.DateUtil
import com.soywiz.klock.Date
import com.soywiz.klock.DateFormat
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParticipantDataDTO(
    val participantId: String,
    val fullName: String,
    val startDatetime: DateEntity,
    val motherName: String,
    val registrationDate: Date
) : Parcelable {
    val formattedStartDateTime: String get() = DateUtil.convertDateToString(startDatetime, DateFormat.FORMAT_DATE.toString())

}
