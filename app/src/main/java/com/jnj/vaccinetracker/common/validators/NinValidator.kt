package com.jnj.vaccinetracker.common.validators

import javax.inject.Inject

class NinValidator @Inject constructor() {

    companion object {
        private val NIN_REGEX = Regex("^[A-Z0-9]{14}$", RegexOption.IGNORE_CASE)

        //  private val NIN_REGEX = Regex("^[a-zA-Z][MF]\\d{2}[a-zA-Z\\d]{7}[a-zA-Z]{3}$")
    }

    fun validate(valueToValidate: String): Boolean {
        return NIN_REGEX.matches(valueToValidate)
    }
}