package com.jnj.vaccinetracker.visit.model

data class OtherSubstanceDataModel(
    val conceptName: String,
    val label: String,
    val category: String,
    val inputType: String,
    val visitType: String,
    val options: List<String>,
    var value: String? = null
)