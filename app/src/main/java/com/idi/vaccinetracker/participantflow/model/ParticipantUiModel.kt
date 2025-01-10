package com.idi.vaccinetracker.participantflow.model

import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.common.ui.model.DisplayValue

data class ParticipantUiModel(
    val participantUUID: String?,
    val participantId: String?,
    val irisMatchingScore: Int?,
    val birthDateText: String?,
    val isBirthDateEstimated: Boolean?,
    val gender: Gender?,
    val telephone: String?,
    val homeLocation: String?,
    val vaccine: DisplayValue?,
    val siteUUID: String?,
    val motherFirstName: String?,
    val motherLastName: String?,
    val childNumber: String?
) {

    constructor(
        participantId: String?,
        matchingScore: Int?,
        siteUUID: String?,
    ) : this(null, participantId, matchingScore, null, null, null, null, null, null, siteUUID, null, null, null)

}