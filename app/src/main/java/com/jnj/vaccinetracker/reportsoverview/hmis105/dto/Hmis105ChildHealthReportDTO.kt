package com.jnj.vaccinetracker.reportsoverview.hmis105.dto

data class Hmis105ChildHealthReportDTO(
    val dose: String,
    val visitLocation: String,
    val months6to11Male: Int = 0,
    val months6to11Female: Int = 0,
    val months12to59Male: Int = 0,
    val months12to59Female: Int = 0,
    val years5to14Male: Int = 0,
    val years5to14Female: Int = 0,
    val total: Int = 0
) {
    val isTotalRow: Boolean get() = dose == "TOTAL"
}
