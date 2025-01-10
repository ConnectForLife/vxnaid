package com.idi.vaccinetracker.sync.domain.usecases.download

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.sync.data.models.ParticipantSyncRecord
import com.idi.vaccinetracker.sync.data.models.SyncRequest
import com.idi.vaccinetracker.sync.data.models.SyncResponse
import com.idi.vaccinetracker.sync.data.models.toFailedSyncRecordDownload
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.idi.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCaseBase
import com.idi.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.idi.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import com.idi.vaccinetracker.sync.domain.usecases.store.StoreParticipantSyncRecordUseCase
import javax.inject.Inject

class DownloadParticipantSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantSyncRecordUseCase: StoreParticipantSyncRecordUseCase,
    override val syncLogger: SyncLogger, override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<ParticipantSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.PARTICIPANT

    override suspend fun storeSyncRecord(record: ParticipantSyncRecord) {
        storeParticipantSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<ParticipantSyncRecord> {
        return api.getAllParticipants(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: ParticipantSyncRecord, dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
        return record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
    }
}