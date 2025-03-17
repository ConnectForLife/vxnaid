package com.jnj.vaccinetracker.reportsoverview.childrenoverview.model

import android.os.Bundle
import androidx.annotation.StringRes
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import javax.inject.Inject

class ReportsOverviewViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    private var screens = listOf<Screen>()
    val launchReportsOverviewFragmentFlowEvent = eventFlow<Unit>()

    init {
        initScreens()
    }

    fun onReportsOverviewClick() {
        launchReportsOverviewFragmentFlowEvent.tryEmit(Unit)
    }

    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.REPORTS_OVERVIEW)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            val screen = screens.firstOrNull()
            currentScreen.set(screen)
        }
    }

    enum class Screen(@StringRes val title: Int) {
        REPORTS_OVERVIEW(R.string.reports_overview_title)
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}