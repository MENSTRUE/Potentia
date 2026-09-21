package com.potentia.research

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.potentia.assessment.AssessmentResult
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

    fun shareSessionCsv(
        context: Context,
        session: PilotSessionRecord
    ) {
        shareFile(
            context = context,
            filename = "potentia_pilot_session_${sessionFileToken(session.sessionId)}_${timestamp()}.csv",
            mimeType = "text/csv",
            payload = PilotResearchStorage.exportLongCsv(listOf(session))
        )
    }


    /**
     * Export ringkasan hasil asesmen yang tersimpan di riwayat lokal.
     * Ini bekerja untuk Mode Pribadi maupun Pilot Pseudonim karena hanya
     * memakai AssessmentResult (6 hasil dimensi), bukan respons mentah 47 item.
     */
    fun shareAssessmentHistoryCsv(
        context: Context,
        results: List<AssessmentResult>
    ) {
        if (results.isEmpty()) return
        shareFile(
            context = context,
            filename = "potentia_assessment_history_${timestamp()}.csv",
            mimeType = "text/csv",
            payload = assessmentHistoryCsv(results),
            subject = "Riwayat hasil asesmen POTENTIA",
            chooserTitle = "Bagikan hasil asesmen POTENTIA"
        )
    }

    fun shareAssessmentResultCsv(
        context: Context,
        result: AssessmentResult
    ) {
        shareFile(
            context = context,
            filename = "potentia_assessment_result_${result.completedAt}_${timestamp()}.csv",
            mimeType = "text/csv",
            payload = assessmentHistoryCsv(listOf(result)),
            subject = "Hasil asesmen POTENTIA",
            chooserTitle = "Bagikan hasil asesmen POTENTIA"
        )
    }

    private fun assessmentHistoryCsv(results: List<AssessmentResult>): String {
        val dimensionOrder = listOf("logical", "creative", "verbal", "spatial", "social", "practical")
        val header = listOf(
            "completed_at",
            "assessment_version",
            "scoring_version",
            "dimension_id",
            "score",
            "answered",
            "expected",
            "answered_ratio",
            "quality_flag",
            "experimental",
            "out_of_domain"
        ).joinToString(",")

        val rows = results
            .sortedByDescending { it.completedAt }
            .flatMap { result ->
                dimensionOrder.map { dimensionId ->
                    val dimension = result.dimensions[dimensionId]
                    listOf(
                        isoTimestamp(result.completedAt),
                        result.assessmentVersion,
                        result.scoringVersion,
                        dimensionId,
                        dimension?.score?.toString().orEmpty(),
                        dimension?.answered?.toString().orEmpty(),
                        dimension?.expected?.toString().orEmpty(),
                        dimension?.answeredRatio?.toString().orEmpty(),
                        dimension?.qualityFlag.orEmpty(),
                        dimension?.experimental?.toString().orEmpty(),
                        dimension?.outOfDomain?.toString().orEmpty()
                    ).joinToString(",") { csv(it) }
                }
            }

        return buildString {
            appendLine(header)
            rows.forEach(::appendLine)
        }
    }

    private fun csv(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    private fun isoTimestamp(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date(epochMillis))

    private fun shareFile(
        context: Context,
        filename: String,
        mimeType: String,
        payload: String,
        subject: String = "Data pilot POTENTIA",
        chooserTitle: String = "Ekspor data pilot POTENTIA"
    ) {
        val exportDir = File(context.cacheDir, "exports").apply {
            mkdirs()
            // Pilot exports may contain free-text responses. Keep only the newest
            // generated export in app cache to reduce residual local copies.
            listFiles()?.forEach { oldFile ->
                runCatching { oldFile.delete() }
            }
        }
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
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun sessionFileToken(sessionId: String): String =
        sessionId
            .removePrefix("S-")
            .replace(Regex("[^A-Za-z0-9_-]"), "")
            .take(12)
            .ifBlank { "session" }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
