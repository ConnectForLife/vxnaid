package com.idi.vaccinetracker.sync.p2p.common.util

import com.idi.vaccinetracker.common.data.database.ParticipantRoomDatabaseConfig
import com.idi.vaccinetracker.sync.p2p.common.models.DbFile
import javax.inject.Inject

class VmpDatabaseFiles @Inject constructor(private val databaseFolder: DatabaseFolder) {

    companion object {
        private const val DB_FILE = ParticipantRoomDatabaseConfig.FILE_NAME
    }

    fun getDbFile(): DbFile.Src = DbFile.Src(databaseFolder.getFile(DB_FILE))

    fun delete(): Boolean = databaseFolder.deleteDatabase(DB_FILE)
}