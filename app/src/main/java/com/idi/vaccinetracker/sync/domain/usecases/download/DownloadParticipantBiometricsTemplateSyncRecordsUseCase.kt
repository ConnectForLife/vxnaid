package com.idi.vaccinetracker.sync.domain.usecases.download

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.sync.data.models.ParticipantBiometricsTemplateSyncRecord
import com.idi.vaccinetracker.sync.data.models.SyncRequest
import com.idi.vaccinetracker.sync.data.models.SyncResponse
import com.idi.vaccinetracker.sync.data.models.toFailedSyncRecordDownload
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.idi.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCaseBase
import com.idi.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.idi.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import com.idi.vaccinetracker.sync.domain.usecases.store.StoreParticipantBiometricsTemplateSyncRecordUseCase
import javax.inject.Inject

class DownloadParticipantBiometricsTemplateSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantBiometricsTemplateSyncRecordUseCase: StoreParticipantBiometricsTemplateSyncRecordUseCase,
    override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val syncLogger: SyncLogger, override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<ParticipantBiometricsTemplateSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.BIOMETRICS_TEMPLATE

    override suspend fun storeSyncRecord(record: ParticipantBiometricsTemplateSyncRecord) {
        storeParticipantBiometricsTemplateSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<ParticipantBiometricsTemplateSyncRecord> {
        return api.getAllParticipantBiometricsTemplates(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: ParticipantBiometricsTemplateSyncRecord, dateLastDownloadAttempt: DateEntity) =
        record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
}