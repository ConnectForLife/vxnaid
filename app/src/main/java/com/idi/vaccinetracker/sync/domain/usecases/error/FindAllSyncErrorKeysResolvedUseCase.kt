package com.idi.vaccinetracker.sync.domain.usecases.error

import com.idi.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class FindAllSyncErrorKeysResolvedUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    suspend fun findAllSyncErrorKeysResolved(limit: Int): List<String> {
        require(limit > 0)
        return syncErrorRepository.findAllSyncErrorKeysByErrorState(SyncErrorState.RESOLVED, limit)
    }
}