package com.idi.vaccinetracker.sync.domain.usecases.download

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.sync.data.models.SyncRequest
import com.idi.vaccinetracker.sync.data.models.SyncResponse
import com.idi.vaccinetracker.sync.data.models.VisitSyncRecord
import com.idi.vaccinetracker.sync.data.models.toFailedSyncRecordDownload
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.idi.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCaseBase
import com.idi.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.idi.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import com.idi.vaccinetracker.sync.domain.usecases.store.StoreVisitSyncRecordUseCase
import javax.inject.Inject

class DownloadVisitSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeVisitSyncRecordUseCase: StoreVisitSyncRecordUseCase,
    override val syncLogger: SyncLogger, override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<VisitSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.VISIT

    override suspend fun storeSyncRecord(record: VisitSyncRecord) {
        storeVisitSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<VisitSyncRecord> {
        return api.getAllVisits(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: VisitSyncRecord, dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
        return record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
    }


}