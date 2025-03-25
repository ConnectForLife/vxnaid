package com.jnj.vaccinetracker.common.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.jnj.vaccinetracker.R
import java.io.File
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.Q)
class FileUtil {

    companion object {
        fun exportToFile(context: Context, fileName: String, mimeType: String, contentWriter: (OutputStream) -> Unit
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
                        context.grantUriPermission(
                            context.packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        openFile(context, uri, mimeType)
                    } else {
                        Toast.makeText(context, R.string.file_does_not_exist, Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: Exception) {
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
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    setPackage("com.microsoft.office.excel")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                Toast.makeText(context, R.string.file_does_not_exist, Toast.LENGTH_LONG).show()
            }
        }
    }
}