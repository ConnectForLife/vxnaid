package com.idi.vaccinetracker.sync.domain.usecases.download

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.sync.data.models.ParticipantImageSyncRecord
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
import com.idi.vaccinetracker.sync.domain.usecases.store.StoreParticipantImageSyncRecordUseCase
import javax.inject.Inject

class DownloadParticipantImageSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantImageSyncRecordUseCase: StoreParticipantImageSyncRecordUseCase,
    override val syncLogger: SyncLogger, override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<ParticipantImageSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.IMAGE

    override suspend fun storeSyncRecord(record: ParticipantImageSyncRecord) {
        storeParticipantImageSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<ParticipantImageSyncRecord> {
        return api.getAllParticipantImages(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: ParticipantImageSyncRecord, dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
        return record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
    }

}