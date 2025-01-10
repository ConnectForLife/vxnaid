package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.common.data.database.typealiases.dateNow
import com.idi.vaccinetracker.common.domain.usecases.GetSelectedSiteUseCase
import com.idi.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.idi.vaccinetracker.sync.domain.entities.SyncScope
import javax.inject.Inject

class BuildSyncScopeUseCase @Inject constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val getSelectedSiteUseCase: GetSelectedSiteUseCase,
) {
    suspend fun buildSyncScope(): SyncScope {
        val selectedSite = getSelectedSiteUseCase.getSelectedSite()
        val config = getConfigurationUseCase.getMasterData()
        val scopeLevel = config.syncScope
        return SyncScope(
            siteUuid = selectedSite.uuid,
            level = scopeLevel,
            dateCreated = dateNow(),
            cluster = selectedSite.cluster,
            country = selectedSite.country)
    }
}