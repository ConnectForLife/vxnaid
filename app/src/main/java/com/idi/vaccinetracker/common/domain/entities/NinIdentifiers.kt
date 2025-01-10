package com.idi.vaccinetracker.common.domain.entities

import com.idi.vaccinetracker.common.data.models.api.response.IdentifierDTO
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

typealias NinIdentifiersList = List<IdentifierDTO>

fun Moshi.ninIdentifiersListAdapter(): JsonAdapter<NinIdentifiersList> = adapter(
    Types.newParameterizedType(
        List::class.java, IdentifierDTO::class.java))
