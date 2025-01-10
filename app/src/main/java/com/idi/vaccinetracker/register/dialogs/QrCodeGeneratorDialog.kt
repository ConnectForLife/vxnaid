package com.idi.vaccinetracker.register.dialogs

import android.os.Bundle
import android.util.Log
import android.app.Dialog
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.zxing.BarcodeFormat
import com.idi.vaccinetracker.R
import com.journeyapps.barcodescanner.BarcodeEncoder

class QrCodeGeneratorDialog(
    private val participantId: String
) : DialogFragment() {

    private lateinit var imageViewQrCode: ImageView
    private lateinit var btnCancel: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_qr_code_generator)

        initializeViews(dialog)
        generateQrCode()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun initializeViews(dialog: Dialog) {
        imageViewQrCode = dialog.findViewById(R.id.imageView_qrCode)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
    }

    private fun generateQrCode() {
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
            Log.e("QrCodeGeneratorDialog", "Error generating QR code", e)
        }
    }
}

