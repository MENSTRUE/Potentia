package com.potentia.research

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PilotExportShare {

    fun shareJson(
        context: Context,
        consentState: PilotConsentState,
        sessions: List<PilotSessionRecord>
    ) {
        shareFile(
            context = context,
            filename = "potentia_pilot_${timestamp()}.json",
            mimeType = "application/json",
            payload = PilotResearchStorage.exportJson(consentState, sessions)
        )
    }

    fun shareCsv(
        context: Context,
        sessions: List<PilotSessionRecord>
    ) {
        shareFile(
            context = context,
            filename = "potentia_pilot_responses_${timestamp()}.csv",
            mimeType = "text/csv",
            payload = PilotResearchStorage.exportLongCsv(sessions)
        )
    }

    private fun shareFile(
        context: Context,
        filename: String,
        mimeType: String,
        payload: String
    ) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, filename).apply {
            writeText(payload, Charsets.UTF_8)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "Data pilot POTENTIA")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ekspor data pilot POTENTIA"))
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
