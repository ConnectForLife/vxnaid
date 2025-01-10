package com.idi.vaccinetracker.sync.domain.usecases.synccompleted

import com.idi.vaccinetracker.common.exceptions.NoNetworkException
import com.idi.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.idi.vaccinetracker.common.exceptions.ReportSyncCompletedDateException
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import com.idi.vaccinetracker.sync.data.models.SyncCompleteRequest
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.yield
import javax.inject.Inject

class ReportSyncCompleteUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val syncLogger: SyncLogger,
) {

    suspend fun reportSyncComplete(): Boolean {
        val syncDate = syncLogger.getSyncCompletedDate() ?: return false
        if (syncDate == syncLogger.getLastReportedSyncCompletedDate()) {
            logInfo("already reported completed sync date $syncDate so skipping reporting")
            return false
        }
        val syncErrorMetadata = SyncErrorMetadata.ReportSyncCompletedDateCall(syncDate)
        val siteUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException("can't report sync date without site uuid")
        return try {
            api.reportSyncComplete(SyncCompleteRequest(siteUuid, syncDate))
            syncLogger.logSyncCompletedDateReported(syncDate)
            syncLogger.clearSyncError(syncErrorMetadata)
            true
        } catch (ex: NoNetworkException) {
            logInfo("no network available to report sync completed")
            false
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            val wrappedEx = ReportSyncCompletedDateException("Error during sync completed date call [$syncDate]", ex)
            syncLogger.logSyncError(syncErrorMetadata, wrappedEx)
            throw wrappedEx
        }
    }
}