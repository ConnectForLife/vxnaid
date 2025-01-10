package com.idi.vaccinetracker.sync.p2p.domain.usecases

import com.idi.vaccinetracker.common.data.database.ParticipantRoomDatabaseConfig
import com.idi.vaccinetracker.sync.p2p.common.util.DatabaseFolder
import javax.inject.Inject

class GetDatabaseSizeUseCase @Inject constructor(private val databaseFolder: DatabaseFolder) {

    private val dbName = ParticipantRoomDatabaseConfig.FILE_NAME

    fun getDatabaseSize(): Long = listOf(
        databaseFolder.getFile(dbName),
        databaseFolder.getFile("$dbName-shm"),
        databaseFolder.getFile("$dbName-wal")
    ).map { it.length() }.sum()
}