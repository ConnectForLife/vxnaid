package com.idi.vaccinetracker.sync.domain.usecases.error

import com.idi.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.idi.vaccinetracker.sync.data.models.SyncErrorsRequest
import com.idi.vaccinetracker.sync.data.models.toDto
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.entities.SyncError
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class UploadSyncErrorsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val syncErrorRepository: SyncErrorRepository,
) {

    suspend fun upload(syncErrors: List<SyncError>) {
        api.uploadSyncErrors(SyncErrorsRequest(syncErrors.map { it.toDto() }))
        syncErrorRepository.updateSyncErrorState(SyncErrorState.UPLOADED, syncErrors.map { it.metadata })
    }
}