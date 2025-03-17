package com.jnj.vaccinetracker.visitsoverview.dto

import android.os.Parcelable
import com.jnj.vaccinetracker.common.util.DateUtil
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParticipantDataDTO(
    val participantId: String,
    val fullName: String,
    val motherName: String,
    val birthDate: DateTime
) : Parcelable {
    val formattedBirthDate: String get() = DateUtil.convertDateToString(birthDate.toDate(),
        DateFormat.FORMAT_DATE.toString())
}
