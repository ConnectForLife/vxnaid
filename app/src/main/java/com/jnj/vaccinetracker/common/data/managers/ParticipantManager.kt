package com.jnj.vaccinetracker.common.data.managers

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.GetPersonImageUseCase
import com.jnj.vaccinetracker.common.domain.usecases.MatchParticipantsUseCase
import com.jnj.vaccinetracker.common.domain.usecases.RegisterParticipantUseCase
import com.jnj.vaccinetracker.common.domain.usecases.UpdateParticipantUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.soywiz.klock.DateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @author druelens
 * @version 1
 */
@Singleton
class ParticipantManager @Inject constructor(
    private val matchParticipantsUseCase: MatchParticipantsUseCase,
    private val getPersonImageUseCase: GetPersonImageUseCase,
    private val registerParticipantUseCase: RegisterParticipantUseCase,
    private val updateParticipantUseCase: UpdateParticipantUseCase,
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    ) {

    /**
     * Match participant based on the authentication criteria.
     * Generates the parameters for the Multipart match API call and calls it.
     */
    suspend fun matchParticipants(
        participantId: String?,
        phone: String?,
        biometricsTemplateBytes: BiometricsTemplateBytes?,
        onProgressPercentChanged: OnProgressPercentChanged = {},
    ): List<ParticipantMatch> {
        return matchParticipantsUseCase.matchParticipants(
            ParticipantIdentificationCriteria(
                participantId = participantId,
                phone = phone,
                biometricsTemplate = biometricsTemplateBytes
            ), onProgressPercentChanged
        )
    }

    /**
     * Gets the person image for a person and decodes the base64 response to a byte array
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getPersonImage(personUuid: String): ImageBytes {
        return getPersonImageUseCase.getPersonImage(personUuid) ?: error("couldn't find person image for person $personUuid")
    }

    private fun createScheduleFirstVisit(): ScheduleFirstVisit {
        val locationUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException("Trying to register scheduled visit without a selected site")
        val operatorUUid = userRepository.getUser()?.uuid ?: throw OperatorUuidNotAvailableException("trying to register scheduled visit without stored operator uuid")
        return ScheduleFirstVisit(
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = dateNow(),
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUUid,
                Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to "1"
            )
        )

    }

    private fun getParticipantAttributes(
        birthWeight: String?,
        telephone: String?,
        siteUuid: String,
        language: String,
        fatherName: String?,
        motherName: String,
        participantName: String,
        childCategory: String?,
    ): MutableMap<String, String> {
        val operatorUUid = userRepository.getUser()?.uuid ?: throw OperatorUuidNotAvailableException("trying to register participant without stored operator uuid")

        val personAttributes = mutableMapOf(
            Constants.ATTRIBUTE_LOCATION to siteUuid,
            Constants.ATTRIBUTE_LANGUAGE to language,
            Constants.ATTRIBUTE_OPERATOR to operatorUUid,
            Constants.ATTRIBUTE_MOTHER_NAME to motherName,
            Constants.ATTRIBUTE_PARTICIPANT_NAME to participantName,
        )
        if (telephone != null) {
            personAttributes[Constants.ATTRIBUTE_TELEPHONE] = telephone
        }
        if (birthWeight != null) {
            personAttributes[Constants.ATTRIBUTE_BIRTH_WEIGHT] = birthWeight
        }
        // mother name can't be null
        if (fatherName != null) {
            personAttributes[Constants.ATTRIBUTE_FATHER_NAME] = fatherName
        }
        if (childCategory != null) {
            personAttributes[Constants.ATTRIBUTE_CHILD_CATEGORY] = childCategory
        }
        return personAttributes
    }

    data class RegisterDetails(
        val participantId: String,
        val nin: String?,
        val birthWeight: String?,
        val gender: Gender,
        val birthDate: DateTime,
        val isBirthDateEstimated: Boolean,
        val telephone: String?,
        val siteUuid: String,
        val language: String,
        val address: Address,
        val picture: ImageBytes?,
        val biometricsTemplateBytes: BiometricsTemplateBytes?,
        val fatherName: String?,
        val motherName: String,
        val participantName: String,
        val childCategory: String?,
    )

    @SuppressWarnings("LongParameterList")
    fun getRegisterParticipant(
        registerDetails: RegisterDetails
    ): RegisterParticipant {

        val personAttributes = getParticipantAttributes(
            birthWeight = registerDetails.birthWeight,
            telephone = registerDetails.telephone,
            siteUuid = registerDetails.siteUuid,
            language = registerDetails.language,
            fatherName = registerDetails.fatherName,
            motherName = registerDetails.motherName,
            participantName = registerDetails.participantName,
            childCategory = registerDetails.childCategory
        )

        return RegisterParticipant(
            participantId = registerDetails.participantId,
            nin = registerDetails.nin,
            gender = registerDetails.gender,
            isBirthDateEstimated = registerDetails.isBirthDateEstimated,
            birthDate = BirthDate(registerDetails.birthDate.unixMillisLong),
            address = registerDetails.address,
            attributes = personAttributes,
            image = registerDetails.picture,
            biometricsTemplate = registerDetails.biometricsTemplateBytes,
            scheduleFirstVisit = createScheduleFirstVisit()
        )
    }

    fun getUpdateParticipant(registerRequest: RegisterParticipant, participantUuid: String): UpdateParticipant {
            return UpdateParticipant(
                participantUuid = participantUuid,
                participantId = registerRequest.participantId,
                nin = registerRequest.nin,
                gender = registerRequest.gender,
                isBirthDateEstimated = registerRequest.isBirthDateEstimated,
                birthDate = registerRequest.birthDate,
                address = registerRequest.address,
                attributes = registerRequest.attributes,
                image = registerRequest.image,
                scheduleFirstVisit = createScheduleFirstVisit()
            )
    }


    suspend fun registerParticipant(request: RegisterParticipant): DraftParticipant {
        return registerParticipantUseCase.registerParticipant(request)
    }

    suspend fun updateParticipant(request: UpdateParticipant): DraftParticipant {
        return updateParticipantUseCase.updateParticipant(request)
    }

    suspend fun fullParticipantRegister(
        participantId: String,
        nin: String?,
        birthWeight: String?,
        gender: Gender,
        birthDate: DateTime,
        isBirthDateEstimated: Boolean,
        telephone: String?,
        siteUuid: String,
        language: String,
        address: Address,
        picture: ImageBytes?,
        biometricsTemplateBytes: BiometricsTemplateBytes?,
        fatherName: String?,
        motherName: String,
        participantName: String,
        childCategory: String?,
    ): DraftParticipant {
        val request = getRegisterParticipant(
            RegisterDetails(
                participantId = participantId,
                nin = nin,
                birthWeight = birthWeight,
                gender = gender,
                birthDate = birthDate,
                isBirthDateEstimated = isBirthDateEstimated,
                telephone = telephone,
                siteUuid = siteUuid,
                language = language,
                address = address,
                picture = picture,
                biometricsTemplateBytes = biometricsTemplateBytes,
                fatherName = fatherName,
                motherName = motherName,
                participantName = participantName,
                childCategory = childCategory
            )
        )
        return registerParticipant(request)
    }

    suspend fun fullParticipantUpdate(
        participantId: String,
        nin: String?,
        birthWeight: String?,
        gender: Gender,
        birthDate: DateTime,
        isBirthDateEstimated: Boolean,
        telephone: String?,
        siteUuid: String,
        language: String,
        address: Address,
        picture: ImageBytes?,
        biometricsTemplateBytes: BiometricsTemplateBytes?,
        fatherName: String?,
        motherName: String,
        participantName: String,
        childCategory: String?,
        participantUuid: String,
    ): DraftParticipant {
        val registerRequest = getRegisterParticipant(
            RegisterDetails(
                participantId = participantId,
                nin = nin,
                birthWeight = birthWeight,
                gender = gender,
                birthDate = birthDate,
                isBirthDateEstimated = isBirthDateEstimated,
                telephone = telephone,
                siteUuid = siteUuid,
                language = language,
                address = address,
                picture = picture,
                biometricsTemplateBytes = biometricsTemplateBytes,
                fatherName = fatherName,
                motherName = motherName,
                participantName = participantName,
                childCategory = childCategory
            )
        )
        val updateRequest = getUpdateParticipant(registerRequest, participantUuid)
        return updateParticipant(updateRequest)
    }

}