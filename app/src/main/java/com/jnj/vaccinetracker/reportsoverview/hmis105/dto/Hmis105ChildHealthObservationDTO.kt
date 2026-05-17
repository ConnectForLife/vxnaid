package com.jnj.vaccinetracker.reportsoverview.hmis105.dto

import com.jnj.vaccinetracker.common.domain.entities.Gender

data class Hmis105ChildHealthObservationDTO(
    val dose: String,
    val administerDate: String,
    val visitLocation: String,
    val ageGroup: String,
    val gender: Gender,
    val attachedClinic: String? = null
)
