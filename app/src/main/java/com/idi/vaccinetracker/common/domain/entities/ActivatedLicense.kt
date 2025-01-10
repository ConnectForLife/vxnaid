package com.idi.vaccinetracker.common.domain.entities

import com.idi.vaccinetracker.common.data.models.LicenseType

data class ActivatedLicense(val licenseType: LicenseType, val activatedLicense: String)