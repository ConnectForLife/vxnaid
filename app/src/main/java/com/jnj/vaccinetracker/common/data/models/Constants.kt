package com.jnj.vaccinetracker.common.data.models

import com.jnj.vaccinetracker.sync.data.models.VisitType

/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
object Constants {

    const val IRIS_TEMPLATE_NAME = "irisTemplate.dat"

    // Participant
    const val ATTRIBUTE_LOCATION = "LocationAttribute"
    const val ATTRIBUTE_LANGUAGE = "personLanguage"
    const val ATTRIBUTE_TELEPHONE = "Telephone Number"
    const val ATTRIBUTE_VACCINE = "Vaccination program"
    const val ATTRIBUTE_ORIGINAL_PARTICIPANT_ID = "originalParticipantId"
    const val ATTRIBUTE_IS_BIRTH_DATE_ESTIMATED = "Is Birth Date Estimated"
    const val ATTRIBUTE_BIRTH_WEIGHT= "Birth Weight"
    const val ATTRIBUTE_MOTHER_NAME= "Mother's name"
    const val ATTRIBUTE_FATHER_NAME= "Father's name"
    const val ATTRIBUTE_PARTICIPANT_NAME= "Child's name"
    const val ATTRIBUTE_CHILD_CATEGORY= "Child category"
    const val NIN_IDENTIFIER_TYPE_NAME = "National ID"
    const val MOTHER_NAME_ATTRIBUTE_TYPE_NAME = "Mother's name"
    const val CHILD_NAME_ATTRIBUTE_TYPE_NAME = "Child's name"
    const val FATHER_NAME_ATTRIBUTE_TYPE_NAME = "Father's name"
    const val CHILD_CATEGORY_ATTRIBUTE_TYPE_NAME = "Child category"

    // Visit
    const val ATTRIBUTE_VISIT_STATUS = "Visit Status"
    const val ATTRIBUTE_VISIT_DAYS_AFTER = "Up Window"
    const val ATTRIBUTE_VISIT_DAYS_BEFORE = "Low Window"
    const val ATTRIBUTE_VISIT_VACCINE_MANUFACTURER = "Vaccine Manufacturer"
    const val ATTRIBUTE_VISIT_DOSE_NUMBER = "Dose number"
    const val ATTRIBUTE_VISIT_LOCATION = "Visit Location"
    const val VISIT_TYPE_DOSING = "Dosing"
    const val VISIT_TYPE_OTHER = "Other"
    const val VISIT_STATUS_OCCURRED = "OCCURRED"
    const val VISIT_STATUS_MISSED = "MISSED"
    const val VISIT_STATUS_SCHEDULED = "SCHEDULED"
    const val OBSERVATION_TYPE_BARCODE = "Barcode"
    const val OBSERVATION_TYPE_MANUFACTURER = "Vaccine Manufacturer"
    const val OBSERVATION_TYPE_VISIT_WEIGHT = "Weight (kg)"
    const val OBSERVATION_TYPE_VISIT_HEIGHT = "Height (cm)"
    const val OBSERVATION_TYPE_VISIT_MUAC = "MUAC"
    const val OBSERVATION_TYPE_VISIT_OEDEMA = "Is Oedema"
    const val RESCHEDULE_VISIT_REASON_ATTRIBUTE_TYPE_NAME = "Reschedule Visit Reason"

    // common attributes
    const val ATTRIBUTE_OPERATOR = "operatorUuid"

    /**
     * upcoming in person visit types
     */
    val SUPPORTED_UPCOMING_VISIT_TYPES = listOf(VisitType.DOSING, VisitType.IN_PERSON_FOLLOW_UP, VisitType.OTHER)

    //  ROLES
    const val ROLE_SYNC_ADMIN = "Sync Admin"
    const val ROLE_OPERATOR = "Operator"

    // PROGRESS
    const val MAX_PERCENT = 100

    const val REQ_REGISTER_PARTICIPANT = 453
    const val REQ_VISIT = 12

    const val UTC_TIME_ZONE_NAME = "UTC"

    const val BARCODE_STR = "Barcode"
    const val MANUFACTURER_NAME_STR = "Manufacturer"
    const val DATE_STR = "Date"

    const val VISIT_TYPE_AT_BIRTH = "At Birth"
    const val VISIT_TYPE_SIX_WEEKS = "6 weeks"
    const val VISIT_TYPE_TEN_WEEKS = "10 weeks"
    const val VISIT_TYPE_FOURTEEN_WEEKS = "14 weeks"
    const val VISIT_TYPE_NINE_MONTHS = "9 months"
    const val VISIT_TYPE_EIGHTEEN_MONTHS = "18 months"
    const val VISIT_TYPE_TWO_YEARS = "2 years"

    val VISIT_TYPES = listOf(
        VISIT_TYPE_AT_BIRTH,
        VISIT_TYPE_SIX_WEEKS,
        VISIT_TYPE_TEN_WEEKS,
        VISIT_TYPE_FOURTEEN_WEEKS,
        VISIT_TYPE_NINE_MONTHS,
        VISIT_TYPE_EIGHTEEN_MONTHS,
        VISIT_TYPE_TWO_YEARS
    )

    const val REFERRAL_CLINIC_CONCEPT_NAME = "Referral Clinic Vxnaid";
    const val REFERRAL_ADDITIONAL_INFO_CONCEPT_NAME = "Referral Additional Info Vxnaid";

    const val SUBSTANCES_AND_DATES_STR = "substancesAndDates"
    const val OTHER_SUBSTANCES_AND_VALUES_STR = "otherSubstancesAndValues"

    const val CONCEPT_NAME_WEIGHT_FOR_AGE_Z_SCORE = "Weight for age Z score"
    const val CONCEPT_NAME_HEIGHT_FOR_AGE_Z_SCORE = "Height for age Z score Vxnaid"
    const val CONCEPT_NAME_MUACA_Z_SCORE = "MUACA Vxnaid"
    const val CONCEPT_NAME_WEIGHT_FOR_HEIGHT_Z_SCORE = "Weight for Height Vxnaid"
    const val CONCEPT_NAME_IS_OEDEMA_Z_SCORE = "Is Oedema"
    const val CONCEPT_NAME_WEIGHT_KG = "Weight (kg)"

    const val CALL_NAVIGATE_TO_MATCH_SCREEN = "CALL_NAVIGATE_TO_MATCH_SCREEN"
    const val PARTICIPANT_MATCH_ID = "PARTICIPANT_MATCH_ID"
}