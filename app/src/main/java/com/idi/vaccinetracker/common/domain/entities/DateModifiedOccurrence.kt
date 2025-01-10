package com.idi.vaccinetracker.common.domain.entities

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

data class DateModifiedOccurrence(
    val dateModified: DateEntity,
    val uuids: List<String>,
) {
    val count: Int get() = uuids.size
}