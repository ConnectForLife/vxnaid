package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject

class ReadyForMigrationSignaler @Inject constructor(private val syncLogger: SyncLogger) {

    /**
     * we need to wait until sync has stopped downloading records.
     */
    suspend fun awaitReady() = syncLogger.awaitSyncPageProgressNotDownloading()
}