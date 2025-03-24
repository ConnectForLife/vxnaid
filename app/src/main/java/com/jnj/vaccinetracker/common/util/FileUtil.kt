package com.jnj.vaccinetracker.common.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.jnj.vaccinetracker.R
import java.io.File
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.Q)
class FileUtil {

    companion object {
        fun exportToFile(
            context: Context,
            fileName: String,
            mimeType: String,
            contentWriter: (OutputStream) -> Unit
        ) {
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        contentWriter(outputStream)
                        outputStream.flush()
                    }

                    // Verify file exists
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    if (file.exists()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.visits_overview_saving_file_success_message),
                            Toast.LENGTH_LONG
                        ).show()

                        // Grant persistent read permission
                        context.grantUriPermission(
                            context.packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        // Use the original MediaStore URI
                        openFile(context, uri, mimeType)
                    } else {
                        Toast.makeText(context, "File saved but not found", Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: Exception) {
                    Log.e("ExportFile", "Error saving file", e)
                    Toast.makeText(
                        context,
                        context.getString(R.string.visits_overview_saving_file_failure_message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.visits_overview_saving_file_failure_message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        private fun openFile(context: Context, uri: Uri, mimeType: String) {
            try {
                var intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    setPackage("com.microsoft.office.excel")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(context.packageManager) == null) {
                    intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        setPackage("com.google.android.apps.docs")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Open with"
                    )
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}