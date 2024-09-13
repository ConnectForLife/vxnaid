package com.jnj.vaccinetracker.common.data.models.api.request

import com.jnj.vaccinetracker.common.data.models.BirthDateDto
import com.jnj.vaccinetracker.common.data.models.api.response.AddressDto
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * @author druelens
 * @version 2
 */
@JsonClass(generateAdapter = true)
data class UpdateParticipantRequest(
    val participantUuid: String,
    val participantId: String?,
    val nin: String?,
    val updateDate: Date,
    val gender: Gender,
    val isBirthDateEstimated: Boolean?,
    val birthdate: BirthDateDto,
    val addresses: List<AddressDto>,
    val attributes: List<AttributeDto>,
    val image: String?, // Base64 representation of participant image
)