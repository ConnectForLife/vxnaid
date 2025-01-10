package com.idi.vaccinetracker.sync.domain.entities

import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.data.database.typealiases.dateNow
import com.idi.vaccinetracker.sync.data.models.SyncScopeLevel

data class SyncScope(val siteUuid: String, val level: SyncScopeLevel, val country: String?, val cluster: String?, val dateCreated: DateEntity) {
    fun isWithinBounds(other: SyncScope) = when (other.level) {
        SyncScopeLevel.COUNTRY -> country.equals(other.country, ignoreCase = true)
        SyncScopeLevel.CLUSTER -> country.equals(other.country, ignoreCase = true) && cluster.equals(other.cluster, ignoreCase = true)
        SyncScopeLevel.SITE -> siteUuid == other.siteUuid
    }

    fun isIdenticalTo(other: SyncScope): Boolean {
        val date = dateNow()
        return copy(dateCreated = date) == other.copy(dateCreated = date)
    }
}