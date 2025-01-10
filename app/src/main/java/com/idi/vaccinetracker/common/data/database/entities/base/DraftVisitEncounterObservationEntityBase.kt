package com.idi.vaccinetracker.common.data.database.entities.base

interface DraftVisitEncounterObservationEntityBase : DraftVisitEncounterObservationBase {
    val value: String
}

fun List<DraftVisitEncounterObservationEntityBase>.toMap() = map { it.name to it.value }.toMap()