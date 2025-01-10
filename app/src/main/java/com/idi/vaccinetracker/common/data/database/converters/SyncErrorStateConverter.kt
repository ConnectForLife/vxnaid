package com.idi.vaccinetracker.common.data.database.converters

import androidx.room.TypeConverter
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState

class SyncErrorStateConverter {

    @TypeConverter
    fun toEnum(code: Int?): SyncErrorState? {
        return code?.let { SyncErrorState.fromCode(it) }
    }

    @TypeConverter
    fun toString(syncErrorState: SyncErrorState?): Int? {
        return syncErrorState?.code
    }
}