package com.jnj.vaccinetracker.common.data.models

import java.text.SimpleDateFormat
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
    VITAMIN_A("Vit A", "VIT_A", "Vitamin A Vxnaid"),
    DEWORMING("Deworming", "DEWORMING", "Deworming Vxnaid"),
    MR2("MR2", "MR2", "Measles Rubella 2 (MR2) Vxnaid"),
    HEPATITIS_B("HepB", "HEPB", "HepB Vxnaid"),
    HPV("HPV", "HPV", "HPV Vxnaid"),
    TETANUS("Td", "TD", "Td Vxnaid");
}

data class SelectedService(
    val service: ChildHealthPlusService,
    val administrationDate: Date,
    val uuid: String = UUID.randomUUID().toString()
) {
    val formattedAdministrationDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(administrationDate)
}

data class PastServiceItem(
    val displayName: String,
    val date: String,
)


