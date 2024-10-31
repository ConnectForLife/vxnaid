package com.jnj.vaccinetracker.visitsoverview.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class VisitDetailsDTO(
    val formattedVisitDate : String,
    val vaccines: String,
    val phoneNumber: String,
    val clientID: String,
    val clientFullName: String
) : Parcelable