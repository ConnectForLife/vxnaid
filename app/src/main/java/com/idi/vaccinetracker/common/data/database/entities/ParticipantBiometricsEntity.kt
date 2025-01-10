package com.idi.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idi.vaccinetracker.common.data.database.entities.base.BiometricsTemplateBase
import com.idi.vaccinetracker.common.data.database.entities.base.ParticipantSyncBase
import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

@Entity(tableName = "participant_biometrics_template")
data class ParticipantBiometricsEntity(
    @PrimaryKey
    override val participantUuid: String,
    override val biometricsTemplateFileName: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : ParticipantSyncBase, BiometricsTemplateBase