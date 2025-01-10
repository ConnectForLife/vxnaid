package com.idi.vaccinetracker.common.data.database.daos.base

import com.idi.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel

interface SyncDao {
    suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>
    suspend fun deleteAll()
}