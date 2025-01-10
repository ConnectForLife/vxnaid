package com.idi.vaccinetracker.common.di

import androidx.lifecycle.ViewModel
import com.idi.vaccinetracker.barcode.ScanBarcodeViewModel
import com.idi.vaccinetracker.common.ui.BaseActivityViewModel
import com.idi.vaccinetracker.common.ui.dialog.SyncErrorViewModel
import com.idi.vaccinetracker.irisscanner.ScannerConnectedViewModel
import com.idi.vaccinetracker.login.LoginViewModel
import com.idi.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.idi.vaccinetracker.participantflow.screens.ParticipantFlowIrisScanViewModel
import com.idi.vaccinetracker.participantflow.screens.ParticipantFlowMatchingViewModel
import com.idi.vaccinetracker.participantflow.screens.ParticipantFlowMotherNameViewModel
import com.idi.vaccinetracker.participantflow.screens.ParticipantFlowParticipantIdViewModel
import com.idi.vaccinetracker.participantflow.screens.ParticipantFlowPhoneNumberViewModel
import com.idi.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.idi.vaccinetracker.register.dialogs.HomeLocationPickerViewModel
import com.idi.vaccinetracker.register.screens.HistoricalDataForVisitTypeViewModel
import com.idi.vaccinetracker.register.screens.RegisterParticipantHistoricalDataViewModel
import com.idi.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsViewModel
import com.idi.vaccinetracker.settings.SettingsViewModel
import com.idi.vaccinetracker.setup.SetupFlowViewModel
import com.idi.vaccinetracker.setup.screens.SetupBackendConfigViewModel
import com.idi.vaccinetracker.setup.screens.SetupPermissionsViewModel
import com.idi.vaccinetracker.setup.screens.SetupSyncConfigViewModel
import com.idi.vaccinetracker.setup.screens.licenses.SetupLicensesViewModel
import com.idi.vaccinetracker.setup.screens.mainmenu.SetupMainMenuViewModel
import com.idi.vaccinetracker.setup.screens.p2p.device_role.SetupP2pDeviceRoleViewModel
import com.idi.vaccinetracker.setup.screens.p2p.transfer.client.SetupP2pDeviceClientTransferViewModel
import com.idi.vaccinetracker.setup.screens.p2p.transfer.server.SetupP2pDeviceServerTransferViewModel
import com.idi.vaccinetracker.splash.SplashViewModel
import com.idi.vaccinetracker.update.UpdateViewModel
import com.idi.vaccinetracker.vaccinesoverview.model.VaccinesOverviewViewModel
import com.idi.vaccinetracker.visit.VisitViewModel
import com.idi.vaccinetracker.visitsoverview.model.VisitsListViewModel
import com.idi.vaccinetracker.visitsoverview.model.VisitsOverviewViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
@SuppressWarnings("TooManyFunctions")
interface ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    fun bindSplashViewModel(splashViewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupFlowViewModel::class)
    fun bindSetupViewModel(setupFlowViewModel: SetupFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupSyncConfigViewModel::class)
    fun bindSetupSyncConfigViewModel(setupSyncConfigViewModel: SetupSyncConfigViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupBackendConfigViewModel::class)
    fun bindSetupBackendConfigViewModel(setupBackendConfigViewModel: SetupBackendConfigViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupPermissionsViewModel::class)
    fun bindSetupPermissionsViewModel(setupPermissionsViewModel: SetupPermissionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScannerConnectedViewModel::class)
    fun bindScannerConnectedViewModel(scannerConnectedViewModel: ScannerConnectedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowViewModel::class)
    fun bindParticipantFlowViewModel(participantFlowViewModel: ParticipantFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BaseActivityViewModel::class)
    fun bindBaseActivityViewModel(baseActivityViewModel: BaseActivityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    fun bindLoginViewModel(loginViewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    fun bindSettingsViewModel(model: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UpdateViewModel::class)
    fun bindUpdateViewModel(model: UpdateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowParticipantIdViewModel::class)
    fun bindParticipantFlowParticipantIdViewModel(participantFlowParticipantIdViewModel: ParticipantFlowParticipantIdViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowMotherNameViewModel::class)
    fun bindParticipantFlowMotherNameViewModel(participantFlowMotherNameViewModel: ParticipantFlowMotherNameViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowPhoneNumberViewModel::class)
    fun bindParticipantFlowPhoneNumberViewModel(participantFlowPhoneNumberViewModel: ParticipantFlowPhoneNumberViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowIrisScanViewModel::class)
    fun bindParticipantFlowIrisScanViewModel(participantFlowIrisScanViewModel: ParticipantFlowIrisScanViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScanBarcodeViewModel::class)
    fun bindScanBarcodeViewModel(scanBarcodeViewModel: ScanBarcodeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowMatchingViewModel::class)
    fun bindParticipantFlowMatchingViewModel(model: ParticipantFlowMatchingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterParticipantFlowViewModel::class)
    fun bindRegisterParticipantFlowViewModel(model: RegisterParticipantFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterParticipantHistoricalDataViewModel::class)
    fun bindRegisterParticipantHistoricalDataViewModel(model: RegisterParticipantHistoricalDataViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterParticipantParticipantDetailsViewModel::class)
    fun bindRegisterParticipantParticipantDetailsViewModel(model: RegisterParticipantParticipantDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeLocationPickerViewModel::class)
    fun bindHomeLocationPickerViewModel(model: HomeLocationPickerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VisitViewModel::class)
    fun bindVisitViewModel(model: VisitViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SyncErrorViewModel::class)
    fun bindSyncErrorViewModel(model: SyncErrorViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupMainMenuViewModel::class)
    fun bindSetupMainMenuViewModel(model: SetupMainMenuViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(SetupP2pDeviceRoleViewModel::class)
    fun bindSetupP2pDeviceRoleViewModel(model: SetupP2pDeviceRoleViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupP2pDeviceClientTransferViewModel::class)
    fun bindSetupP2pDeviceClientTransferViewModel(model: SetupP2pDeviceClientTransferViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupP2pDeviceServerTransferViewModel::class)
    fun bindSetupP2pDeviceServerTransferViewModel(model: SetupP2pDeviceServerTransferViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupLicensesViewModel::class)
    fun bindSetupLicensesViewModel(model: SetupLicensesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HistoricalDataForVisitTypeViewModel::class)
    fun bindRegisterParticipantAdministeredVaccinesViewModel(model: HistoricalDataForVisitTypeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VisitsOverviewViewModel::class)
    fun bindVisitsOverviewViewModel(model: VisitsOverviewViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VisitsListViewModel::class)
    fun bindVisitsListViewModel(model: VisitsListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VaccinesOverviewViewModel::class)
    fun bindVaccinesOverviewViewModel(model: VaccinesOverviewViewModel): ViewModel
}