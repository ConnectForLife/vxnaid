package com.idi.vaccinetracker.sync.domain.usecases.error

import com.idi.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class GetSyncErrorCountUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    suspend fun syncErrorCount(): Long = syncErrorRepository.countByErrorStates(SyncErrorState.statesNotResolved())

}