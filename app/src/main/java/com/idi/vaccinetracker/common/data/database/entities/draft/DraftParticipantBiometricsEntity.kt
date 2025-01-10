package com.idi.vaccinetracker.common.data.database.entities.draft

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idi.vaccinetracker.common.data.database.entities.base.BiometricsTemplateBase
import com.idi.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.idi.vaccinetracker.common.data.database.entities.base.UploadableDraftWithDate
import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.DraftState

@Entity(tableName = "draft_participant_biometrics_template")
data class DraftParticipantBiometricsEntity(
    @PrimaryKey
    override val participantUuid: String,
    override val biometricsTemplateFileName: String,
    override val draftState: DraftState,
    override val dateLastUploadAttempt: DateEntity?,
) : ParticipantUuidContainer, BiometricsTemplateBase, UploadableDraftWithDate