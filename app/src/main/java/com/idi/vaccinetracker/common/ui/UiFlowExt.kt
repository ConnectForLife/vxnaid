package com.idi.vaccinetracker.common.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow

interface UiFlowExt {

    fun <T> Flow<T>.launchIn(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launchWhenStarted {
            collect { }
        }
    }
}
