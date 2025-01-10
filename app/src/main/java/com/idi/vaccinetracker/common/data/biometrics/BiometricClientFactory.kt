package com.idi.vaccinetracker.common.data.biometrics

import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.di.BiometricsModule
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import javax.inject.Inject

class BiometricClientFactory @Inject constructor(private val dispatchers: AppCoroutineDispatchers, private val participantDataFileIO: ParticipantDataFileIO) {

    fun create(): BiometricClient {
        val nBiometricClient = BiometricsModule().provideBiometricsClient()
        return BiometricClient(nBiometricClient, dispatchers, participantDataFileIO)
    }
}