package com.idi.vaccinetracker.common.di

import com.idi.vaccinetracker.barcode.ScanBarcodeActivity
import com.idi.vaccinetracker.common.ui.dialog.SuccessDialog
import com.idi.vaccinetracker.common.ui.dialog.SyncErrorDialog
import com.idi.vaccinetracker.irisscanner.ScannerConnectedActivity
import com.idi.vaccinetracker.login.LoginActivity
import com.idi.vaccinetracker.login.RefreshSessionDialog
import com.idi.vaccinetracker.participantflow.ParticipantFlowActivity
import com.idi.vaccinetracker.participantflow.dialogs.ParticipantFlowCancelWorkflowDialog
import com.idi.vaccinetracker.participantflow.dialogs.ParticipantFlowMandatoryIrisDialog
import com.idi.vaccinetracker.participantflow.dialogs.ParticipantFlowMissingIdentifiersDialog
import com.idi.vaccinetracker.participantflow.dialogs.ParticipantFlowNoTelephoneDialog
import com.idi.vaccinetracker.participantflow.screens.*
import com.idi.vaccinetracker.register.RegisterParticipantFlowActivity
import com.idi.vaccinetracker.register.dialogs.HomeLocationPickerDialog
import com.idi.vaccinetracker.register.dialogs.RegisterParticipantConfirmNoTelephoneDialog
import com.idi.vaccinetracker.register.dialogs.RegisterParticipantHasChildEverVaccinatedDialog
import com.idi.vaccinetracker.register.dialogs.RegisterParticipantIdNotMatchingDialog
import com.idi.vaccinetracker.register.dialogs.RegisterParticipantSuccessfulDialog
import com.idi.vaccinetracker.register.dialogs.VaccineDialog
import com.idi.vaccinetracker.register.screens.HistoricalDataForVisitTypeFragment
import com.idi.vaccinetracker.register.screens.RegisterParticipantCameraPermissionFragment
import com.idi.vaccinetracker.register.screens.RegisterParticipantHistoricalDataFragment
import com.idi.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsFragment
import com.idi.vaccinetracker.register.screens.RegisterParticipantPicturePreviewFragment
import com.idi.vaccinetracker.register.screens.RegisterParticipantTakePictureFragment
import com.idi.vaccinetracker.settings.SettingsDialog
import com.idi.vaccinetracker.setup.SetupFlowActivity
import com.idi.vaccinetracker.setup.dialogs.SetupCancelWizardDialog
import com.idi.vaccinetracker.setup.screens.SetupBackendConfigFragment
import com.idi.vaccinetracker.setup.screens.SetupIntroFragment
import com.idi.vaccinetracker.setup.screens.SetupPermissionsFragment
import com.idi.vaccinetracker.setup.screens.SetupSyncConfigFragment
import com.idi.vaccinetracker.setup.screens.licenses.SetupLicensesFragment
import com.idi.vaccinetracker.setup.screens.mainmenu.SetupMainMenuFragment
import com.idi.vaccinetracker.setup.screens.p2p.device_role.SetupP2pDeviceRoleFragment
import com.idi.vaccinetracker.setup.screens.p2p.dialogs.ConfirmStopServiceDialog
import com.idi.vaccinetracker.setup.screens.p2p.transfer.client.SetupP2pDeviceClientTransferFragment
import com.idi.vaccinetracker.setup.screens.p2p.transfer.server.SetupP2pDeviceServerTransferFragment
import com.idi.vaccinetracker.splash.SplashActivity
import com.idi.vaccinetracker.sync.presentation.SyncAndroidService
import com.idi.vaccinetracker.update.UpdateDialog
import com.idi.vaccinetracker.visit.VisitActivity
import com.idi.vaccinetracker.common.dialogs.DatePickerDialog
import com.idi.vaccinetracker.common.ui.dialog.ValidationErrorDialog
import com.idi.vaccinetracker.register.dialogs.HistoricalVisitDateDialog
import com.idi.vaccinetracker.participantflow.dialogs.AdverseEffectsSuccessfulDialog
import com.idi.vaccinetracker.register.dialogs.AlreadyAdministeredVaccineDatePickerDialog
import com.idi.vaccinetracker.register.dialogs.MultipleVisitsDialog
import com.idi.vaccinetracker.register.dialogs.TransferClinicDialog
import com.idi.vaccinetracker.register.dialogs.UpdateParticipantSuccessfulDialog
import com.idi.vaccinetracker.vaccinesoverview.VaccinesOverviewFlowActivity
import com.idi.vaccinetracker.vaccinesoverview.screens.VaccinesOverviewFragment
import com.idi.vaccinetracker.visit.dialog.DialogScheduleMissingSubstances
import com.idi.vaccinetracker.visit.dialog.DialogVaccineBarcode
import com.idi.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.idi.vaccinetracker.visit.dialog.RescheduleVisitDialog
import com.idi.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import com.idi.vaccinetracker.visit.screens.ContraindicationsFragment
import com.idi.vaccinetracker.visit.screens.ReferralFragment
import com.idi.vaccinetracker.visit.screens.VisitVaccinesFragment
import com.idi.vaccinetracker.visit.screens.VisitCaptureDataFragment
import com.idi.vaccinetracker.visitsoverview.VisitsOverviewFlowActivity
import com.idi.vaccinetracker.visitsoverview.dialog.VisitDetailsDialog
import com.idi.vaccinetracker.visitsoverview.screens.VisitsListFragment
import com.idi.vaccinetracker.visitsoverview.screens.VisitsOverviewFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author maartenvangiel
 * @version 1
 */
@Module
@SuppressWarnings("TooManyFunctions")
interface AndroidModule {

    @ContributesAndroidInjector
    fun bindSplashActivity(): SplashActivity

    @ContributesAndroidInjector
    fun bindSetupActivity(): SetupFlowActivity

    @ContributesAndroidInjector
    fun bindSetupCancelWizardDialog(): SetupCancelWizardDialog

    @ContributesAndroidInjector
    fun bindSetupIntroFragment(): SetupIntroFragment

    @ContributesAndroidInjector
    fun bindSetupBackendConfigFragment(): SetupBackendConfigFragment

    @ContributesAndroidInjector
    fun bindSetupSyncConfigFragment(): SetupSyncConfigFragment

    @ContributesAndroidInjector
    fun bindSetupPermissionsFragment(): SetupPermissionsFragment

    @ContributesAndroidInjector
    fun bindScannerConnectedActivity(): ScannerConnectedActivity

    @ContributesAndroidInjector
    fun bindParticipantFlowActivity(): ParticipantFlowActivity

    @ContributesAndroidInjector
    fun bindParticipantFlowIntroFragment(): ParticipantFlowIntroFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowAddOrSearchFragment(): ParticipantFlowAddOrSearchFragment

    @ContributesAndroidInjector
    fun bindAdverseEffectsFragment(): AdverseEffectsFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantAdministeredVaccinesFragment(): HistoricalDataForVisitTypeFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantHistoricalDataFragment(): RegisterParticipantHistoricalDataFragment

    @ContributesAndroidInjector
    fun bindLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    fun bindSettingsDialog(): SettingsDialog

    @ContributesAndroidInjector
    fun bindUpdateDialog(): UpdateDialog

    @ContributesAndroidInjector
    fun bindAdverseEffectsSuccessfulDialog(): AdverseEffectsSuccessfulDialog

    @ContributesAndroidInjector
    fun bindRefreshSessionDialog(): RefreshSessionDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowParticipantIdFragment(): ParticipantFlowParticipantIdFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowPhoneNumberFragment(): ParticipantFlowPhoneNumberFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowMotherNameFragment(): ParticipantFlowMotherNameFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowNoTelephoneDialog(): ParticipantFlowNoTelephoneDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowIrisScanFragment(): ParticipantFlowIrisScanFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowIrisScanLeftFragment(): ParticipantFlowIrisScanLeftFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowIrisScanRightFragment(): ParticipantFlowIrisScanRightFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowMatchingFragment(): ParticipantFlowMatchingFragment

    @ContributesAndroidInjector
    fun bindScanBarcodeActivity(): ScanBarcodeActivity

    @ContributesAndroidInjector
    fun bindRegisterParticipantActivity(): RegisterParticipantFlowActivity

    @ContributesAndroidInjector
    fun bindRegisterParticipantCameraPermissionFragment(): RegisterParticipantCameraPermissionFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantTakePictureFragment(): RegisterParticipantTakePictureFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantPicturePreviewFragment(): RegisterParticipantPicturePreviewFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantFlowParticipantDetailsFragment(): RegisterParticipantParticipantDetailsFragment

    @ContributesAndroidInjector
    fun bindHomeLocationPickerDialog(): HomeLocationPickerDialog

    @ContributesAndroidInjector
    fun bindDialogVaccineBarcode(): DialogVaccineBarcode

    @ContributesAndroidInjector
    fun bindRegisterParticipantSuccessfulDialog(): RegisterParticipantSuccessfulDialog

    @ContributesAndroidInjector
    fun bindMultipleVisitsDialog(): MultipleVisitsDialog

    @ContributesAndroidInjector
    fun bindUpdateParticipantSuccessfulDialog(): UpdateParticipantSuccessfulDialog

    @ContributesAndroidInjector
    fun bindVaccineDialog(): VaccineDialog

    @ContributesAndroidInjector
    fun bindAlreadyAdministeredVaccineDatePickerDialog(): AlreadyAdministeredVaccineDatePickerDialog

    @ContributesAndroidInjector
    fun bindHistoricalVisitDateDialog(): HistoricalVisitDateDialog

    @ContributesAndroidInjector
    fun bindRegisterParticipantConfirmNoTelephoneDialog(): RegisterParticipantConfirmNoTelephoneDialog

    @ContributesAndroidInjector
    fun bindRegisterParticipantIdNotMatchingDialog(): RegisterParticipantIdNotMatchingDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowCancelWorkflowDialog(): ParticipantFlowCancelWorkflowDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowMissingIdentifiersDialog(): ParticipantFlowMissingIdentifiersDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowMandatoryIrisDialog(): ParticipantFlowMandatoryIrisDialog

    @ContributesAndroidInjector
    fun bindVisitActivity(): VisitActivity

    @ContributesAndroidInjector
    fun bindContraindicationsFragment(): ContraindicationsFragment

    @ContributesAndroidInjector
    fun bindVisitVaccinesFragment(): VisitVaccinesFragment

    @ContributesAndroidInjector
    fun bindReferralFragment(): ReferralFragment

    @ContributesAndroidInjector
    fun bindVisitCaptureDataFragment(): VisitCaptureDataFragment

    @ContributesAndroidInjector
    fun bindSuccessDialog(): SuccessDialog

    @ContributesAndroidInjector
    fun bindDosingOutOfWindowDialog(): DosingOutOfWindowDialog

    @ContributesAndroidInjector
    fun bindDialogScheduleMissingSubstances(): DialogScheduleMissingSubstances

    @ContributesAndroidInjector
    fun bindValidationErrorDialog(): ValidationErrorDialog

    @ContributesAndroidInjector
    fun bindDatePickerDialog(): DatePickerDialog

    @ContributesAndroidInjector
    fun bindVisitRegisteredSuccessDialog(): VisitRegisteredSuccessDialog

    @ContributesAndroidInjector
    fun bindRescheduleVisitDialog(): RescheduleVisitDialog

    @ContributesAndroidInjector
    fun bindVaccineTrackerSyncAndroidService(): SyncAndroidService

    @ContributesAndroidInjector
    fun bindSyncErrorDialog(): SyncErrorDialog

    @ContributesAndroidInjector
    fun bindSetupMainMenuFragment(): SetupMainMenuFragment

    @ContributesAndroidInjector
    fun bindSetupP2pDeviceClientTransferFragment(): SetupP2pDeviceClientTransferFragment

    @ContributesAndroidInjector
    fun bindSetupP2pDeviceServerTransferFragment(): SetupP2pDeviceServerTransferFragment

    @ContributesAndroidInjector
    fun bindSetupLicensesFragment(): SetupLicensesFragment

    @ContributesAndroidInjector
    fun bindSetupP2pDeviceRoleFragment(): SetupP2pDeviceRoleFragment

    @ContributesAndroidInjector
    fun bindConfirmBackPressDialog(): ConfirmStopServiceDialog

    @ContributesAndroidInjector
    fun bindRegisterParticipantIsChildNewbornDialog(): RegisterParticipantHasChildEverVaccinatedDialog

    @ContributesAndroidInjector
    fun bindTransferClinicDialog(): TransferClinicDialog

    @ContributesAndroidInjector
    fun bindVisitsOverviewFragment(): VisitsOverviewFragment

    @ContributesAndroidInjector
    fun bindVisitsListFragment(): VisitsListFragment

    @ContributesAndroidInjector
    fun bindVisitsOverviewFlowActivity(): VisitsOverviewFlowActivity

    @ContributesAndroidInjector
    fun bindVisitDetailsDialog(): VisitDetailsDialog

    @ContributesAndroidInjector
    fun bindVaccinesOverviewFragment(): VaccinesOverviewFragment

    @ContributesAndroidInjector
    fun bindVaccinesOverviewFlowActivity(): VaccinesOverviewFlowActivity
}
