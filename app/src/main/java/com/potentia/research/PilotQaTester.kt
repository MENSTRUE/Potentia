package com.potentia.research

import android.content.Context
import com.potentia.ai.CreativeScorer
import com.potentia.assessment.AssessmentBank
import com.potentia.assessment.AssessmentScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Debug-only smoke-test helper.
 *
 * This creates a fully synthetic 47-item-style session so the developer can
 * verify scoring, CSV export, HTTPS sync, Apps Script, and Google Sheets without
 * manually answering the assessment. The generated record is NEVER written to
 * PilotResearchStorage or AssessmentStorage. PilotSyncClient.uploadQa() marks it
 * as qa_test so the server routes it to QA_* sheets instead of real pilot data.
 */
object PilotQaTester {

    data class RunResult(
        val record: PilotSessionRecord,
        val uploadResult: PilotSyncClient.Result
    )

    suspend fun run(
        context: Context,
        bank: AssessmentBank
    ): RunResult = withContext(Dispatchers.Default) {
        val responses = generateResponses(bank)
        val scorer = CreativeScorer.fromAssets(context.applicationContext)
        val result = AssessmentScoringEngine(scorer).score(
            bank = bank,
            responses = responses
        )

        val record = PilotSessionRecord(
            participantId = "QA-DEBUG",
            sessionId = "QA-${UUID.randomUUID()}",
            consentVersion = "qa-smoke-test-not-participant-data",
            appVersion = appVersionName(context),
            startedAt = result.completedAt - 120_000L,
            completedAt = result.completedAt,
            assessmentVersion = result.assessmentVersion,
            scoringVersion = result.scoringVersion,
            language = bank.language,
            responses = bank.items.map { item ->
                PilotItemResponse(
                    itemId = item.itemId,
                    dimensionId = item.dimensionId,
                    responseType = item.responseType,
                    response = responses.getValue(item.itemId)
                )
            },
            result = result
        )

        val upload = withContext(Dispatchers.IO) {
            PilotSyncClient.uploadQa(record)
        }

        RunResult(record = record, uploadResult = upload)
    }

    internal fun generateResponses(bank: AssessmentBank): Map<String, String> =
        LinkedHashMap<String, String>().apply {
            bank.items.forEachIndexed { index, item ->
                val value = when (item.responseType) {
                    "single_choice", "single_choice_image" ->
                        item.correctValue
                            ?: item.choices.getOrNull(index % item.choices.size.coerceAtLeast(1))?.value
                            ?: "qa_choice"

                    "likert_1_5" -> ((index % 5) + 1).toString()

                    "sjt_single_choice" ->
                        item.choiceScores.maxByOrNull { it.value }?.key
                            ?: item.choices.firstOrNull()?.value
                            ?: "qa_sjt"

                    "free_text" ->
                        "Jawaban sintetis untuk menguji alur sistem dan scoring POTENTIA."

                    "free_text_list" ->
                        "ide uji pertama; ide uji kedua; ide uji ketiga"

                    else -> item.choices.firstOrNull()?.value ?: "QA_TEST"
                }
                put(item.itemId, value)
            }
        }

    private fun appVersionName(context: Context): String = runCatching {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
            .ifBlank { "unknown" }
    }.getOrDefault("unknown")
}
