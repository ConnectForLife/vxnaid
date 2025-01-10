package com.idi.vaccinetracker.common.data.biometrics

import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.logWarn
import com.neurotec.biometrics.client.NBiometricClient

fun NBiometricClient.cancelSilently() {
    try {
        if (!isDisposed) {
            logWarn("cancelSilently")
            cancel()
        } else {
            logWarn("cancelSilently already disposed")
        }
    } catch (ex: Exception) {
        logError("failed to cancel NBiometricClient", ex)
    }

}