package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.zxing.BarcodeFormat
import com.jnj.vaccinetracker.R
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlin.random.Random

class QrCodeGeneratorDialog(private val participantId: String) : DialogFragment(R.layout.dialog_qr_code_generator) {
    private val tag = "QrCodeGeneratorDialog"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageViewQrCode = view.findViewById<ImageView>(R.id.imageView_qrCode)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(
                participantId,
                BarcodeFormat.QR_CODE,
                250,
                250
            )
            imageViewQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(tag, "Error generating QR code", e)
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

}
