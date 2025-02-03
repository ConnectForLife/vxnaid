package com.jnj.vaccinetracker.common.validators

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
        return phoneRegex.matches(fullPhoneNumber)
    }
}