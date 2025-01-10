package com.idi.vaccinetracker.sync.p2p.domain.usecases

import com.idi.vaccinetracker.common.data.database.ParticipantRoomDatabase
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.yield
import javax.inject.Inject

class IsDatabaseValidUseCase @Inject constructor(
    private val participantRoomDatabaseFactory: ParticipantRoomDatabase.Factory
) {

    /**
     * @return whether valid
     */
    suspend fun isDatabaseValid(fileName: String): Boolean {
        logInfo("isDatabaseValid: $fileName")
        // create database, open data and do a query
        return try {
            val db = participantRoomDatabaseFactory.createDatabaseWithDefaultPassphrase(fileName, deleteDatabaseIfCorrupt = false)
            db.close()
            true
        } catch (ex: Exception) {
            yield()
            logError("failed to open query database: $fileName", ex)
            false
        }
    }
}