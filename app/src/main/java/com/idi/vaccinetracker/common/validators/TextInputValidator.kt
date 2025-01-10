package com.idi.vaccinetracker.common.validators

import javax.inject.Inject

class TextInputValidator @Inject constructor() {

    companion object {
        private val ONLY_LETTERS_WITH_SPACES_REGEX = Regex("^[a-zA-Z ]+$")
    }

    fun validate(valueToValidate: String): Boolean {
        return valueToValidate.matches(ONLY_LETTERS_WITH_SPACES_REGEX)
    }
}