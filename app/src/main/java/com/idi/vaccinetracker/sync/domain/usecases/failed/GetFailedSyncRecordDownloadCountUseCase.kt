package com.idi.vaccinetracker.sync.domain.usecases.failed

import com.idi.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import javax.inject.Inject

class GetFailedSyncRecordDownloadCountUseCase @Inject constructor(private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository) {

    suspend fun count(syncEntityType: SyncEntityType) = failedSyncRecordDownloadRepository.count(syncEntityType)
}