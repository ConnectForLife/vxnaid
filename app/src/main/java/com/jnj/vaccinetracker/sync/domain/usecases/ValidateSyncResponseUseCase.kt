package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.exceptions.SyncResponseValidationException
import com.jnj.vaccinetracker.common.exceptions.TotalSyncScopeRecordCountMismatchException
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.SyncRequest
import com.jnj.vaccinetracker.sync.data.models.SyncResponse
import com.jnj.vaccinetracker.sync.data.models.SyncStatus
import com.jnj.vaccinetracker.sync.domain.usecases.delete.GetDeletedSyncRecordCountUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.GetFailedSyncRecordDownloadCountUseCase
import javax.inject.Inject

class ValidateSyncResponseUseCase @Inject constructor(
    private val getUploadedDraftCountUseCase: GetUploadedDraftCountUseCase,
    private val getSyncRecordCountUseCase: GetSyncRecordCountUseCase,
    private val getFailedSyncRecordDownloadCountUseCase: GetFailedSyncRecordDownloadCountUseCase,
    private val getDeletedSyncRecordCountUseCase: GetDeletedSyncRecordCountUseCase,
) {

    private fun <T> T?.shouldBe(other: T?, name: String) {
        require(this == other) { "Invalid sync response [$name], got '$this' but expected '$other'" }
    }

    private suspend fun validateCounts(optimize: Boolean, totalSyncScopeRecordCount: Long, ignoredCount: Long?, voidedCount: Long?, syncEntityType: SyncEntityType) {
        val failedCount = getFailedSyncRecordDownloadCountUseCase.count(syncEntityType)
        val successCount = getSyncRecordCountUseCase.getCount(syncEntityType)
        val deletedCount = getDeletedSyncRecordCountUseCase.count(syncEntityType)
        val syncEntityCount = successCount + failedCount + deletedCount
        // Log computed counts for diagnostics
        logInfo("validateCounts: optimize=$optimize, totalSyncScopeRecordCount=$totalSyncScopeRecordCount, ignoredCount=$ignoredCount, voidedCount=$voidedCount")
        logInfo("validateCounts: successCount=$successCount, failedCount=$failedCount, deletedCount=$deletedCount, syncEntityCount=$syncEntityCount")

        if (optimize) {
            val draftCount = getUploadedDraftCountUseCase.getCount(syncEntityType)
            val count = draftCount + syncEntityCount
            logInfo("validateCounts (optimize): draftCount=$draftCount, local total including drafts=$count")
            if (count > totalSyncScopeRecordCount) {
                val message = "local sync record count is $syncEntityCount including $draftCount uploaded drafts, $deletedCount voided, $failedCount failed. " +
                        "But backend totalSyncScopeRecordCount is $totalSyncScopeRecordCount of which $ignoredCount are expected to be uploaded drafts and $voidedCount are expected to be voided"
                // extra diagnostic logs
                logWarn("ValidateSyncResponse failed (optimize). Details: successCount=$successCount, draftCount=$draftCount, failedCount=$failedCount, deletedCount=$deletedCount, localTotalIncludingDrafts=$count, backendTotal=$totalSyncScopeRecordCount, ignoredCount=$ignoredCount, voidedCount=$voidedCount")
                logWarn("Full message: $message")
                // TotalSyncScopeRecordCountMismatchException expects (message, backendTableCount)
                throw TotalSyncScopeRecordCountMismatchException(message = message, backendTableCount = totalSyncScopeRecordCount)
            } else if (count < totalSyncScopeRecordCount) {
                // Backend has MORE records than local: log warning but don't fail the sync. This is a safe condition indicating local is behind.
                logWarn("Backend total ($totalSyncScopeRecordCount) is greater than local total including drafts ($count). Continuing sync; local DB may be incomplete.")
            }
        } else {
            if (syncEntityCount > totalSyncScopeRecordCount) {
                val message = "local sync record count is $syncEntityCount including $deletedCount voided, $failedCount failed but backend totalSyncScopeRecordCount is $totalSyncScopeRecordCount of which $voidedCount are expected to be voided"
                logWarn("ValidateSyncResponse failed. Details: successCount=$successCount, failedCount=$failedCount, deletedCount=$deletedCount, localTotal=$syncEntityCount, backendTotal=$totalSyncScopeRecordCount, voidedCountExpected=$voidedCount")
                logWarn("Full message: $message")
                throw TotalSyncScopeRecordCountMismatchException(message = message, backendTableCount = totalSyncScopeRecordCount)
            } else if (syncEntityCount < totalSyncScopeRecordCount) {
                // Backend has MORE records than local: log and continue
                logWarn("Backend total ($totalSyncScopeRecordCount) is greater than local total ($syncEntityCount). Continuing sync; local DB may be incomplete.")
            }
        }
    }


    suspend fun validate(syncResponse: SyncResponse<*>, syncRequest: SyncRequest, syncEntityType: SyncEntityType) {
        with(syncResponse) {
            try {
                dateModifiedOffset.shouldBe(syncRequest.dateModifiedOffset, "dateModifiedOffset")
                syncScope.shouldBe(syncRequest.syncScope, "syncScope")
                val elementsMissing = uuidsWithDateModifiedOffset - syncRequest.uuidsWithDateModifiedOffset
                require(elementsMissing.isEmpty()) { "uuidsWithDateModifiedOffset is missing uuids $elementsMissing" }
                require(uuidsWithDateModifiedOffset.size == syncRequest.uuidsWithDateModifiedOffset.size) {
                    "syncRequest.uuidsWithDateModifiedOffset.size " +
                            "${syncRequest.uuidsWithDateModifiedOffset.size} must be equal to syncRequest.uuidsWithDateModifiedOffset.size ${syncRequest.uuidsWithDateModifiedOffset.size}"
                }
                limit.shouldBe(syncRequest.limit, "limit")
                val maxLimit = limit + uuidsWithDateModifiedOffset.size
                require(records.size <= maxLimit) { "record count (${records.size}) must not be greater than limit $limit + uuidsWithDateModifiedOffset $uuidsWithDateModifiedOffset = $maxLimit" }
                if (records.size > limit) {
                    logWarn("records size ${records.size} is greater than limit $limit {}", syncRequest)
                }
                when (syncStatus) {
                    SyncStatus.OUT_OF_SYNC -> require(records.isNotEmpty()) { "got OUT_OF_SYNC status but empty records" }
                    SyncStatus.OK -> require(records.isEmpty()) { "got OK status but not empty records" }
                }.let {}
            } catch (ex: Exception) {
                throw SyncResponseValidationException(ex, syncResponse.syncStatus)
            }
            if (totalSyncScopeRecordCount != null && syncStatus == SyncStatus.OK) {
                // call validateCounts with positional, non-null safe arguments to avoid named-parameter mismatch
                validateCounts(
                    syncRequest.optimize,
                    totalSyncScopeRecordCount,
                    totalIgnoredRecordCount,
                    totalVoidedRecordCount,
                    syncEntityType
                )
            }
        }

    }
}