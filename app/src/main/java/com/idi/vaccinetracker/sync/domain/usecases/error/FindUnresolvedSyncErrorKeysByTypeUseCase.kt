package com.idi.vaccinetracker.sync.domain.usecases.error

import com.idi.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class FindUnresolvedSyncErrorKeysByTypeUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    suspend fun findUnresolvedSyncErrorKeysByType(type: String): List<String> {
        return syncErrorRepository.findAllSyncErrorKeysByType(SyncErrorState.statesNotResolved(), type)
    }
}