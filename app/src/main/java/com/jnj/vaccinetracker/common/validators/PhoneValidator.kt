package com.jnj.vaccinetracker.common.validators

import com.jnj.vaccinetracker.common.helpers.logWarn
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import javax.inject.Inject

class PhoneValidator @Inject constructor(private val phoneNumberUtil: PhoneNumberUtil) {

    companion object {
        /**
         * only numbers and white spaces
         */
        private val phoneRegex = "^256\\d{9}\$".toRegex()
    }

    fun validate(fullPhoneNumber: String): Boolean {
        if (!phoneRegex.matches(fullPhoneNumber)) {
            logWarn("$fullPhoneNumber not matching with $phoneRegex")
            return false
        }
       return true
    }
}