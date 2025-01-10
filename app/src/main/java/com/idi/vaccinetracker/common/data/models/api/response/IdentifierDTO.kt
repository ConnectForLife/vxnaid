package com.idi.vaccinetracker.common.data.models.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IdentifierDTO(
    val participantUuid: String,
    val identifierTypeName: String,
    val identifierValue: String,
)
