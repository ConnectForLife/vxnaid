package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.zxing.BarcodeFormat
import com.jnj.vaccinetracker.R
import com.journeyapps.barcodescanner.BarcodeEncoder

class QrCodeGeneratorDialog : DialogFragment(R.layout.dialog_qr_code_generator) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputText = view.findViewById<EditText>(R.id.input_text)
        val btnGenerateQrCode = view.findViewById<Button>(R.id.btnGenerateQrCode)
        val imageViewQrCode = view.findViewById<ImageView>(R.id.imageView_qrCode)

        btnGenerateQrCode.setOnClickListener {
            val text = inputText.text.toString().trim()

            if (text.isNotEmpty()) {
                try {
                    val barcodeEncoder = BarcodeEncoder()
                    val bitmap = barcodeEncoder.encodeBitmap(
                        text,
                        BarcodeFormat.QR_CODE,
                        250,
                        250
                    )
                    imageViewQrCode.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}