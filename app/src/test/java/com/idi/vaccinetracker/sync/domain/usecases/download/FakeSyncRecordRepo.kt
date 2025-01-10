package com.idi.vaccinetracker.sync.domain.usecases.download

import com.idi.vaccinetracker.sync.data.models.SyncRecordBase
import com.idi.vaccinetracker.sync.data.models.uuid

class FakeSyncRecordRepo<R : SyncRecordBase> {
    private val recordMap = mutableMapOf<String, R>()
    fun storeSyncRecord(syncRecord: R) {
        if (recordMap.containsKey(syncRecord.uuid))
            throw Exception("already contains ${syncRecord.uuid}")
        recordMap[syncRecord.uuid] = syncRecord
    }

    val size get() = recordMap.size

    val values get() = recordMap.values
}