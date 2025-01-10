package com.idi.vaccinetracker.common.data.models.api.request

import com.idi.vaccinetracker.common.data.models.LicenseType
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetLicensesRequest(val licenseTypes: List<LicenseType>)