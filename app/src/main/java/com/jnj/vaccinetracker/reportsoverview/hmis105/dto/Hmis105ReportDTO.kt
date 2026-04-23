package com.jnj.vaccinetracker.reportsoverview.hmis105.dto

data class Hmis105ReportDTO(
    val doses: String,
    val under1Static: Int = 0,
    val under1Outreach: Int = 0,
    val age1to4Static: Int = 0,
    val age1to4Outreach: Int = 0,
    val age5to14Static: Int = 0,
    val age5to14Outreach: Int = 0,
    val total: Int = 0
)

