package com.jnj.vaccinetracker.visit.model

data class SubstanceDataModel(
    val conceptName: String,
    val label: String,
    val category: String,
    val routeOfAdministration: String,
    val group: String,
    val maximumAgeInWeeks: Int?,
    val minimumWeeksNumberAfterPreviousDose: Int?,
    val visitType: String?,
    var obsDate: String? = null
)