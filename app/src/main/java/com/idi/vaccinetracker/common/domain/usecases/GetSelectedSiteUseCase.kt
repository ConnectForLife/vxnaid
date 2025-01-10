package com.idi.vaccinetracker.common.domain.usecases

import com.idi.vaccinetracker.common.domain.entities.Site
import com.idi.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.idi.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.idi.vaccinetracker.common.exceptions.SiteNotFoundException
import com.idi.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import javax.inject.Inject

class GetSelectedSiteUseCase @Inject constructor(
    private val syncSettingsRepository: SyncSettingsRepository,
    private val getSitesUseCase: GetSitesUseCase,
) {

    suspend fun getSelectedSite(): Site {
        val siteUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException()
        val allSites = getSitesUseCase.getMasterData().results
        return allSites.find { it.uuid == siteUuid } ?: throw SiteNotFoundException(siteUuid)
    }
}