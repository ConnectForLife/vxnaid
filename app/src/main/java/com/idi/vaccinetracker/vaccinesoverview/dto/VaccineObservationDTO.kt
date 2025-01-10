package com.idi.vaccinetracker.vaccinesoverview.dto

class VaccineObservationDTO(
    val vaccineName: String,
    val administerDate: String,
    var visitLocation: String,
    val ageGroup: String
)