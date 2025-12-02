package com.jnj.vaccinetracker.visitsoverview.dto

import android.os.Parcelable
import com.jnj.vaccinetracker.common.util.DateUtil
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class ParticipantDataDTO(
    val participantId: String,
    val fullName: String,
    val motherName: String,
    val birthDate: DateTime,
    val registrationDate: Date
) : Parcelable {
    val formattedRegistrationDate: String get() = DateUtil.convertDateToString(registrationDate,
        DateFormat.FORMAT_DATE.toString())
}
