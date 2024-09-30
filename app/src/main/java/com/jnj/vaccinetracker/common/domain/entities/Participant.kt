package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.Constants

sealed class ParticipantBase {
    abstract val participantUuid: String
    abstract val nin: String?
    abstract val image: ParticipantImageFileBase?
    abstract val biometricsTemplate: ParticipantBiometricsTemplateFileBase?
    abstract val participantId: String
    abstract val childNumber: String?
    abstract val gender: Gender
    abstract val isBirthDateEstimated: Boolean?
    abstract val birthDate: BirthDate
    abstract val attributes: Map<String, String>
    abstract val address: Address?
    abstract val childFirstName: String?
    abstract val childLastName: String?

    val phone: String? get() = attributes[Constants.ATTRIBUTE_TELEPHONE]
    val locationUuid: String? get() = attributes[Constants.ATTRIBUTE_LOCATION]
    val originalParticipantId: String? get() = attributes[Constants.ATTRIBUTE_ORIGINAL_PARTICIPANT_ID]
    val regimen: String? get() = attributes[Constants.ATTRIBUTE_VACCINE]
    val birthWeight: String? get() = attributes[Constants.ATTRIBUTE_BIRTH_WEIGHT]
    val fatherFirstname: String? get() = attributes[Constants.ATTRIBUTE_FATHER_FIRST_NAME]
    val fatherLastName: String? get() = attributes[Constants.ATTRIBUTE_FATHER_LAST_NAME]
    val motherFirstName: String? get() = attributes[Constants.ATTRIBUTE_MOTHER_FIRST_NAME]
    val motherLastName: String? get() = attributes[Constants.ATTRIBUTE_MOTHER_LAST_NAME]
    val childCategory: String? get() = attributes[Constants.ATTRIBUTE_CHILD_CATEGORY]
}

data class Participant(
    override val participantUuid: String,
    override val dateModified: DateEntity,
    override val image: ParticipantImageFile?,
    override val biometricsTemplate: ParticipantBiometricsTemplateFile?,
    override val participantId: String,
    override val childNumber: String?,
    override val nin: String?,
    override val gender: Gender,
    override val isBirthDateEstimated: Boolean?,
    override val birthDate: BirthDate,
    override val attributes: Map<String, String>,
    override val address: Address?,
    override val childFirstName: String?,
    override val childLastName: String?
) : ParticipantBase(), SyncBase {

}

data class DraftParticipant(
    override val participantUuid: String,
    val registrationDate: DateEntity,
    override val image: DraftParticipantImageFile?,
    override val biometricsTemplate: DraftParticipantBiometricsTemplateFile?,
    override val participantId: String,
    override val childNumber: String?,
    override val nin: String?,
    override val gender: Gender,
    override val isBirthDateEstimated: Boolean?,
    override val birthDate: BirthDate,
    override val attributes: Map<String, String>,
    override val address: Address?,
    override val childFirstName: String?,
    override val childLastName: String?,
    override val draftState: DraftState,
    val isUpdate: Boolean = false,
) : ParticipantBase(), SyncBase, UploadableDraft {
    override val dateModified: DateEntity get() = registrationDate
}

fun Map<String, String>.withOriginalParticipantId(participantId: String?): Map<String, String> {
    val participantIdKey = Constants.ATTRIBUTE_ORIGINAL_PARTICIPANT_ID
    return participantId?.let { this + mapOf(participantId to it) } ?: filterKeys { it != participantIdKey }
}

fun Map<String, String>.withPhone(phone: String?): Map<String, String> {
    val phoneKey = Constants.ATTRIBUTE_TELEPHONE
    return phone?.let { this + mapOf(phoneKey to it) } ?: filterKeys { it != phoneKey }
}

fun Map<String, String>.withLocationUuid(locationUuid: String?): Map<String, String> {
    val locationKey = Constants.ATTRIBUTE_LOCATION
    return locationUuid?.let { this + mapOf(locationKey to it) } ?: filterKeys { it != locationKey }
}

fun Map<String, String>.withBirthWeight(birthWeight: String?): Map<String,String> {
    val birthWeightKey = Constants.ATTRIBUTE_BIRTH_WEIGHT
    return birthWeight?.let { this + mapOf(birthWeightKey to it) } ?: filterKeys { it != birthWeightKey}
}

fun DraftParticipant.toParticipantWithoutAssets(): Participant = Participant(
    participantUuid = participantUuid,
    dateModified = dateModified,
    image = null,
    biometricsTemplate = null,
    participantId = participantId,
    nin = nin,
    childNumber = childNumber,
    gender = gender,
    isBirthDateEstimated = isBirthDateEstimated,
    birthDate = birthDate,
    attributes = attributes,
    address = address,
    childFirstName = childFirstName,
    childLastName = childLastName
)
