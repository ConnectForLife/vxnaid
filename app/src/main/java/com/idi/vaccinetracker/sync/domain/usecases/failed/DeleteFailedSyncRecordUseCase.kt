package com.idi.vaccinetracker.sync.domain.usecases.failed

import com.idi.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.idi.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import javax.inject.Inject

class DeleteFailedSyncRecordUseCase @Inject constructor(private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository) {

    suspend fun delete(failedSyncRecordDownload: FailedSyncRecordDownload) = failedSyncRecordDownloadRepository.delete(failedSyncRecordDownload)
}