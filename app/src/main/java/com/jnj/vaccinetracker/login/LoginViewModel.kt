package com.jnj.vaccinetracker.login

import com.jnj.vaccinetracker.BuildConfig
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.managers.LoginManager
import com.jnj.vaccinetracker.common.data.managers.UpdateManager
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Site
import com.jnj.vaccinetracker.common.exceptions.OperatorAuthenticationException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.isManualFlavor
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject


/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
class LoginViewModel @Inject constructor(
    private val loginManager: LoginManager,
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val licenseManager: LicenseManager,
    private val updateManager: UpdateManager,
    private val configurationManager: ConfigurationManager,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

    val loading = mutableLiveBoolean()
    val usernameValidationMessage = mutableLiveData<String>()
    val passwordValidationMessage = mutableLiveData<String>()
    val visitPlaceValidationMessage = mutableLiveData<String>()
    val attachedClinicValidationMessage = mutableLiveData<String>()
    val errorMessage = mutableLiveData<String>()
    val prefillUsername = mutableLiveData<String>()
    val versionNumber = mutableLiveData<String>()
    val deviceName = mutableLiveData<String>()
    val latestVersion = mutableLiveBoolean(true)
    val attachedClinics = mutableLiveData<List<Site>>(emptyList())
    private val prefillBackendUrl = mutableLiveData<String>()
    private var allSites = emptyList<Site>()

    val loginCompleted = eventFlow<Unit>()

    fun init(isLoginActivity: Boolean) {
        initState()
        if (isLoginActivity) {
            checkVersion()
            getDeviceName()
        }
    }

    private fun initState() {
        userRepository.observeLastUsername()
            .filterNotNull()
            .onEach { username ->
                prefillUsername.set(username)
            }.launchIn(scope)
        syncSettingsRepository.observeBackendUrl()
            .onEach { prefillBackendUrl.set(it) }
            .launchIn(scope)
        // Load sites from configuration and filter attached clinics
        scope.launch {
            loadSitesFromConfiguration()
            // After loading sites, populate attached clinics based on the previously selected site
            val selectedSiteUuid = syncSettingsRepository.getSiteUuid()
            if (selectedSiteUuid != null) {
                val selectedSite = allSites.find { it.uuid == selectedSiteUuid }
                if (selectedSite != null) {
                    filterAttachedClinicsByParent(selectedSite.name)
                }
            }
        }
    }

    private suspend fun loadSitesFromConfiguration() {
        try {
            allSites = configurationManager.getSites()
            logInfo("📍 Loaded ${allSites.size} sites from configuration")
            allSites.forEach { site ->
                logInfo("📍 Site: name=${site.name}, uuid=${site.uuid}, locationId=${site.locationId}, parentLocationId=${site.parentLocationId}, parentLocationUuid=${site.parentLocationUuid}")
            }
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logError("Failed to load sites from configuration: ", ex)
        }
    }

    /**
     * Filter and update attached clinics based on the selected parent location.
     * Shows all child clinics that have the selected site as their parent.
     * Example: If site "CFL Clinic" (location_id=1) is selected, 
     * this returns all sites with parent_location=1 (A, A1, A2, A3)
     * @param selectedSiteName The name of the selected parent location
     */
    fun filterAttachedClinicsByParent(selectedSiteName: String?) {
        logInfo("🔍 filterAttachedClinicsByParent called with: $selectedSiteName")
        if (selectedSiteName.isNullOrEmpty()) {
            logInfo("🔍 selectedSiteName is empty, returning no clinics")
            attachedClinics.value = emptyList()
            return
        }
        
        // Find the selected site to get its location ID
        val selectedSite = allSites.find { it.name == selectedSiteName }
        if (selectedSite == null) {
            logError("🔍 Selected site not found: $selectedSiteName")
            logInfo("🔍 Available sites: ${allSites.map { it.name }}")
            attachedClinics.value = emptyList()
            return
        }
        
        logInfo("🔍 Selected site found: uuid=${selectedSite.uuid}, locationId=${selectedSite.locationId}, parentLocationId=${selectedSite.parentLocationId}")
        
        // Determine which location ID to use as parent
        val parentLocationIdToMatch = selectedSite.locationId
        
        if (parentLocationIdToMatch == null) {
            logInfo("🔍 parentLocationIdToMatch is null, checking parentsituation")
            // Fallback: if locationId is not available, find siblings by checking if they have same parent
            if (selectedSite.parentLocationId != null) {
                // This site is a child, find siblings
                logInfo("🔍 Selected site is a child (has parentLocationId=${selectedSite.parentLocationId}), finding siblings")
                val siblingClinics = allSites.filter { site ->
                    site.parentLocationId == selectedSite.parentLocationId && site.name != selectedSiteName
                }
                logInfo("🔍 Found ${siblingClinics.size} sibling clinics: ${siblingClinics.map { it.name }}")
                attachedClinics.value = siblingClinics
            } else {
                // This site is a parent (no parent itself), no children will be filtered
                logInfo("🔍 Selected site is a parent (no parent itself), no attached clinics")
                attachedClinics.value = emptyList()
            }
        } else {
            // Find all sites that have this location as their parent
            logInfo("🔍 Looking for sites with parentLocationId=$parentLocationIdToMatch")
            val filteredClinics = allSites.filter { site ->
                site.parentLocationId == parentLocationIdToMatch
            }
            logInfo("🔍 Found ${filteredClinics.size} child clinics: ${filteredClinics.map { it.name }}")
            attachedClinics.value = filteredClinics
        }
    }

    private suspend fun doLogin(
        username: String,
        password: String,
    ) {
        try {
            loading.set(true)
            val user = loginManager.login(username, password)
            userRepository.saveUser(user, dateNow())
            loading.set(false)
            loginCompleted.tryEmit(Unit)
        } catch (ex: OperatorAuthenticationException) {
            val stringResource = when (ex.reason) {
                OperatorAuthenticationException.Reason.LocalCredentialsNotFound -> R.string.login_label_error_offline
                OperatorAuthenticationException.Reason.LocalCredentialsPasswordMismatch,
                OperatorAuthenticationException.Reason.RemoteLoginError,
                -> R.string.login_label_error_not_authenticated
                OperatorAuthenticationException.Reason.SyncAdminRole -> R.string.login_label_error_sync_admin_role_not_allowed
                OperatorAuthenticationException.Reason.NotOperatorRole -> R.string.login_label_error_opertor_role_required
                OperatorAuthenticationException.Reason.NotAssignedLocation -> R.string.login_label_error_location_not_assigned
            }
            errorMessage.set(resourcesWrapper.getString(stringResource))
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logError("Something went wrong while logging in: ", ex)
            errorMessage.set(resourcesWrapper.getString(R.string.login_label_error))
            loading.set(false)
        }
    }

    /**
     * Log in using username and password. backendUrl will be updated if its value is not null (useful for refreshing a login without replacing the backend url)
     */
    fun login(
        username: String,
        password: String,
        visitPlace: String,
        attachedClinic: String = "",
    ) {
        if (!validateInput(username, password, visitPlace, attachedClinic)) return
        scope.launch {
            doLogin(username, password)
        }
    }

    /**
     * Check for the latest version of the application
     * if this app is being manually installed/updated (manual flavor)
     */
    private fun checkVersion() {
        versionNumber.set(BuildConfig.VERSION_NAME)
        if (isManualFlavor) {
            scope.launch {
                try {
                    // On the login screen we always want to call the API to check for update,
                    // so we clear any cache present first
                    updateManager.clearLatestVersionCache()
                    latestVersion.set(updateManager.isLatestVersion())
                } catch (ex: Throwable) {
                    yield()
                    ex.rethrowIfFatal()
                    logError("Something went wrong retrieving the latest version: ", ex)
                }

            }
        }
    }

    /**
     * get the device name from the user repository
     */
    private fun getDeviceName() {
        println("get device name: ${userRepository.getDeviceName()}")
        deviceName.set(userRepository.getDeviceName())
    }

    fun logout() {
        userRepository.logOut()
    }

    private fun validateInput(
        username: String,
        password: String,
        visitPlace: String,
        attachedClinic: String = "",
    ): Boolean {
        var validated = true
        usernameValidationMessage.set(null)
        passwordValidationMessage.set(null)

        if (username.isEmpty()) {
            validated = false
            usernameValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_username))
        }

        if (password.isEmpty()) {
            validated = false
            passwordValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_password))
        }

        if (visitPlace.isEmpty()) {
            validated = false
            visitPlaceValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_visit_place))
        }

        if (attachedClinic.isEmpty()) {
            validated = false
            attachedClinicValidationMessage.set(resourcesWrapper.getString(R.string.login_label_attached_clinic_validation_error))
        }

        return validated
    }
}