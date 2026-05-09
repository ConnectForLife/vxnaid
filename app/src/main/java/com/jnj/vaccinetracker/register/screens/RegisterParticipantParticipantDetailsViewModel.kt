package com.jnj.vaccinetracker.register.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftParticipantDao
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.data.models.api.response.IdentifierDTO
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantIdUseCase
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GenerateUniqueParticipantIdUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetAddressMasterDataOrderUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetTempBiometricsTemplatesBytesUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.exceptions.ParticipantAlreadyExistsException
import com.jnj.vaccinetracker.common.exceptions.ParticipantNinAlreadyExistsException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.model.DisplayValue
import com.jnj.vaccinetracker.common.validators.NinValidator
import com.jnj.vaccinetracker.common.validators.ParticipantIdValidator
import com.jnj.vaccinetracker.common.validators.PhoneValidator
import com.jnj.vaccinetracker.common.validators.TextInputValidator
import com.jnj.vaccinetracker.common.viewmodel.MutableLiveDataUnique
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toDomain
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import com.jnj.vaccinetracker.sync.domain.usecases.error.FindUnresolvedSyncErrorKeysByTypeUseCase
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import kotlin.random.Random

@SuppressWarnings("TooManyFunctions")
@RequiresApi(Build.VERSION_CODES.O)
class RegisterParticipantParticipantDetailsViewModel @Inject constructor(
    private val phoneValidator: PhoneValidator,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val configurationManager: ConfigurationManager,
    private val resourcesWrapper: ResourcesWrapper,
    private val participantManager: ParticipantManager,
    override val dispatchers: AppCoroutineDispatchers,
    private val participantIdValidator: ParticipantIdValidator,
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val getTempBiometricsTemplatesBytesUseCase: GetTempBiometricsTemplatesBytesUseCase,
    private val fullPhoneFormatter: FullPhoneFormatter,
    private val generateUniqueParticipantIdUseCase: GenerateUniqueParticipantIdUseCase,
    private val textInputValidator: TextInputValidator,
    private val ninValidator: NinValidator,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    private val getAddressMasterDataOrderUseCase: GetAddressMasterDataOrderUseCase,
    private val draftParticipantDao: DraftParticipantDao,
    private val findParticipantByParticipantIdUseCase: FindParticipantByParticipantIdUseCase,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val findUnresolvedSyncErrorKeysByTypeUseCase: FindUnresolvedSyncErrorKeysByTypeUseCase,
    private val syncErrorRepository: SyncErrorRepository,
    ) : ViewModelBase() {

    companion object {
        /**
         * wait this long before we validate a field while typing
         */
        private val INLINE_VALIDATION_DELAY = 2.seconds

        fun calculateAgeFromDate(birthDate: DateTime): String {
            val now = DateTime.now()
            val years = now.yearInt - birthDate.yearInt
            val months = now.month1 - birthDate.month1
            val days = now.dayOfMonth - birthDate.dayOfMonth

            val adjustedMonths = if (days < 0) months - 1 else months
            val adjustedYears = if (adjustedMonths < 0) years - 1 else years

            val totalMonths = (adjustedYears * 12) + adjustedMonths

            val daysUntilNow = now.unixMillisLong / (1000 * 60 * 60 * 24)
            val daysUntilBirthDate = birthDate.unixMillisLong / (1000 * 60 * 60 * 24)
            val totalDays = (daysUntilNow - daysUntilBirthDate).toInt()

            return when {
                totalDays < 30 -> "$totalDays days"
                totalMonths < 24 -> "$totalMonths months"
                else -> "$adjustedYears years"
            }
        }
    }

    data class Args(
            val participantId: String?,
            val isManualSetParticipantID: Boolean,
            val leftEyeScanned: Boolean,
            val rightEyeScanned: Boolean,
            val phoneNumber: String?,
            val participantUuid: String?,
            val registerDetails: ParticipantManager.RegisterDetails?,
            val duplicateError: String? = null,
    )

    private val args = stateFlow<Args?>(null)

    val registerFailedEvents = eventFlow<String>()
    val registerNoPhoneEvents = eventFlow<Unit>()
    val registerNoMatchingIdEvents = eventFlow<Unit>()
    val registerChildNewbornEvents = eventFlow<ParticipantManager.RegisterDetails>()
    val registerParticipantSuccessDialogEvents = eventFlow<ParticipantSummaryUiModel>()
    val updateParticipantSuccessDialogEvents = eventFlow<ParticipantSummaryUiModel>()

    val loading = mutableLiveBoolean()

    val participantId = mutableLiveData<String?>()
    val participantIdValidationMessage = mutableLiveData<String>()

    val participantUuid = mutableLiveData<String?>()
    val scannedParticipantId = mutableLiveData<String?>()
    val duplicateError = mutableLiveData<String?>(null)

    val isManualSetParticipantID = mutableLiveBoolean()
    val isAutoGeneratedParticipantId = mutableLiveBoolean()

    val nin = mutableLiveData<String?>()
    val ninValidationMessage = mutableLiveData<String>()

    val childNumber = mutableLiveData<String?>()
    val childNumberValidationMessage = mutableLiveData<String>()

    val childFirstName = mutableLiveData<String>()
    val childFirstNameValidationMessage = mutableLiveData<String>()
    val childLastName = mutableLiveData<String>()
    val childLastNameValidationMessage = mutableLiveData<String>()

    val motherFirstName = mutableLiveData<String>()
    val motherFirstNameValidationMessage = mutableLiveData<String>()
    val motherLastName = mutableLiveData<String>()
    val motherLastNameValidationMessage = mutableLiveData<String>()

    val fatherFirstName = mutableLiveData<String>()
    val fatherFirstNameValidationMessage = mutableLiveData<String>()
    val fatherLastName = mutableLiveData<String>()
    var fatherLastNameValidationMessage = mutableLiveData<String>()

    val birthWeight = mutableLiveData<String>()
    val birthWeightValidationMessage = mutableLiveData<String>()
    val bestContactTime = mutableLiveData<String>()
    val bestContactTimeValidationMessage = mutableLiveData<String>()
    val childCategory = mutableLiveData<DisplayValue>()
    val childCategoryValidationMessage = mutableLiveData<String>()
    val childCategoryNames = mutableLiveData<List<DisplayValue>>()
    val birthDate = mutableLiveData<DateTime>()
    val birthDateText = mutableLiveData<String>()
    val birthDateValidationMessage = mutableLiveData<String>()

    val estimatedAgeText = mutableLiveData<String>()
    val estimatedAgeValidationMessage = mutableLiveData<String>()
    val isBirthDateEstimated = mutableLiveData<Boolean>()

    val leftIrisScanned = mutableLiveBoolean()
    val rightIrisScanned = mutableLiveBoolean()
    val gender = mutableLiveData<Gender>()
    val defaultPhoneCountryCode = mutableLiveData<String>()
    private val phoneCountryCode = mutableLiveData<String>()
    val phone = mutableLiveData<String>()
    val homeLocationLabel = mutableLiveData<String>()
    val homeLocation = mutableLiveData<Address>()
    val vaccine = mutableLiveData<DisplayValue>()
    val language = mutableLiveData<DisplayValue>()
    val genderValidationMessage = mutableLiveData<String>()
    val phoneValidationMessage = mutableLiveData<String>()
    val homeLocationValidationMessage = mutableLiveData<String>()
    val languageValidationMessage = mutableLiveData<String>()

    val vaccineNames = mutableLiveData<List<DisplayValue>>()
    val languages = mutableLiveData<List<DisplayValue>>()
    val ninIdentifiers = mutableLiveData<NinIdentifiersList>()

    var visitTypes = mutableLiveData<List<String>>()

    var canSkipPhone = false
    private val irisScans = ArrayMap<IrisPosition, Boolean>()

    var hasChildBeenVaccinatedAlreadyAsked = false
    var registerParticipantRequest = mutableLiveData<RegisterParticipant>()

    private var validatePhoneJob: Job? = null
    private var validateParticipantIdJob: Job? = null

    private var originalNinValue: String? = null

    init {
        initState()
    }

    private suspend fun load(args: Args) {
        loading.set(true)
        try {
            visitTypes.value = configurationManager.getSubstancesConfig().map { it.visitType }.distinct()
            val config = configurationManager.getConfiguration()
            isAutoGeneratedParticipantId.value = config.isAutoGenerateParticipantId
            if (config.isAutoGenerateParticipantId) {
                isManualSetParticipantID.value = false
                participantId.value = generateUniqueParticipantIdUseCase.generateUniqueParticipantId()
            } else {
                isManualSetParticipantID.value = args.isManualSetParticipantID
                participantId.value = args.participantId
                if (!isManualSetParticipantID.value) {
                    scannedParticipantId.value = participantId.value
                }
            }
            leftIrisScanned.set(args.leftEyeScanned)
            rightIrisScanned.set(args.rightEyeScanned)
            irisScans[IrisPosition.LEFT] = args.leftEyeScanned
            irisScans[IrisPosition.RIGHT] = args.rightEyeScanned

            args.phoneNumber?.let {
                phone.set(it)
            }

            participantUuid.set(args.participantUuid)
            duplicateError.set(args.duplicateError)

            val site = syncSettingsRepository.getSiteUuid()?.let { configurationManager.getSiteByUuid(it) }
                    ?: throw NoSiteUuidAvailableException()
            val configuration = configurationManager.getConfiguration()
            val loc = configurationManager.getLocalization()
            onSiteAndConfigurationLoaded(site, configuration, loc)
            setNinIdentifiers()
            onParticipantBack(args)
            onParticipantEdit()
            originalNinValue = nin.value
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            logError("Failed to get site by uuid: ", ex)
        }
    }

    private suspend fun setNinIdentifiers() {
        val remoteNINs = configurationManager.getNinIdentifiers()
        val localNINs = draftParticipantDao.findAllByDraftState(DraftState.UPLOAD_PENDING)
            .filter { draftParticipant -> draftParticipant?.nin != null }
            .map { draftParticipant ->
                IdentifierDTO(
                    draftParticipant!!.participantUuid,
                    Constants.NIN_IDENTIFIER_TYPE_NAME,
                    draftParticipant.nin ?: ""
                )
            }
        ninIdentifiers.set(remoteNINs + localNINs)
    }

    suspend fun onParticipantEdit() {
        if (participantUuid.value != null) {
            val participantBase = findParticipantByParticipantUuidUseCase.findByParticipantUuid(participantUuid.value!!)
            participantBase?.participantId?.let { setParticipantId(it) }
            participantBase?.childNumber?.let { setChildNumber(it) }
            participantBase?.nin?.let { setNin(it) }
            participantBase?.birthWeight?.let { setBirthWeight(it) }
            participantBase?.bestContactTime?.let { setBestContactTime(it) }
            participantBase?.gender?.let { setGender(it) }
            setBirthDateOrEstimatedAge(participantBase?.birthDate?.toDateTime(), participantBase?.isBirthDateEstimated ?: false)
            participantBase?.phone?.let { setPhone(it, true) }
            participantBase?.motherFirstName?.let { setMotherFirstName(it) }
            participantBase?.motherLastName?.let { setMotherLastName(it) }
            participantBase?.fatherFirstname?.let { setFatherFirstName(it) }
            participantBase?.fatherLastName?.let { setFatherLastName(it) }
            participantBase?.childFirstName?.let { setChildFirstName(it) }
            participantBase?.childLastName?.let { setChildLastName(it) }
            participantBase?.childCategory?.let { category ->
                setSelectedChildCategory(DisplayValue(category, category))
            }
            participantBase?.attributes?.get(Constants.ATTRIBUTE_LANGUAGE)?.let { language ->
                setSelectedLanguage(DisplayValue(language, language))
            }
            participantBase?.address?.let { address ->
                val stringRepresentation = address.toStringRepresentation(configurationManager, getAddressMasterDataOrderUseCase)
                setHomeLocation(address, stringRepresentation)
            }
        }
    }

    suspend fun onParticipantBack(args: Args) {
        if (args.registerDetails != null) {
            args.registerDetails.participantId.let { setParticipantId(it) }
            args.registerDetails.childNumber?.let { setChildNumber(it) }
            args.registerDetails.nin?.let { setNin(it) }
            args.registerDetails.birthWeight?.let { setBirthWeight(it) }
            args.registerDetails.bestContactTime?.let { setBestContactTime(it) }
            args.registerDetails.gender.let { setGender(it) }
            setBirthDateOrEstimatedAge(args.registerDetails.birthDate, args.registerDetails.isBirthDateEstimated)
            args.registerDetails.motherFirstName.let { setMotherFirstName(it) }
            args.registerDetails.motherLastName.let { setMotherLastName(it) }
            args.registerDetails.fatherFirstName.let { setFatherFirstName(it) }
            args.registerDetails.fatherLastName.let { setFatherLastName(it) }
            args.registerDetails.childFirstName?.let { setChildFirstName(it) }
            args.registerDetails.childLastName?.let { setChildLastName(it) }
            args.registerDetails.childCategory?.let { category ->
                setSelectedChildCategory(DisplayValue(category, category))
            }
            args.registerDetails.language?.let { language ->
                setSelectedLanguage(DisplayValue(language, language))
            }
            args.registerDetails.address.let { address ->
                val stringRepresentation = address.toStringRepresentation(configurationManager, getAddressMasterDataOrderUseCase)
                setHomeLocation(address, stringRepresentation)
            }
        }
    }

    private fun initState() {
        args.filterNotNull().distinctUntilChanged()
                .onEach { args ->
                    load(args)
                }.launchIn(scope)
    }

    fun setArguments(args: Args) {
        this.args.tryEmit(args)
    }

    private fun onSiteAndConfigurationLoaded(site: Site, configuration: Configuration, loc: TranslationMap) {
        defaultPhoneCountryCode.set(site.countryCode)
        if (phoneCountryCode.get() == null) phoneCountryCode.set(site.countryCode)

        vaccineNames.set(configuration.vaccines.map { vaccine ->
            DisplayValue(vaccine.name, loc[vaccine.name])
        })

        languages.set(configuration.personLanguages.map { language ->
            DisplayValue(language.name, loc[language.name]) })
    }

    private suspend fun ImageBytes.compress() = ImageHelper.compressRawImage(this, dispatchers.io)

    @SuppressWarnings("LongParameterList", "LongMethod")
    fun submitRegistration(
            picture: ParticipantImageUiModel?,
    ) {
        scope.launch {
            doRegistration(picture)
        }
    }

    private suspend fun doRegistration(
            picture: ParticipantImageUiModel?,
    ) {
        val siteUuid = syncSettingsRepository.getSiteUuid()
                ?: return logWarn("Cannot submit registration: no site UUID known")
        val homeLocation = homeLocation.get()
        val participantId = participantId.get()
        val nin = nin.get()
        val childNumber = childNumber.get()
        val birthWeight = birthWeight.get()
        val bestContactTimeValue = bestContactTime.get()
        val gender = gender.get()
        val birthDate = birthDate.get()
        val isBirthDateEstimated = isBirthDateEstimated.get()
        val fullPhoneNumber = createFullPhone()
        val motherFirstName = motherFirstName.get()
        val motherLastName = motherLastName.get()
        val fatherFirstName = fatherFirstName.get()
        val fatherLastName = fatherLastName.get()
        val childFirstName = childFirstName.get()
        val childLastName = childLastName.get()
        val childUuid = participantUuid.get()
        val childCategoryValue = childCategory.get()?.value
        val languageValue = language.get()?.value

        val areInputsValid = validateInputs(participantId, gender, birthDate, homeLocation,
            motherFirstName, motherLastName, fatherFirstName, fatherLastName, childFirstName,
            childLastName, childNumber, birthWeight, bestContactTimeValue, nin)

        var phoneNumberToSubmit: String? = null

        //if manual entered participantId check if it is matching incoming one
        if (isManualSetParticipantID.get()) {
            logInfo("participantId: $participantId")
            registerNoMatchingIdEvents.tryEmit(Unit)
            return
        }

        // Validate the phone number input. If empty, it shows the dialog that it can be skipped.
        if (areInputsValid && phone.get().isNullOrEmpty() && !canSkipPhone) {
            registerNoPhoneEvents.tryEmit(Unit)
            return
        } else if (!phone.get().isNullOrEmpty() && !phoneValidator.validate(fullPhoneNumber)) {
            phoneValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_phone))
            return
        } else if (!phone.get().isNullOrEmpty()) {
            phoneNumberToSubmit = fullPhoneNumber
        }
        if (!areInputsValid)
            return

        loading.set(true)

        try {
            val compressedImage = picture?.toDomain()?.compress()
            val biometricsTemplateBytes = getTempBiometricsTemplatesBytesUseCase.getBiometricsTemplate(irisScans)
            val registerDetails = ParticipantManager.RegisterDetails(
                participantId = participantId!!,
                nin = nin,
                childNumber = childNumber,
                birthWeight = birthWeight,
                bestContactTime = bestContactTimeValue,
                gender = gender!!,
                birthDate = birthDate!!,
                isBirthDateEstimated = isBirthDateEstimated!!,
                telephone = phoneNumberToSubmit,
                siteUuid = siteUuid,
                language = languageValue,
                address = homeLocation!!,
                picture = compressedImage,
                biometricsTemplateBytes = biometricsTemplateBytes,
                motherFirstName = motherFirstName!!,
                motherLastName = motherLastName!!,
                fatherFirstName = fatherFirstName,
                fatherLastName = fatherLastName,
                childFirstName = childFirstName,
                childLastName = childLastName,
                childCategory = childCategoryValue,
                dateCreated = dateNow().time
            )
            val registerRequest = participantManager.getRegisterParticipant(registerDetails)

            if (!hasChildBeenVaccinatedAlreadyAsked && participantUuid.value == null) {
                registerParticipantRequest.value = registerRequest
                registerChildNewbornEvents.tryEmit(registerDetails)
                return
            }

            val result: DraftParticipant = if (childUuid != null) {
                if (!duplicateError.value.isNullOrEmpty()) {
                    val draft: DraftParticipant? = draftParticipantRepository.findByParticipantUuid(childUuid)

                    draft?.let { draftToUpdate ->
                        val updatedDraft = when (duplicateError.value) {
                            ParticipantAlreadyExistsException.toStringExceptionName() -> draftToUpdate.copy(participantId = participantId)
                            ParticipantNinAlreadyExistsException.toStringExceptionName() -> draftToUpdate.copy(nin = nin)
                            else -> draftToUpdate // No change for other errors
                        }
                        draftParticipantRepository.updateDraftParticipant(updatedDraft)

                        val errorKeys = findUnresolvedSyncErrorKeysByTypeUseCase
                            .findUnresolvedSyncErrorKeysByType(SyncErrorMetadata.UploadParticipantPendingCall.TYPE)

                        val keysToDelete = errorKeys.filter { key ->
                            key.contains(draftToUpdate.participantUuid)
                        }
                        syncErrorRepository.deleteByKeysAndErrorState(keysToDelete, SyncErrorState.UPLOADED)

                        draftToUpdate
                    } ?: throw IllegalStateException("DraftParticipant not found for UUID: $childUuid")
                } else {
                    val updateRequest = participantManager.getUpdateParticipant(registerRequest, childUuid)
                    participantManager.updateParticipant(updateRequest)
                }
            } else {
                participantManager.registerParticipant(registerRequest)
            }

            loading.set(false)

            val participant = ParticipantSummaryUiModel(
                                result.participantUuid,
                                participantId,
                                gender,
                                birthDate.format(DateFormat.FORMAT_DATE),
                                isBirthDateEstimated,
                                null,
                                compressedImage?.let { ParticipantImageUiModel(it.bytes) }
            )

            if (participantUuid.value != null) {
                updateParticipantSuccessDialogEvents.tryEmit(participant)
                return
            }

            registerParticipantSuccessDialogEvents.tryEmit(participant)

        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            logError("Failed to register participant: ", ex)
            when (ex) {
                is ParticipantAlreadyExistsException -> {
                    val errorMessage = resourcesWrapper.getString(R.string.participant_registration_details_error_participant_already_exists)
                    participantIdValidationMessage.set(errorMessage)
                    registerFailedEvents.tryEmit(errorMessage)
                }

                is ParticipantNinAlreadyExistsException -> {
                    val errorMessage = resourcesWrapper.getString(R.string.participant_registration_details_error_nin_already_exist)
                    ninValidationMessage.set(errorMessage)
                    registerFailedEvents.tryEmit(errorMessage)
                }

                is OperatorUuidNotAvailableException -> {
                    sessionExpiryObserver.notifySessionExpired()
                }

                else -> {
                    registerFailedEvents.tryEmit(resourcesWrapper.getString(R.string.general_label_error))
                }
            }
        }
    }

    suspend fun doRegistrationUsingRegisterRequest(
        request: RegisterParticipant,
    ): ParticipantSummaryUiModel {
        loading.set(true)

        try {
            val result = participantManager.registerParticipant(request)
            loading.set(false)

            val participant = ParticipantSummaryUiModel(
                result.participantUuid,
                request.participantId,
                request.gender,
                request.birthDate.toDateTime().format(DateFormat.FORMAT_DATE),
                request.isBirthDateEstimated,
                null,
                request.image?.let { ParticipantImageUiModel(it.bytes) }
            )
            return participant
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            logError("Failed to register participant: ", ex)
            when (ex) {
                is ParticipantAlreadyExistsException -> {
                    val errorMessage = resourcesWrapper.getString(R.string.participant_registration_details_error_participant_already_exists)
                    participantIdValidationMessage.set(errorMessage)
                    registerFailedEvents.tryEmit(errorMessage)
                }

                is ParticipantNinAlreadyExistsException -> {
                    val errorMessage = resourcesWrapper.getString(R.string.participant_registration_details_error_nin_already_exist)
                    ninValidationMessage.set(errorMessage)
                    registerFailedEvents.tryEmit(errorMessage)
                }

                is OperatorUuidNotAvailableException -> {
                    sessionExpiryObserver.notifySessionExpired()
                }

                else -> {
                    registerFailedEvents.tryEmit(resourcesWrapper.getString(R.string.general_label_error))
                }
            }
            throw ex
        }
    }

    @SuppressWarnings("LongParameterList")
    private suspend fun validateInputs(
        participantId: String?,
        gender: Gender?,
        birthDate: DateTime?,
        homeLocation: Address?,
        motherFirstName: String?,
        motherLastName: String?,
        fatherFirstName: String?,
        fatherLastName: String?,
        childFirstName: String?,
        childLastName: String?,
        childNumber: String?,
        birthWeight: String?,
        bestContactTime: String?,
        nin: String?
    ): Boolean {
        var isValid = true
        val validationErrors: MutableList<String> = mutableListOf()

        resetValidationMessages()
        resetValidationErrors()

        fun addValidationError(isValidField: Boolean, errorMessageResId: Int, liveDataField: MutableLiveDataUnique<String>?) {
            if (!isValidField) {
                isValid = false
                val errorMessage = resourcesWrapper.getString(errorMessageResId)
                validationErrors.add(errorMessage)
                liveDataField?.set(errorMessage)
            }
        }

        addValidationError(
            !participantId.isNullOrEmpty() && participantIdValidator.validate(participantId),
            if (participantId.isNullOrEmpty()) R.string.participant_registration_details_error_no_participant_id
            else R.string.participant_registration_details_error_invalid_participant_id,
            participantIdValidationMessage
        )

        val existingParticipant = findParticipantByParticipantIdUseCase.findByParticipantId(participantId = participantId!!)
        if (existingParticipant != null) {
            if (existingParticipant.participantUuid != participantUuid.value) {
                addValidationError(
                    false,
                    R.string.participant_registration_details_error_participant_already_exists,
                    participantIdValidationMessage
                )
            }
        }

        addValidationError(
            !birthWeight.isNullOrEmpty(),
            R.string.participant_registration_details_error_invalid_birth_weight,
            birthWeightValidationMessage
        )

        if (!birthWeight.isNullOrEmpty()) {
            val weight = birthWeight.toDoubleOrNull()
            if (weight == null || weight < 0.1 || weight > 8.0) {
                addValidationError(
                    false,
                    R.string.participant_registration_details_error_invalid_birth_weight,
                    birthWeightValidationMessage
                )
            }
        }

        addValidationError(gender == Gender.MALE || gender == Gender.FEMALE, R.string.participant_registration_details_error_no_gender, genderValidationMessage)

        addValidationError(birthDate != null, R.string.participant_registration_details_error_birth_date_cannot_be_empty, birthDateValidationMessage)

        addValidationError(birthDate != null, R.string.participant_registration_details_error_birth_date_cannot_be_empty, estimatedAgeValidationMessage)

        addValidationError(homeLocation?.isWholeAddressEmpty() == false, R.string.participant_registration_details_error_no_home_location, homeLocationValidationMessage)

        addValidationError(
            !motherFirstName.isNullOrEmpty() && textInputValidator.validate(motherFirstName),
            if (motherFirstName.isNullOrEmpty()) R.string.participant_registration_details_error_no_mother_first_name
            else R.string.participant_registration_details_error_no_letters_used,
            motherFirstNameValidationMessage
        )

        addValidationError(
            !motherLastName.isNullOrEmpty() && textInputValidator.validate(motherLastName),
            if (motherLastName.isNullOrEmpty()) R.string.participant_registration_details_error_no_mother_last_name
            else R.string.participant_registration_details_error_no_letters_used,
            motherLastNameValidationMessage
        )

        if (!fatherFirstName.isNullOrEmpty()) {
            addValidationError(textInputValidator.validate(fatherFirstName), R.string.participant_registration_details_error_no_letters_used, fatherFirstNameValidationMessage)
        }
        if (!fatherLastName.isNullOrEmpty()) {
            addValidationError(textInputValidator.validate(fatherLastName), R.string.participant_registration_details_error_no_letters_used, fatherLastNameValidationMessage)
        }

        if (!childFirstName.isNullOrEmpty()) {
            addValidationError(textInputValidator.validate(childFirstName), R.string.participant_registration_details_error_no_letters_used, childFirstNameValidationMessage)
        }
        if (!childLastName.isNullOrEmpty()) {
            addValidationError(textInputValidator.validate(childLastName), R.string.participant_registration_details_error_no_letters_used, childLastNameValidationMessage)
        }

        val shouldValidateNin = !(isEditMode() && originalNinValue == nin)
        if (shouldValidateNin && !nin.isNullOrEmpty()) {
            val isFormatValid = ninValidator.validate(nin)
            addValidationError(isFormatValid, R.string.participant_registration_details_error_nin_wrong_format, ninValidationMessage)
            if (isFormatValid) {
                val isNinUnique = !isNinAlreadyExist(nin)
                addValidationError(isNinUnique, R.string.participant_registration_details_error_nin_already_exist, ninValidationMessage)
            }
        }

        this.validationErrors.value = validationErrors

        return isValid
    }

    private fun isNinAlreadyExist(ninValue: String?): Boolean {
        val ninIds = ninIdentifiers.get()?.map { it.identifierValue }

        if (!ninIds.isNullOrEmpty()) {
            return ninIds.any { it.equals(ninValue, ignoreCase = true) }
        }

        return false
    }

    private fun resetValidationMessages() {
        participantIdValidationMessage.set(null)
        ninValidationMessage.set(null)
        genderValidationMessage.set(null)
        birthWeightValidationMessage.set(null)
        bestContactTimeValidationMessage.set(null)
        birthDateValidationMessage.set(null)
        estimatedAgeValidationMessage.set(null)
        phoneValidationMessage.set(null)
        homeLocationValidationMessage.set(null)
        languageValidationMessage.set(null)
        motherFirstNameValidationMessage.set(null)
        motherLastNameValidationMessage.set(null)
        fatherFirstNameValidationMessage.set(null)
        fatherLastNameValidationMessage.set(null)
        childFirstNameValidationMessage.set(null)
        childLastNameValidationMessage.set(null)
        childNumberValidationMessage.set(null)
    }

    fun setGender(gender: Gender) {
        if (this.gender.get() == gender) return
        this.gender.set(gender)
        genderValidationMessage.set(null)
    }

    fun onParticipantIdScanned(participantIdBarcode: String) {
        logInfo("onParticipantIdScanned: $participantIdBarcode")
        isManualSetParticipantID.set(false)
        scannedParticipantId.set(participantIdBarcode)
        setParticipantId(participantIdBarcode)
    }

    fun setParticipantId(participantId: String) {
        if (this.participantId.value == participantId) return
        this.participantId.value = participantId
        validateParticipantId()
        this.participantId.set(participantId)

        //in case we have a scanned id, and we change our id back to original scanned value, the confirm field disappears
        if (participantId == scannedParticipantId.value) {
            isManualSetParticipantID.set(false)
        }
        validateParticipantId()
    }

    fun setNin(nin: String) {
        if (this.nin.get() == nin) return
        this.nin.set(nin)

        val inferredGender = getGenderFromNin(nin)
        inferredGender?.let {
            this.gender.set(it)
        }
    }

    private fun getGenderFromNin(nin: String): Gender? {
        return when {
            nin.startsWith("CM", ignoreCase = true) -> Gender.MALE
            nin.startsWith("CF", ignoreCase = true) -> Gender.FEMALE
            else -> null
        }
    }

    fun setChildNumber(childNumber: String) {
        if (this.childNumber.get() == childNumber) return
        this.childNumber.set(childNumber)
    }

    fun setMotherFirstName(motherFirstName: String?) {
        if (this.motherFirstName.get() == motherFirstName) return
        this.motherFirstName.set(motherFirstName)
    }

    fun setMotherLastName(motherLastName: String?) {
        if (this.motherLastName.get() == motherLastName) return
        this.motherLastName.set(motherLastName)
    }

    fun setFatherFirstName(fatherFirstName: String?) {
        if (this.fatherFirstName.get() == fatherFirstName) return
        this.fatherFirstName.set(fatherFirstName)
    }

    fun setFatherLastName(fatherLastName: String?) {
        if (this.fatherLastName.get() == fatherLastName) return
        this.fatherLastName.set(fatherLastName)
    }

    fun setChildFirstName(childFirstName: String) {
        if (this.childFirstName.get() == childFirstName) return
        this.childFirstName.set(childFirstName)
    }

    fun setChildLastName(childLastName: String) {
        if (this.childLastName.get() == childLastName) return
        this.childLastName.set(childLastName)
    }

    fun setBirthWeight(birthWeight: String) {
        if (this.birthWeight.get() == birthWeight) return
        this.birthWeight.set(birthWeight)
    }

    fun setBestContactTime(bestContactTime: String) {
        if (this.bestContactTime.get() == bestContactTime) return
        this.bestContactTime.set(bestContactTime)
    }

    private fun validateParticipantId() {
        logInfo("validateParticipantId")
        participantIdValidationMessage.set(null)
        validateParticipantIdJob?.cancel()
        validateParticipantIdJob = scope.launch {
            delaySafe(INLINE_VALIDATION_DELAY)
            val validateParticipantId = participantId.value
            if (!validateParticipantId.isNullOrEmpty() && !participantIdValidator.validate(validateParticipantId)) {
                participantIdValidationMessage.value = resourcesWrapper.getString(R.string.participant_registration_details_error_invalid_participant_id)
            }
        }
    }

    private fun setBirthDateOrEstimatedAge(birthDate: DateTime?, isBirthDateEstimated: Boolean) {
        if (isBirthDateEstimated && birthDate != null) {
            val currentDate = DateTime.now()
            val daysDifference = (currentDate - birthDate).days.toInt()

            val years = daysDifference / 365
            val remainingDaysAfterYears = daysDifference % 365
            val months = remainingDaysAfterYears / 30
            val remainingDaysAfterMonths = remainingDaysAfterYears % 30
            val weeks = remainingDaysAfterMonths / 7

            setEstimatedAgeText(years, months, weeks)
            setBirthDateBasedOnEstimatedBirthdate(birthDate)
        } else {
            setBirthDate(birthDate)
        }
    }

    fun setBirthDate(birthDate: DateTime?) {
        this.birthDate.set(birthDate)
        val formattedDate = birthDate?.format(DateFormat.FORMAT_DATE)
        this.birthDateText.set(formattedDate)
        birthDateValidationMessage.set(null)
        isBirthDateEstimated.set(false)
        this.estimatedAgeText.set(null)
    }

    fun setBirthDateBasedOnEstimatedBirthdate(birthDate: DateTime?) {
        this.birthDate.set(birthDate)
        isBirthDateEstimated.set(true)
        this.birthDateText.set(null)
    }

    fun setEstimatedAgeText(yearsEstimated: Int?, monthsEstimated: Int?, weeksEstimated: Int?) {
        val ageString = listOfNotNull(
            yearsEstimated?.let {"${it}y"},
            monthsEstimated.let { "${it}m" },
            weeksEstimated.let { "${it}w" }
        ).joinToString(" ")
        this.estimatedAgeText.set(ageString)
    }

    private fun createFullPhone(): String {
        val phone = phone.value ?: return ""
        val phoneCountryCode = phoneCountryCode.get() ?: return ""
        return fullPhoneFormatter.toFullPhoneNumberOrNull(phone, phoneCountryCode) ?: ""
    }

    private fun validatePhone() {
        logInfo("validatePhone")
        phoneValidationMessage.set(null)
        validatePhoneJob?.cancel()
        validatePhoneJob = scope.launch {
            delaySafe(INLINE_VALIDATION_DELAY)
            val fullPhone = createFullPhone()
            if (fullPhone.isNotEmpty() && !phoneValidator.validate(fullPhone)) {
                phoneValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_phone))
            }
        }
    }

    fun setPhone(phone: String, removeCode: Boolean = false) {
        if (this.phone.get() == phone) return
        var modifiedPhone = phone
        if (removeCode) {
            if (modifiedPhone.length >= 3) {
                // it will only work for Uganda and other 3 digit country codes
                modifiedPhone = modifiedPhone.removeRange(0, 3)
            }
        }
        this.phone.set(modifiedPhone)

        validatePhone()
    }

    fun setPhoneCountryCode(selectedCountryCode: String) {
        if (phoneCountryCode.get() == selectedCountryCode) return // Break feedback loop
        phoneCountryCode.set(selectedCountryCode)
        validatePhone()
    }

    fun setSelectedChildCategory(childCategoryName: DisplayValue) {
        if (this.childCategory.get() == childCategoryName) return
        childCategory.set(childCategoryName)
        childCategoryValidationMessage.set(null)
    }

    fun setSelectedLanguage(languageName: DisplayValue) {
        if (this.language.get() == languageName) return
        language.set(languageName)
        languageValidationMessage.set(null)
    }

    fun setHomeLocation(homeLocation: Address, stringRepresentation: String) {
        this.homeLocation.set(homeLocation)
        this.homeLocationLabel.set(stringRepresentation)
        homeLocationValidationMessage.set(null)
    }

    fun isEditMode(): Boolean {
        return participantUuid.value != null;
    }

    fun generateChildId(): String {
        val identifierLength = 8
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..identifierLength)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
