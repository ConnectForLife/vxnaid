package com.jnj.vaccinetracker.visitsoverview.dto

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity


data class ParticipantDataDTO(
    val participantUuid: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val birthDate: DateEntity,
    val attributes: Map<String, String>,
    val homeLocation: String?,
    val phone: String?,
) {
    val formattedBirthDate: String = ""

    val fullName: String
        get() = "$firstName $lastName"
}
