package com.jnj.vaccinetracker.common.validators

import javax.inject.Inject

class NinValidator @Inject constructor() {

    companion object {
         /**
         * The regex ^[A-Z0-9]{14}$ matches exactly 14 alphanumeric characters (letters or digits)
         * with case insensitivity due to RegexOption.IGNORE_CASE 
         * This regex will now allow  both CM00000000FFKH and CM00000000FF6H, 
         */        
        private val NIN_REGEX = Regex("^[A-Z0-9]{14}$", RegexOption.IGNORE_CASE)
    }

    fun validate(valueToValidate: String): Boolean {
        return NIN_REGEX.matches(valueToValidate)
    }
}