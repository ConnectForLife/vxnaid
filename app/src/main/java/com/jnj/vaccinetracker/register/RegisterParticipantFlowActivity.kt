package com.jnj.vaccinetracker.register

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivityRegisterParticipantFlowBinding
import com.jnj.vaccinetracker.register.screens.HistoricalDataForVisitTypeFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantCameraPermissionFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantHistoricalDataFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantPicturePreviewFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantTakePictureFragment
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog
import kotlinx.coroutines.launch

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class RegisterParticipantFlowActivity : BaseActivity(),
    RescheduleVisitDialog.RescheduleVisitListener
{

    companion object {
        private const val EXTRA_IRIS_LEFT = "irisScannedLeft"
        private const val EXTRA_IRIS_RIGHT = "irisScannedRight"
        private const val EXTRA_MANUAL_ID = "isManualEnteredParticipantId"
        const val EXTRA_PARTICIPANT_ID = "participantId"
        const val EXTRA_PARTICIPANT = "participant"
        const val EXTRA_PARTICIPANT_UUID = "participantBase"
        const val EXTRA_COUNTRY_CODE = "phoneCountryCode"
        const val EXTRA_PHONE_NUMBER = "participantPhoneNumber"


        fun create(
            context: Context,
            participantId: String?,
            isManualEnteredParticipantId: Boolean?,
            irisScannedLeft: Boolean,
            irisScannedRight: Boolean,
            countryCode: String?,
            phoneNumber: String?,
            participantUuid: String? = null,
        ): Intent {
            return Intent(context, RegisterParticipantFlowActivity::class.java)
                .putExtra(EXTRA_PARTICIPANT_ID, participantId)
                .putExtra(EXTRA_IRIS_LEFT, irisScannedLeft)
                .putExtra(EXTRA_IRIS_RIGHT, irisScannedRight)
                .putExtra(EXTRA_COUNTRY_CODE, countryCode)
                .putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                .putExtra(EXTRA_MANUAL_ID, isManualEnteredParticipantId)
                .putExtra(EXTRA_PARTICIPANT_UUID, participantUuid)
        }
    }

    private val viewModel: RegisterParticipantFlowViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityRegisterParticipantFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            viewModel.setArguments(
                intent.getStringExtra(EXTRA_PARTICIPANT_ID),
                leftEyeScanned = intent.getBooleanExtra(EXTRA_IRIS_LEFT, false),
                rightEyeScanned = intent.getBooleanExtra(EXTRA_IRIS_RIGHT, false),
                countryCode = intent.getStringExtra(EXTRA_COUNTRY_CODE),
                phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER),
                isManualEnteredId = intent.getBooleanExtra(EXTRA_MANUAL_ID, false),
                participantUuid = intent.getStringExtra(EXTRA_PARTICIPANT_UUID)
            )
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register_participant_flow)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel.currentScreen.observe(this) { screen ->
            navigateToScreen(screen, viewModel.navigationDirection)
        }
        viewModel.requestFinish.observe(this) { shouldClose ->
            if (shouldClose == true) finishIfEdit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        finishIfEdit()

        val hasCameraPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        // Edge case for going back from the take picture screen: if the user already has camera permission, no need to go back to that screen
        if (viewModel.currentScreen.value == RegisterParticipantFlowViewModel.Screen.TAKE_PICTURE && hasCameraPermission) {
            return super.onBackPressed()
        }

        if (!viewModel.navigateBack()) {
            super.onBackPressed()
        }
    }

    private fun finishIfEdit() {
        if (viewModel.participantUuid.value != null) {
            finish()
        }
    }

    private fun navigateToScreen(screen: RegisterParticipantFlowViewModel.Screen?, navigationDirection: NavigationDirection) {
        val fragment = when (screen) {
            RegisterParticipantFlowViewModel.Screen.CAMERA_PERMISSION -> RegisterParticipantCameraPermissionFragment()
            RegisterParticipantFlowViewModel.Screen.TAKE_PICTURE -> RegisterParticipantTakePictureFragment()
            RegisterParticipantFlowViewModel.Screen.CONFIRM_PICTURE -> RegisterParticipantPicturePreviewFragment()
            RegisterParticipantFlowViewModel.Screen.PARTICIPANT_DETAILS -> RegisterParticipantParticipantDetailsFragment()
            RegisterParticipantFlowViewModel.Screen.PARTICIPANT_CAPTURE_HISTORICAL_DATA -> RegisterParticipantHistoricalDataFragment()
            RegisterParticipantFlowViewModel.Screen.VISIT_TYPE_HISTORICAL_DATA -> HistoricalDataForVisitTypeFragment.create(viewModel.visitTypeName.value)
            else -> null
        }
        screen?.let { title = getString(it.title) }

        fragment?.let { newFragment ->
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { existingFragment ->
                if (newFragment::class == existingFragment::class) return logWarn("Fragment of this type is already shown, not navigating")
            }

            val transaction = supportFragmentManager.beginTransaction()

            transaction.animateNavigationDirection(navigationDirection)

            transaction
                .replace(R.id.fragment_container, newFragment)
                .commit()
        }
    }


    override val syncBanner: SyncBanner
        get() = binding.syncBanner

    override fun onRescheduleVisitListener() {
        finish()
    }

}