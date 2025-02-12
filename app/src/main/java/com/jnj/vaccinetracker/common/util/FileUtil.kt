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
        fun exportToFile(context: Context, fileName: String, mimeType: String, contentWriter: (OutputStream) -> Unit) {
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
                    }
                    Toast.makeText(context, context.getString(R.string.visits_overview_saving_file_success_message), Toast.LENGTH_LONG).show()
                    openFile(context, fileName)
                } catch (e: Exception) {
                    Log.e("ExportFile", "Error saving file", e)
                    Toast.makeText(context, context.getString(R.string.visits_overview_saving_file_failure_message), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.visits_overview_saving_file_failure_message), Toast.LENGTH_LONG).show()
            }
        }

        fun openFile(context: Context, fileName: String) {
            val file = File(context.getExternalFilesDir(null), fileName)
            if (file.exists()) {
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.file provider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, getMimeType(fileName) ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, context.getString(R.string.visits_overview_no_available_app_message), Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e("OpenFile", "File does not exist: ${file.absolutePath}")
                Toast.makeText(context, context.getString(R.string.visits_overview_failed_to_open_file_message), Toast.LENGTH_LONG).show()
            }
        }

        private fun getMimeType(fileName: String): String? {
            return when {
                fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
                fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                else -> "*/*"
            }
        }
    }
}
