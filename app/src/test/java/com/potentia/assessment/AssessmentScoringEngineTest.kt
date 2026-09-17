package com.potentia.assessment

import com.potentia.ai.CreativeScoreResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentScoringEngineTest {

    private val engine = AssessmentScoringEngine { task, _ ->
        CreativeScoreResult(
            rawScore = if (task == "cre_001") 2.0 else 4.0,
            uiScore = if (task == "cre_001") 50.0 else 100.0,
            outOfDomain = true,
            experimental = true
        )
    }

    @Test
    fun objectiveScoringCoversLogicalVerbalAndSpatial() {
        val bank = bankOf(
            section(
                "logical",
                objective("LOG_001", "logical", correct = "B")
            ),
            section(
                "verbal",
                objective("VER_001", "verbal", correct = "A")
            ),
            section(
                "spatial",
                objective(
                    id = "SPA_001",
                    dimension = "spatial",
                    correct = "D",
                    responseType = "single_choice_image"
                )
            )
        )

        val result = engine.score(
            bank,
            mapOf(
                "LOG_001" to "B",
                "VER_001" to "C",
                "SPA_001" to "D"
            )
        )

        assertEquals(100.0, result.dimensions.getValue("logical").score!!, 1e-9)
        assertEquals(0.0, result.dimensions.getValue("verbal").score!!, 1e-9)
        assertEquals(100.0, result.dimensions.getValue("spatial").score!!, 1e-9)
    }

    @Test
    fun reverseScoredLikertIsAppliedCorrectly() {
        val bank = bankOf(
            section(
                "social",
                AssessmentItem(
                    assessmentVersion = VERSION,
                    itemId = "SOC_004",
                    dimensionId = "social",
                    subconstruct = "test",
                    prompt = "test",
                    responseType = "likert_1_5",
                    pilotOnly = true,
                    active = true,
                    reverseScored = true
                )
            )
        )

        val highResponse = engine.score(bank, mapOf("SOC_004" to "5"))
        val lowResponse = engine.score(bank, mapOf("SOC_004" to "1"))

        assertEquals(0.0, highResponse.dimensions.getValue("social").score!!, 1e-9)
        assertEquals(100.0, lowResponse.dimensions.getValue("social").score!!, 1e-9)
    }

    @Test
    fun socialSjtUsesRelativeChoiceWeight() {
        val bank = bankOf(
            section(
                "social",
                sjt(
                    id = "SOC_007",
                    dimension = "social",
                    scores = mapOf("A" to 0.0, "B" to 1.0, "C" to 2.0)
                )
            )
        )

        val result = engine.score(bank, mapOf("SOC_007" to "B"))

        assertEquals(50.0, result.dimensions.getValue("social").score!!, 1e-9)
    }

    @Test
    fun practicalSjtUsesRelativeChoiceWeight() {
        val bank = bankOf(
            section(
                "practical",
                sjt(
                    id = "PRA_001",
                    dimension = "practical",
                    scores = mapOf("A" to 1.0, "B" to 3.0, "C" to 2.0)
                )
            )
        )

        val result = engine.score(bank, mapOf("PRA_001" to "C"))

        assertEquals((2.0 / 3.0) * 100.0, result.dimensions.getValue("practical").score!!, 1e-9)
    }

    @Test
    fun invalidResponseIsSkippedAndTriggersInsufficientAnswers() {
        val bank = bankOf(
            section(
                "social",
                AssessmentItem(
                    assessmentVersion = VERSION,
                    itemId = "SOC_001",
                    dimensionId = "social",
                    subconstruct = "test",
                    prompt = "test",
                    responseType = "likert_1_5",
                    pilotOnly = true,
                    active = true
                )
            )
        )

        val result = engine.score(bank, mapOf("SOC_001" to "99"))
        val dimension = result.dimensions.getValue("social")

        assertEquals(0, dimension.answered)
        assertNull(dimension.score)
        assertEquals("insufficient_answers", dimension.qualityFlag)
    }

    @Test
    fun exactlySeventyFivePercentAnsweredPassesQualityGate() {
        val items = (1..4).map { index ->
            objective(
                id = "LOG_${index.toString().padStart(3, '0')}",
                dimension = "logical",
                correct = "A"
            )
        }
        val bank = bankOf(section("logical", *items.toTypedArray()))

        val result = engine.score(
            bank,
            mapOf(
                "LOG_001" to "A",
                "LOG_002" to "A",
                "LOG_003" to "B"
            )
        )
        val dimension = result.dimensions.getValue("logical")

        assertEquals(3, dimension.answered)
        assertEquals(0.75, dimension.answeredRatio, 1e-9)
        assertNull(dimension.qualityFlag)
        assertEquals((2.0 / 3.0) * 100.0, dimension.score!!, 1e-9)
    }

    @Test
    fun belowSeventyFivePercentAnsweredReturnsNoDimensionScore() {
        val items = (1..4).map { index ->
            objective(
                id = "VER_${index.toString().padStart(3, '0')}",
                dimension = "verbal",
                correct = "A"
            )
        }
        val bank = bankOf(section("verbal", *items.toTypedArray()))

        val result = engine.score(
            bank,
            mapOf(
                "VER_001" to "A",
                "VER_002" to "A"
            )
        )
        val dimension = result.dimensions.getValue("verbal")

        assertEquals(2, dimension.answered)
        assertEquals(0.5, dimension.answeredRatio, 1e-9)
        assertEquals("insufficient_answers", dimension.qualityFlag)
        assertNull(dimension.score)
    }

    @Test
    fun creativeFreeTextUsesInjectedScorerAndStaysExperimentalOutOfDomain() {
        val bank = bankOf(
            section(
                "creative",
                AssessmentItem(
                    assessmentVersion = VERSION,
                    itemId = "CRE_001",
                    dimensionId = "creative",
                    subconstruct = "originality",
                    prompt = "test",
                    responseType = "free_text",
                    pilotOnly = true,
                    active = true
                )
            )
        )

        val result = engine.score(bank, mapOf("CRE_001" to "jawaban kreatif"))
        val dimension = result.dimensions.getValue("creative")

        assertEquals(50.0, dimension.score!!, 1e-9)
        assertTrue(dimension.experimental)
        assertTrue(dimension.outOfDomain)
    }

    @Test
    fun creativeListAveragesUpToConfiguredMaximumResponses() {
        val calls = mutableListOf<String>()
        val listEngine = AssessmentScoringEngine { _, text ->
            calls += text
            CreativeScoreResult(
                rawScore = when (text) {
                    "satu" -> 1.0
                    "dua" -> 3.0
                    else -> 4.0
                },
                uiScore = 0.0,
                outOfDomain = false
            )
        }
        val bank = bankOf(
            section(
                "creative",
                AssessmentItem(
                    assessmentVersion = VERSION,
                    itemId = "CRE_002",
                    dimensionId = "creative",
                    subconstruct = "fluency",
                    prompt = "test",
                    responseType = "free_text_list",
                    pilotOnly = true,
                    active = true,
                    maxResponses = 2
                )
            )
        )

        val result = listEngine.score(
            bank,
            mapOf("CRE_002" to "satu\ndua\ntiga")
        )

        assertEquals(listOf("satu", "dua"), calls)
        assertEquals(50.0, result.dimensions.getValue("creative").score!!, 1e-9)
        assertFalse(result.dimensions.getValue("creative").outOfDomain)
    }

    private fun bankOf(vararg sections: AssessmentSection): AssessmentBank =
        AssessmentBank(
            assessmentVersion = VERSION,
            scoringVersion = "test-scoring-v1",
            language = "id",
            status = "PILOT_ONLY",
            productionValidated = false,
            totalItems = sections.sumOf { it.items.size },
            sections = sections.toList()
        )

    private fun section(
        id: String,
        vararg items: AssessmentItem
    ): AssessmentSection = AssessmentSection(
        id = id,
        title = id,
        instruction = "",
        items = items.toList()
    )

    private fun objective(
        id: String,
        dimension: String,
        correct: String,
        responseType: String = "single_choice"
    ): AssessmentItem = AssessmentItem(
        assessmentVersion = VERSION,
        itemId = id,
        dimensionId = dimension,
        subconstruct = "test",
        prompt = "test",
        responseType = responseType,
        pilotOnly = true,
        active = true,
        correctValue = correct
    )

    private fun sjt(
        id: String,
        dimension: String,
        scores: Map<String, Double>
    ): AssessmentItem = AssessmentItem(
        assessmentVersion = VERSION,
        itemId = id,
        dimensionId = dimension,
        subconstruct = "test",
        prompt = "test",
        responseType = "sjt_single_choice",
        pilotOnly = true,
        active = true,
        choiceScores = scores
    )

    private companion object {
        const val VERSION = "test-v1"
    }
}
