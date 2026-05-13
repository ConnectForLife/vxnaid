package com.jnj.vaccinetracker.common.data.models

import java.util.*

/**
 * Child Health+ service categories for simplified registration
 * and service documentation for clients without full immunization history
 */
enum class ChildHealthPlusService(
    val displayName: String,
    val serviceKey: String,
    val conceptName: String,
) {
    VITAMIN_A("Vitamin A (Vit A)", "VIT_A", "Vitamin A Vxnaid"),
    DEWORMING("Deworming", "DEWORMING", "Deworming Vxnaid"),
    MR1("MR1", "MR1", "Measles Rubella 1 (MR1) Vxnaid"),
    HEPATITIS_B("Hepatitis B (HepB)", "HEPB", "HepB Vxnaid"),
    HPV("Human Papillomavirus (HPV)", "HPV", "HPV Vxnaid"),
    TETANUS_DIPHTHERIA("Tetanus/Diphtheria (Td)", "TD", "Td Vxnaid");

    fun requiresNextVisitScheduling(): Boolean =
        this == VITAMIN_A || this == DEWORMING || this == TETANUS_DIPHTHERIA
}

/**
 * Dose numbers for different services
 */
enum class DoseNumber(val label: String, val doseKey: String, val order: Int) {
    // Vitamin A & Deworming
    DOSE_1("Dose 1", "1", 1),
    DOSE_2("Dose 2", "2", 2),

    // Tetanus/Diphtheria
    TD_1("Td1", "TD_1", 1),
    TD_2("Td2", "TD_2", 2),
    TD_3("Td3", "TD_3", 3),
    TD_4("Td4", "TD_4", 4),
    TD_5("Td5", "TD_5", 5),

    // Hepatitis B
    HEPB_1("HepB1", "HEPB_1", 1),
    HEPB_2("HepB2", "HEPB_2", 2),
    HEPB_3("HepB3", "HEPB_3", 3)
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


