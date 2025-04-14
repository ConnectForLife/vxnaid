package com.jnj.vaccinetracker.common.domain.entities

import android.os.Parcelable
import androidx.annotation.StringRes
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.Gender.values
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Gender(@StringRes val translation: Int) : Parcelable {
    @field:Json(name = "O")
    OTHERS(R.string.gender_others),

    @field:Json(name = "M")
    MALE(R.string.gender_male),

    @field:Json(name = "F")
    FEMALE(R.string.gender_female);

    val code: String
        get() = when (this) {
            OTHERS -> "O"
            MALE -> "M"
            FEMALE -> "F"
        }

    companion object {
        fun fromCode(code: String) = values().firstOrNull { it.code == code }
    }
}
