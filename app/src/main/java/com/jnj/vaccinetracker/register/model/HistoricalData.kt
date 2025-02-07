package com.jnj.vaccinetracker.register.model

import java.util.Date


data class HistoricalData(
    val visitDate: Date?,
    val value: Map<String, SubstancesData>
)