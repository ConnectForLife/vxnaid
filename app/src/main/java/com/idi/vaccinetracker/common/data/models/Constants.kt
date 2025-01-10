package com.idi.vaccinetracker.common.data.models

import com.idi.vaccinetracker.sync.data.models.VisitType

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
    const val ATTRIBUTE_MOTHER_FIRST_NAME= "Mother's first name"
    const val ATTRIBUTE_MOTHER_LAST_NAME= "Mother's last name"
    const val ATTRIBUTE_FATHER_FIRST_NAME= "Father's first name"
    const val ATTRIBUTE_FATHER_LAST_NAME= "Father's last name"

    const val ATTRIBUTE_CHILD_CATEGORY= "Child category"
    const val NIN_IDENTIFIER_TYPE_NAME = "National ID"

    // Visit
    const val ATTRIBUTE_VISIT_STATUS = "Visit Status"
    const val ATTRIBUTE_VISIT_DAYS_AFTER = "Up Window"
    const val ATTRIBUTE_VISIT_DAYS_BEFORE = "Low Window"
    const val ATTRIBUTE_VISIT_DOSE_NUMBER = "Dose number"
    const val ATTRIBUTE_VISIT_TYPE_VXNAID = "Visit type Vxnaid"
    const val ATTRIBUTE_VISIT_LOCATION = "Visit Location"
    const val VISIT_TYPE_DOSING = "Dosing"
    const val VISIT_TYPE_OTHER = "Other"
    const val VISIT_TYPE_ADVERSE_EFFECTS = "Adverse Effects"
    const val VISIT_STATUS_OCCURRED = "OCCURRED"
    const val VISIT_STATUS_MISSED = "MISSED"
    const val VISIT_STATUS_SCHEDULED = "SCHEDULED"
    const val OBSERVATION_TYPE_MANUFACTURER = "Vaccine Manufacturer"
    const val RESCHEDULE_VISIT_REASON_ATTRIBUTE_TYPE_NAME = "Reschedule Visit Reason"
    const val ADVERSE_EFFECTS_OBSERVATION = "Adverse Effects Vxnaid"

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
    const val REQ_VISITS_OVERVIEW = 312
    const val REQ_VACCINES_OVERVIEW = 412

    const val UTC_TIME_ZONE_NAME = "UTC"

    const val BARCODE_STR = "Barcode"
    const val MANUFACTURER_NAME_STR = "Manufacturer"
    const val DATE_STR = "Date"
    const val SPACE_STR = " "
    const val VXNAID_DATE_SUFFIX = "Vxnaid $DATE_STR"

    const val REFERRAL_CLINIC_CONCEPT_NAME = "Referral Clinic Vxnaid"
    const val REFERRAL_ADDITIONAL_INFO_CONCEPT_NAME = "Referral Additional Info Vxnaid"

    const val SUBSTANCES_AND_DATES_STR = "substancesAndDates"
    const val OTHER_SUBSTANCES_AND_VALUES_STR = "otherSubstancesAndValues"

    const val CONCEPT_NAME_WEIGHT_FOR_AGE_Z_SCORE = "Weight for age Z score"
    const val CONCEPT_NAME_HEIGHT_FOR_AGE_Z_SCORE = "Height for age Z score Vxnaid"
    const val CONCEPT_NAME_MUACA_Z_SCORE = "MUACA Vxnaid"
    const val CONCEPT_NAME_WEIGHT_FOR_HEIGHT_Z_SCORE = "Weight for Height Vxnaid"
    const val CONCEPT_NAME_Z_SCORE = "Z-score Vxnaid"
    const val CONCEPT_NAME_IS_OEDEMA_Z_SCORE = "Is Oedema"
    const val CONCEPT_NAME_WEIGHT_KG = "Weight (kg)"
    const val CONCEPT_NAME_RECEIVED_LLIN = "Received LLIN Vxnaid"

    const val CALL_NAVIGATE_TO_MATCH_SCREEN = "CALL_NAVIGATE_TO_MATCH_SCREEN"
    const val PARTICIPANT_MATCH_ID = "PARTICIPANT_MATCH_ID"

    const val VISIT_PLACE_STATIC = "Static"
    const val VISIT_PLACE_OUTREACH = "Outreach"
    const val VISIT_PLACE_SCHOOL = "School"

    const val USER_PREFERENCES_FILE_NAME = "user_preferences"
    const val VISIT_PLACE_FILE_KEY = "visit_place"

    const val CHILD_CATEGORY_NATIONAL = "National"
    const val CHILD_CATEGORY_FOREIGNER = "Foreigner"
    const val CHILD_CATEGORY_REFUGEE = "Refugee"

    const val VISITS_OVERVIEW_SCHEDULED_VISITS_KEY = "Scheduled Visits"
    const val VISITS_OVERVIEW_HISTORICAL_VISITS_KEY = "Historical Visits"
    const val VISITS_OVERVIEW_MISSED_VISITS_KEY = "Missed Visits"

    const val VISIT_DATE_FILE_COLUMN_HEADER = "Visit Date"
    const val CLIENT_ID_FILE_COLUMN_HEADER = "Client ID"
    const val CLIENT_NAME_FILE_COLUMN_HEADER = "Client Name"
    const val PHONE_NUMBER_FILE_COLUMN_HEADER = "Phone Number"
    const val CLIENT_MOTHER_NAME_FILE_HEADER = "Mother Name"

    const val VACCINES_CATEGORY_NAME = "Immunization"
    const val GROUP_AGE_FIRST = "0-11 months"
    const val GROUP_AGE_SECOND = "12-59 months"
    const val GROUP_AGE_THIRD = "5-14 years"
    const val GROUP_AGE_FOURTH = "14+ years"

    const val EMPTY_STRING_VALUE = ""
    const val ALL_STRING = "All"
    const val YES_ANSWER = "Yes"

    const val VXNAID_DATE_CONCEPT_NAME_SUFFIX = "Vxnaid Date"
    const val NOT_NEEDED_STRING_VALUE = "notNeeded"

    const val AT_BIRTH_VISIT_TYPE = "At Birth"
    const val SIX_WEEKS_VISIT_TYPE = "6 weeks"
    const val HEP_B_BD_VACCINE_CONCEPT_NAME = "Hep B BD Vxnaid"
    const val POLIO_0_VACCINE_CONCEPT_NAME = "Polio 0 Vxnaid"
}