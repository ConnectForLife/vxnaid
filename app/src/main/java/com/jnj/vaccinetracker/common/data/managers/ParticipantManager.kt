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
        fatherFirstName: String?,
        fatherLastName: String?,
        motherFirstName: String,
        motherLastName: String,
        childCategory: String?,
    ): MutableMap<String, String> {
        val operatorUUid = userRepository.getUser()?.uuid ?: throw OperatorUuidNotAvailableException("trying to register participant without stored operator uuid")

        val personAttributes = mutableMapOf(
            Constants.ATTRIBUTE_LOCATION to siteUuid,
            Constants.ATTRIBUTE_LANGUAGE to language,
            Constants.ATTRIBUTE_OPERATOR to operatorUUid,
            Constants.ATTRIBUTE_MOTHER_FIRST_NAME to motherFirstName,
            Constants.ATTRIBUTE_MOTHER_LAST_NAME to motherLastName
        )

        if (telephone != null) {
            personAttributes[Constants.ATTRIBUTE_TELEPHONE] = telephone
        }
        if (birthWeight != null) {
            personAttributes[Constants.ATTRIBUTE_BIRTH_WEIGHT] = birthWeight
        }

        if (fatherFirstName != null) {
            personAttributes[Constants.ATTRIBUTE_FATHER_FIRST_NAME] = fatherFirstName
        }

        if (fatherLastName != null) {
            personAttributes[Constants.ATTRIBUTE_FATHER_LAST_NAME] = fatherLastName
        }

        if (childCategory != null) {
            personAttributes[Constants.ATTRIBUTE_CHILD_CATEGORY] = childCategory
        }
        return personAttributes
    }

    data class RegisterDetails(
        val participantId: String,
        val nin: String?,
        val childNumber: String?,
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
        val motherFirstName: String,
        val motherLastName: String,
        val fatherFirstName: String?,
        val fatherLastName: String?,
        val childFirstName: String?,
        val childLastName: String?,
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
            fatherFirstName = registerDetails.fatherFirstName,
            fatherLastName = registerDetails.fatherLastName,
            motherFirstName = registerDetails.motherFirstName,
            motherLastName = registerDetails.motherLastName,
            childCategory = registerDetails.childCategory
        )

        return RegisterParticipant(
            participantId = registerDetails.participantId,
            nin = registerDetails.nin,
            childNumber = registerDetails.childNumber,
            gender = registerDetails.gender,
            isBirthDateEstimated = registerDetails.isBirthDateEstimated,
            birthDate = BirthDate(registerDetails.birthDate.unixMillisLong),
            address = registerDetails.address,
            attributes = personAttributes,
            image = registerDetails.picture,
            biometricsTemplate = registerDetails.biometricsTemplateBytes,
            scheduleFirstVisit = createScheduleFirstVisit(),
            childFirstName = registerDetails.childFirstName,
            childLastName = registerDetails.childLastName
        )
    }

    fun getUpdateParticipant(registerRequest: RegisterParticipant, participantUuid: String): UpdateParticipant {
            return UpdateParticipant(
                participantUuid = participantUuid,
                participantId = registerRequest.participantId,
                nin = registerRequest.nin,
                childNumber = registerRequest.childNumber,
                gender = registerRequest.gender,
                isBirthDateEstimated = registerRequest.isBirthDateEstimated,
                birthDate = registerRequest.birthDate,
                address = registerRequest.address,
                attributes = registerRequest.attributes,
                image = registerRequest.image,
                scheduleFirstVisit = createScheduleFirstVisit(),
                childFirstName = registerRequest.childFirstName,
                childLastName = registerRequest.childLastName
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
        childNumber: String?,
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
        fatherFirstName: String?,
        fatherLastName: String?,
        motherFirstName: String,
        motherLastName: String,
        childFirstName: String?,
        childLastName: String?,
        childCategory: String?,
    ): DraftParticipant {
        val request = getRegisterParticipant(
            RegisterDetails(
                participantId = participantId,
                nin = nin,
                childNumber = childNumber,
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
                fatherFirstName = fatherFirstName,
                fatherLastName = fatherLastName,
                motherFirstName = motherFirstName,
                motherLastName = motherLastName,
                childFirstName = childFirstName,
                childLastName = childLastName,
                childCategory = childCategory
            )
        )
        return registerParticipant(request)
    }

    suspend fun fullParticipantUpdate(
        participantId: String,
        nin: String?,
        childNumber: String?,
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
        fatherFirstName: String?,
        fatherLastName: String?,
        motherFirstName: String,
        motherLastName: String,
        childFirstName: String?,
        childLastName: String?,
        childCategory: String?,
        participantUuid: String,
    ): DraftParticipant {
        val registerRequest = getRegisterParticipant(
            RegisterDetails(
                participantId = participantId,
                nin = nin,
                childNumber = childNumber,
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
                fatherFirstName = fatherFirstName,
                fatherLastName = fatherLastName,
                motherFirstName = motherFirstName,
                motherLastName = motherLastName,
                childFirstName = childFirstName,
                childLastName = childLastName,
                childCategory = childCategory
            )
        )
        val updateRequest = getUpdateParticipant(registerRequest, participantUuid)
        return updateParticipant(updateRequest)
    }

}