package com.idi.vaccinetracker.visitsoverview.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class VisitDetailsDTO(
    val formattedVisitDate : String,
    val visitType: String,
    val phoneNumber: String,
    val clientID: String,
    val clientFullName: String,
    val clientMotherName: String
) : Parcelable