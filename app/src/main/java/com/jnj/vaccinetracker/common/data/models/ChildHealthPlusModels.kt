package com.jnj.vaccinetracker.common.data.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * Child Health+ service categories for simplified registration
 * and service documentation for clients without full immunization history.
 *
 * @param displayName User-facing label shown in UI
 * @param serviceKey Internal identifier stored in visit attributes
 * @param conceptName OpenMRS concept name used in observations
 * @param availableDoses Ordered list of dose labels; empty means single-dose (no selection dialog)
 * @param requiresFollowUp True for services where a next-visit date should be offered
 */
enum class ChildHealthPlusService(
    val displayName: String,
    val serviceKey: String,
    val conceptName: String,
    val availableDoses: List<String> = emptyList(),
    val requiresFollowUp: Boolean = false,
) {
    VITAMIN_A(
        displayName = "Vit A",
        serviceKey = "VIT_A",
        conceptName = "Vitamin A Vxnaid",
        availableDoses = listOf("Dose 1", "Dose 2"),
        requiresFollowUp = true,
    ),
    DEWORMING(
        displayName = "Deworming",
        serviceKey = "DEWORMING",
        conceptName = "Deworming Vxnaid",
        availableDoses = listOf("Dose 1", "Dose 2"),
        requiresFollowUp = true,
    ),
    MR2(
        displayName = "MR2",
        serviceKey = "MR2",
        conceptName = "Measles Rubella 2 (MR2) Vxnaid",
    ),
    HEPATITIS_B(
        displayName = "HepB",
        serviceKey = "HEPB",
        conceptName = "HepB Vxnaid",
        availableDoses = listOf("HepB 1", "HepB 2", "HepB 3"),
    ),
    HPV(
        displayName = "HPV",
        serviceKey = "HPV",
        conceptName = "HPV Vxnaid",
    ),
    TETANUS(
        displayName = "Tetanus",
        serviceKey = "TD",
        conceptName = "Td Vxnaid",
        availableDoses = listOf("Td1", "Td2", "Td3", "Td4", "Td5"),
        requiresFollowUp = true,
    );

    val hasDoseSelection: Boolean get() = availableDoses.isNotEmpty()
}

data class SelectedService(
    val service: ChildHealthPlusService,
    val administrationDate: Date,
    val dose: String? = null,
    val isPregnantWoman: Boolean = false,
    val nextVisitDate: Date? = null,
    val uuid: String = UUID.randomUUID().toString(),
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val formattedAdministrationDate: String
        get() = dateFormat.format(administrationDate)

    val formattedNextVisitDate: String?
        get() = nextVisitDate?.let { dateFormat.format(it) }

    /** Display label combining service name and dose when a specific dose was selected. */
    val displayLabel: String
        get() = if (dose != null) "${service.displayName} – $dose" else service.displayName
}

data class PastServiceItem(
    val displayName: String,
    val date: String,
)