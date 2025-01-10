package com.idi.vaccinetracker.sync.data.models

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.data.models.BirthDateDto
import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.data.models.api.response.AddressDto
import com.idi.vaccinetracker.common.data.models.api.response.AttributeDto
import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import com.idi.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

typealias ParticipantSyncResponse = SyncResponse<ParticipantSyncRecord>

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ParticipantSyncRecord : SyncRecordBase {

    @TypeLabel(SyncRecordBase.TYPE_UPDATE)
    @JsonClass(generateAdapter = true)
    data class Update(
        override val participantUuid: String,
        override val dateModified: SyncDate,
        val participantId: String,
        val nin: String?,
        val childNumber: String?,
        val gender: Gender,
        val isBirthDateEstimated: Boolean?,
        val birthDate: BirthDateDto,
        val attributes: List<AttributeDto>,
        @Json(name = "addresses")
        val address: AddressDto?,
        val childFirstName: String?,
        val childLastName: String?
    ) : ParticipantSyncRecord() {
        val locationUuid: String? = attributes.find { it.type == Constants.ATTRIBUTE_LOCATION }?.value
    }

    @TypeLabel(SyncRecordBase.TYPE_DELETE)
    @JsonClass(generateAdapter = true)
    data class Delete(
        override val participantUuid: String,
        override val dateModified: SyncDate,
        val participantId : String,
    ) : ParticipantSyncRecord() {
        companion object {
            fun Delete.toDomain() = DeletedSyncRecord.Participant(participantUuid, dateModified, participantId)
        }
    }
}

fun List<AttributeDto>.toMap() = distinctBy { it.type }.map { it.type to it.value }.toMap()

fun ParticipantSyncRecord.toFailedSyncRecordDownload(dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
    return FailedSyncRecordDownload.Participant(
        participantUuid = participantUuid,
        dateModified = dateModified,
        dateLastDownloadAttempt = dateLastDownloadAttempt,
    )
}