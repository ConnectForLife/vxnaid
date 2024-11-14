package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.zxing.BarcodeFormat
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsViewModel
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlin.random.Random

class QrCodeGeneratorDialog(private val textViewParticipantId: TextView) : DialogFragment(R.layout.dialog_qr_code_generator) {
    private val tag = "QrCodeGeneratorDialog"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageViewQrCode = view.findViewById<ImageView>(R.id.imageView_qrCode)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        // Generate a unique child ID
        val uniqueChildId = generateChildId()

        try {
            // Generate the QR code
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(
                uniqueChildId,
                BarcodeFormat.QR_CODE,
                250,
                250
            )
            imageViewQrCode.setImageBitmap(bitmap)
            textViewParticipantId.text = uniqueChildId
            // Set the generated ID in the ViewModel
        //    viewModel.setParticipantId(uniqueChildId)

        } catch (e: Exception) {
            Log.e(tag, "Error generating QR code", e)
        }

        // Cancel button dismisses the dialog
        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    // Helper function to generate a random child ID
    private fun generateChildId(): String {
        val identifierLength = 8
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..identifierLength)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
