package com.idi.vaccinetracker.common.data.database.entities.base

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

interface SyncBase {
    val dateModified: DateEntity

    companion object {
        const val COL_DATE_MODIFIED = "dateModified"
    }
}