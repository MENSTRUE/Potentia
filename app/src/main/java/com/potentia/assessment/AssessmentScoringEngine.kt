package com.potentia.assessment

import com.potentia.ai.CreativeScorer

class AssessmentScoringEngine(
    private val creativeScorer: CreativeScorer
) {
    fun score(
        bank: AssessmentBank,
        responses: Map<String, String>
    ): AssessmentResult {
        val itemScores = mutableMapOf<String, MutableList<Double>>()
        val creativeOutOfDomain = mutableListOf<Boolean>()

        bank.items.forEach { item ->
            val raw = responses[item.itemId]?.trim().orEmpty()
            if (raw.isBlank()) return@forEach

            val normalized = when (item.responseType) {
                "single_choice", "single_choice_image" -> {
                    if (raw == item.correctValue) 1.0 else 0.0
                }

                "likert_1_5" -> {
                    val value = raw.toDoubleOrNull() ?: return@forEach
                    if (value !in 1.0..5.0) return@forEach
                    val scoredValue = if (item.reverseScored) 6.0 - value else value
                    (scoredValue - 1.0) / 4.0
                }

                "sjt_single_choice" -> {
                    val rawScore = item.choiceScores[raw] ?: return@forEach
                    val maxScore = item.choiceScores.values.maxOrNull() ?: return@forEach
                    if (maxScore <= 0.0) return@forEach
                    rawScore / maxScore
                }

                "free_text", "free_text_list" -> {
                    val creative = scoreCreative(item, raw)
                    creativeOutOfDomain += creative.second
                    creative.first
                }

                else -> return@forEach
            }

            itemScores.getOrPut(item.dimensionId) { mutableListOf() }
                .add(normalized.coerceIn(0.0, 1.0))
        }

        val dimensions = LinkedHashMap<String, DimensionResult>()

        bank.sections.forEach { section ->
            val expected = section.items.count { it.active }
            val scores = itemScores[section.id].orEmpty()
            val answered = scores.size
            val answeredRatio = if (expected > 0) answered.toDouble() / expected else 0.0
            val qualityFlag = if (answeredRatio < 0.75) "insufficient_answers" else null
            val score = if (qualityFlag == null && scores.isNotEmpty()) {
                scores.average() * 100.0
            } else {
                null
            }

            dimensions[section.id] = DimensionResult(
                id = section.id,
                score = score,
                answered = answered,
                expected = expected,
                answeredRatio = answeredRatio,
                qualityFlag = qualityFlag,
                experimental = section.id == "creative",
                outOfDomain = section.id == "creative" && creativeOutOfDomain.any { it }
            )
        }

        return AssessmentResult(
            completedAt = System.currentTimeMillis(),
            assessmentVersion = bank.assessmentVersion,
            scoringVersion = bank.scoringVersion,
            dimensions = dimensions
        )
    }

    private fun scoreCreative(
        item: AssessmentItem,
        raw: String
    ): Pair<Double, Boolean> {
        val pieces = if (item.responseType == "free_text_list") {
            raw.split(Regex("[\\n;]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(item.maxResponses ?: 3)
        } else {
            listOf(raw.trim()).filter { it.isNotBlank() }
        }

        if (pieces.isEmpty()) return 0.0 to true

        val results = pieces.map { answer ->
            creativeScorer.score(
                task = item.itemId.lowercase(),
                text = answer
            )
        }

        val rawAverage = results.map { it.rawScore }.average()
        val normalized = (rawAverage / 4.0).coerceIn(0.0, 1.0)
        return normalized to results.any { it.outOfDomain }
    }
}
