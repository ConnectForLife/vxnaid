package com.idi.vaccinetracker.sync.data.mappers

import com.idi.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.idi.vaccinetracker.common.exceptions.SiteNotFoundException
import com.idi.vaccinetracker.sync.data.models.SyncScopeDto
import com.idi.vaccinetracker.sync.data.models.toSyncScopeDto
import com.idi.vaccinetracker.sync.domain.entities.SyncScope
import javax.inject.Inject

class SyncScopeToDtoMapper @Inject constructor(private val getSitesUseCase: GetSitesUseCase) {

    suspend fun toDto(syncScope: SyncScope): SyncScopeDto {
        val sites = getSitesUseCase.getMasterData()
        val site = sites.results.find { it.uuid == syncScope.siteUuid } ?: throw SiteNotFoundException(syncScope.siteUuid)
        return site.toSyncScopeDto(syncScope.level)
    }
}