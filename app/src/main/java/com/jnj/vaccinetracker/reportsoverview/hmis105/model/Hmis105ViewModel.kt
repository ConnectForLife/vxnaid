package com.jnj.vaccinetracker.reportsoverview.hmis105.model

import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ReportDTO
import com.jnj.vaccinetracker.reportsoverview.hmis105.repository.Hmis105Repository
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.coroutines.launch
import javax.inject.Inject

class Hmis105ViewModel @Inject constructor(
    userRepository: UserRepository,
    private val hmisMalaria105Repository: Hmis105Repository,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val reportDTOs = mutableLiveData<List<Hmis105ReportDTO>>(emptyList())
    val isLoading = mutableLiveData<Boolean>(false)
    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    
    private var screens = listOf<Screen>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    init {
        initScreens()
    }

    fun getHmisMalaria105Data(startDate: DateTime?, endDate: DateTime?) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val start = startDate?.toDate() ?: getTodayMidnight()
                val end = endDate?.toDate() ?: addDaysToDate(getTodayMidnight(), 1)
                
                Log.d("Hmis105ViewModel", "Loading report data from $start to $end")
                val data = hmisMalaria105Repository.getHmisMalaria105ReportData(start, end, currentLocationUuid)
                Log.d("Hmis105ViewModel", "Report data loaded: ${data.size} rows")
                reportDTOs.value = data
            } catch (ex: Exception) {
                Log.e("Hmis105ViewModel", "Error loading report data", ex)
                reportDTOs.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.HMIS105_REPORT)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            val screen = screens.firstOrNull()
            currentScreen.set(screen)
        }
    }

    enum class Screen(@StringRes val label: Int) {
        HMIS105_REPORT(R.string.hmis105_report_title)
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}

