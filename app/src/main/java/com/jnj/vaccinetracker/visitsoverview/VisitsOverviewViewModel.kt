package com.jnj.vaccinetracker.visitsoverview

import android.os.Bundle
import androidx.annotation.StringRes
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import javax.inject.Inject

class VisitsOverviewViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val currentScreen = mutableLiveData<Screen>()
    private val visitTypeName = mutableLiveData<String>()
    var navigationDirection = NavigationDirection.NONE
    private var screens = listOf<Screen>()
    val launchVisitsOverviewFragmentFlowEvent = eventFlow<Unit>()

    companion object {
        const val VISITS_OVERVIEW_SCHEDULED_VISITS_KEY = "Scheduled Visits"
        const val VISITS_OVERVIEW_HISTORICAL_VISITS_KEY = "Historical Visits"
        const val VISITS_OVERVIEW_MISSED_VISITS_KEY = "Missed Visits"
    }

    fun getScreenByKey(key: String): Screen? {
        return when (key) {
            VISITS_OVERVIEW_SCHEDULED_VISITS_KEY -> Screen.SCHEDULED_VISITS
            VISITS_OVERVIEW_HISTORICAL_VISITS_KEY -> Screen.HISTORICAL_VISITS
            VISITS_OVERVIEW_MISSED_VISITS_KEY -> Screen.MISSED_VISITS
            else -> null
        }
    }



    init {
        initScreens()
    }

    fun onVisitsOverviewClick() {
        launchVisitsOverviewFragmentFlowEvent.tryEmit(Unit)
    }

    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.VISITS_OVERVIEW)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            val screen = screens.firstOrNull()
            currentScreen.set(screen)
        }
    }

    fun navigateBack(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val screens = Screen.entries.toTypedArray()
        val previousScreenIndex = screens.indexOf(currentScreen) - 1

        if (previousScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.BACKWARD
            this.currentScreen.set(screens[previousScreenIndex])
            return true
        }

        return false
    }

    fun openVisitOverView(visitTypeName: String) {
        this.visitTypeName.set(visitTypeName)
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.VISITS_OVERVIEW)
        this.visitTypeName.set(null)
    }

    enum class Screen(@StringRes val title: Int) {
        VISITS_OVERVIEW(R.string.visits_overview_title),
        SCHEDULED_VISITS(R.string.visits_overview_scheduled_visits_title),
        HISTORICAL_VISITS(R.string.visits_overview_historical_visits_title),
        MISSED_VISITS(R.string.visits_overview_missed_visits_title)

    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}