package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
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

class QrCodeGeneratorDialog : DialogFragment(R.layout.dialog_qr_code_generator) {

    private var participantIdTextView: TextView? = null
    private val tag = "QrCodeGeneratorDialog"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageViewQrCode = view.findViewById<ImageView>(R.id.imageView_qrCode)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        participantIdTextView = requireActivity().findViewById(R.id.textView_participant_id)

        val uniqueChildId = generateChildId()
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(
                uniqueChildId,
                BarcodeFormat.QR_CODE,
                250,
                250
            )
            imageViewQrCode.setImageBitmap(bitmap)
            participantIdTextView?.text = uniqueChildId
        } catch (e: Exception) {
            Log.e(tag, "Error generating QR code", e)
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun generateChildId(): String {
        val identifierLength = 8
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..identifierLength)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}