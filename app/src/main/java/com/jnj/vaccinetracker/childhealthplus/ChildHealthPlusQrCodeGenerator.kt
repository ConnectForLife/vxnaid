package com.jnj.vaccinetracker.childhealthplus

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

object ChildHealthPlusQrCodeGenerator {
    fun generate(content: String, size: Int = 250): Bitmap? {
        return try {
            BarcodeEncoder().encodeBitmap(content, BarcodeFormat.QR_CODE, size, size)
        } catch (_: Exception) {
            null
        }
    }
}

