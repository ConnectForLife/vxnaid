package com.jnj.vaccinetracker.register.dialogs

import android.content.ContentValues.TAG
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.util.Log
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

        val inputText = view.findViewById<EditText>(R.id.input_text)
        val btnGenerateQrCode = view.findViewById<Button>(R.id.btnGenerateQrCode)
        val imageViewQrCode = view.findViewById<ImageView>(R.id.imageView_qrCode)
        val btnOk = view.findViewById<Button>(R.id.btn_ok)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        participantIdTextView = requireActivity().findViewById(R.id.textView_participant_id)

        btnGenerateQrCode.setOnClickListener {
            val uniqueChildId = generateChildId()
            inputText.setText(uniqueChildId)

            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(
                    uniqueChildId,
                    BarcodeFormat.QR_CODE,
                    250,
                    250
                )
                imageViewQrCode.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating QR code", e)
            }
        }

        btnOk.setOnClickListener {
            participantIdTextView?.text = inputText.text.toString()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun generateChildId(): String {
        val identifierLength = 8
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..identifierLength)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}