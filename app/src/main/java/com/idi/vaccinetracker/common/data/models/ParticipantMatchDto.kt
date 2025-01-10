package com.idi.vaccinetracker.common.data.models

import com.idi.vaccinetracker.common.data.models.api.response.AddressDto
import com.idi.vaccinetracker.common.data.models.api.response.AttributeDto
import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.common.domain.entities.ParticipantMatch
import com.idi.vaccinetracker.sync.data.models.toMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ParticipantMatchDto(
    val uuid: String,
    val participantId: String,
    val matchingScore: Int?,
    val gender: Gender,
    val birthDate: BirthDateDto,
    val childNumber: String?,
    @Json(name = "addresses") val address: AddressDto?,
    val attributes: List<AttributeDto>,
) {
    val telephoneNumber: String?
        get() = attributes.find { it.type == Constants.ATTRIBUTE_TELEPHONE }?.value
}

fun ParticipantMatchDto.toDomain() = ParticipantMatch(
    uuid = uuid,
    participantId = participantId,
    matchingScore = matchingScore,
    gender = gender,
    birthDate = birthDate.toDomain(),
    childNumber = childNumber,
    address = address,
    attributes = attributes.toMap()
)