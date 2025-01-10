package com.idi.vaccinetracker.sync.domain.usecases.failed

import com.idi.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.sync.data.models.SyncDate
import com.idi.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import javax.inject.Inject

class FindAllFailedSyncRecordsByDateLastDownloadAttemptUseCase @Inject constructor(private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository) {

    suspend fun findAllByDateLastDownloadAttemptLesserThan(syncEntityType: SyncEntityType, date: SyncDate, limit: Int): List<FailedSyncRecordDownload> {
        return failedSyncRecordDownloadRepository.findAllByDateLastDownloadAttemptLesserThan(syncEntityType, date.date, 0, limit)
    }
}