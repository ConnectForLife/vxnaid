package com.idi.vaccinetracker.participantflow.model

import android.os.Parcelable
import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.common.ui.model.DisplayValue
import kotlinx.parcelize.Parcelize


@Parcelize
data class ParticipantSummaryUiModel(
    val participantUuid: String,
    val participantId: String,
    val gender: Gender,
    val birthDateText: String,
    val isBirthDateEstimated: Boolean,
    // todo can be removed
    val vaccine: DisplayValue?,
    val participantPicture: ParticipantImageUiModel?,
) : Parcelable