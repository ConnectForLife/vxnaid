package com.jnj.vaccinetracker.common.data.models

import java.util.*

/**
 * Child Health+ service categories for simplified registration
 * and service documentation for clients without full immunization history
 */
enum class ChildHealthPlusService(val displayName: String, val serviceKey: String) {
    VITAMIN_A("Vitamin A (Vit A)", "VIT_A"),
    DEWORMING("Deworming", "DEWORMING"),
    MR1("MR1", "MR1"),
    HEPATITIS_B("Hepatitis B (HepB)", "HEPB"),
    HPV("Human Papillomavirus (HPV)", "HPV"),
    TETANUS_DIPHTHERIA("Tetanus/Diphtheria (Td)", "TD")
}

/**
 * Dose numbers for different services
 */
enum class DoseNumber(val label: String, val doseKey: String) {
    // Vitamin A & Deworming
    DOSE_1("Dose 1", "1"),
    DOSE_2("Dose 2", "2"),
    
    // Tetanus/Diphtheria
    TD_1("Td1", "TD_1"),
    TD_2("Td2", "TD_2"),
    TD_3("Td3", "TD_3"),
    TD_4("Td4", "TD_4"),
    TD_5("Td5", "TD_5"),
    
    // Hepatitis B
    HEPB_1("HepB1", "HEPB_1"),
    HEPB_2("HepB2", "HEPB_2"),
    HEPB_3("HepB3", "HEPB_3")
}

/**
 * Represents a selected service with dose information
 */
data class SelectedService(
    val service: ChildHealthPlusService,
    val dose: DoseNumber? = null,
    val administrationDate: Date,
    val nextVisitDate: Date? = null,
    val uuid: String = UUID.randomUUID().toString()
)

/**
 * Represents a Child Health+ client record
 */
data class ChildHealthPlusData(
    val uuid: String = UUID.randomUUID().toString(),
    val participantUuid: String? = null,
    val clientName: String,
    val dateOfBirth: Date,
    val sex: String, // "M" or "F"
    val contactInfo: String,
    val mothersName: String,
    val services: List<SelectedService> = emptyList(),
    val isPregnantWoman: Boolean = false,
    val locationUuid: String? = null,
    val operatorUuid: String? = null,
    val dateCreated: Date = Date(),
    val dateModified: Date = Date()
) {
    val ageMonths: Int
        get() {
            val calendar = Calendar.getInstance()
            val age = calendar.get(Calendar.YEAR) - getYearOfBirth()
            return age * 12
        }

    private fun getYearOfBirth(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = dateOfBirth
        return calendar.get(Calendar.YEAR)
    }
}

