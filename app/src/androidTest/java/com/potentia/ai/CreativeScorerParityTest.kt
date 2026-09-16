package com.potentia.ai

import kotlin.math.abs

object CreativeScorerParityTest {

    data class Vector(
        val task: String,
        val text: String,
        val expectedRawScore: Double,
        val expectedUiScore: Double,
        val tolerance: Double
    )

    fun assertVector(
        scorer: CreativeScorer,
        vector: Vector
    ) {
        val actual = scorer.score(
            task = vector.task,
            text = vector.text
        )

        check(
            abs(actual.rawScore - vector.expectedRawScore) <= vector.tolerance
        ) {
            "Raw parity failed for ${vector.task}: ${vector.text}"
        }

        check(
            abs(actual.uiScore - vector.expectedUiScore) <= 1e-4
        ) {
            "UI parity failed for ${vector.task}: ${vector.text}"
        }
    }
}
