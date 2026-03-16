package com.jnj.vaccinetracker.reportsoverview.hmis105.dto

import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import java.util.Date

data class Hmis105ObservationDTO(
    val vaccineName: String,
    val visitDate: Date,
    val birthDate: BirthDate,
    val visitLocation: String, // Static, Outreach, School
    val participantUuid: String
)

