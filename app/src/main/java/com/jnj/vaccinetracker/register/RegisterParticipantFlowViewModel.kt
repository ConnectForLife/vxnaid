package com.jnj.vaccinetracker.register

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.domain.entities.ImageBytes
import com.jnj.vaccinetracker.common.domain.entities.RegisterParticipant
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.soywiz.klock.DateTime
import javax.inject.Inject

/**
 * Responsible for managing the flow of registering a new participant.
 */
class RegisterParticipantFlowViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val participantManager: ParticipantManager
) : ViewModelBase() {

    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    val participantPicture = mutableLiveData<ParticipantImageUiModel>()
    val participantId = mutableLiveData<String>()
    val participantUuid = mutableLiveData<String>()
    val participant = mutableLiveData<ParticipantSummaryUiModel>()
    val registerParticipant = mutableLiveData<RegisterParticipant>()
    val registerDetails = mutableLiveData<ParticipantManager.RegisterDetails>().apply {
        value = ParticipantManager.RegisterDetails(
            participantId = "",
            nin = "",
            childNumber = "",
            birthWeight = "",
            gender = Gender.MALE,
            birthDate = DateTime.now(),
            isBirthDateEstimated = false,
            telephone = "",
            siteUuid = "",
            language = "",
            address = Address("", "", "", "", "", "", ""),
            picture = null,
            biometricsTemplateBytes = null,
            motherFirstName = "",
            motherLastName = "",
            fatherFirstName = "",
            fatherLastName = "",
            childFirstName = "",
            childLastName = "",
            childCategory = ""
        )
    }
    val visitTypeName = mutableLiveData<String>()
    val leftEyeScanned = mutableLiveBoolean()
    val rightEyeScanned = mutableLiveBoolean()
    val isManualEnteredId = mutableLiveBoolean()
    val countryCode = mutableLiveData<String>()
    val phoneNumber = mutableLiveData<String>()
    val requestFinish = mutableLiveData<Boolean>()

    suspend fun setArguments(
        participantId: String?,
        leftEyeScanned: Boolean,
        rightEyeScanned: Boolean,
        countryCode: String?,
        phoneNumber: String?,
        isManualEnteredId: Boolean,
        participantUuid: String?,
    ) {
        if (currentScreen.get() == null) {
            currentScreen.set(Screen.PARTICIPANT_DETAILS)
        }
        this.participantId.set(participantId)
        this.leftEyeScanned.set(leftEyeScanned)
        this.rightEyeScanned.set(rightEyeScanned)
        this.countryCode.set(countryCode)
        this.phoneNumber.set(phoneNumber)
        this.isManualEnteredId.set(isManualEnteredId)
        this.participantUuid.set(participantUuid)
        if (participantUuid != null) {
            val participantPicture = loadParticipantPicture(participantUuid)?.toUiModel()
            if (participantPicture != null && !isImageEmpty(participantPicture.byteArray)) {
                this.participantPicture.set(participantPicture)
            } else {
                this.participantPicture.set(null)
            }
            currentScreen.set(Screen.PARTICIPANT_DETAILS)
        }
    }

    fun isImageEmpty(imageBytes: ByteArray): Boolean {
        val bitmap: Bitmap? = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        return bitmap == null
    }

    private suspend fun loadParticipantPicture(participantUUID: String?): ImageBytes? {
        if (participantUUID == null) return null
        return try {
            participantManager.getPersonImage(participantUUID)
        } catch (ex: Throwable) {
            ex.rethrowIfFatal()
            logDebug("No picture for participant $participantUUID")
            null
        }
    }

    fun navigateBack(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val screens = Screen.values()
        val previousScreenIndex = screens.indexOf(currentScreen) - 1

        if (previousScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.BACKWARD
            this.currentScreen.set(screens[previousScreenIndex])
            return true
        }

        return false
    }

    private fun navigateForward(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val screens = Screen.values()
        val nextScreenIndex = screens.indexOf(currentScreen) + 1

        if (nextScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.FORWARD
            this.currentScreen.set(screens[nextScreenIndex])
            return true
        }

        return false
    }

    fun confirmCameraPermissionGranted() {
        navigateForward()
    }

    fun savePictureAndContinue(imgBytes: ParticipantImageUiModel) {
        participantPicture.set(imgBytes)
        navigateForward()
    }

    fun retakePicture() {
        participantPicture.set(null)
        navigateBack()
    }

    fun skipPicture() {
        participantPicture.set(null)
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.PARTICIPANT_DETAILS)
    }

    fun confirmPicture() {
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.PARTICIPANT_DETAILS)
    }

    fun onChildPictureClicked() {
        if (participantPicture.get() != null) {
            currentScreen.set(Screen.CONFIRM_PICTURE)
        } else {
            currentScreen.set(Screen.TAKE_PICTURE)
        }
    }

    fun backToRegistrationPage() {
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.PARTICIPANT_DETAILS)
    }

    fun confirmRegistrationWithCaptureVaccinesPage(registerParticipant: RegisterParticipant) {
       this.registerParticipant.set(registerParticipant)
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.PARTICIPANT_CAPTURE_HISTORICAL_DATA)
    }
    fun openHistoricalDataForVisitType(visitTypeName: String) {
        this.visitTypeName.set(visitTypeName)
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.VISIT_TYPE_HISTORICAL_DATA)
        this.visitTypeName.set(null)
    }

    enum class Screen(@StringRes val title: Int) {
        CAMERA_PERMISSION(R.string.participant_registration_picture_title),
        TAKE_PICTURE(R.string.participant_registration_picture_title),
        CONFIRM_PICTURE(R.string.participant_registration_picture_title),
        PARTICIPANT_DETAILS(R.string.participant_registration_details_title),
        PARTICIPANT_CAPTURE_HISTORICAL_DATA(R.string.participant_registration_historical_data_title),
        VISIT_TYPE_HISTORICAL_DATA(R.string.participant_registration_historical_data_title)
    }
}
