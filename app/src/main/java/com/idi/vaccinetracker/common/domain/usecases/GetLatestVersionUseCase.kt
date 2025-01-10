package com.idi.vaccinetracker.common.domain.usecases

import com.idi.vaccinetracker.common.domain.entities.VaccineTrackerVersion
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class GetLatestVersionUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
) {
    suspend fun getLatestVersion(): VaccineTrackerVersion = api.getLatestVersion()
}