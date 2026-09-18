package com.potentia.growth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthCatalogTest {

    private val dimensions = listOf(
        "logical", "creative", "verbal", "spatial", "social", "practical"
    )

    private val allExercises
        get() = dimensions.flatMap(GrowthCatalog::forDimension)

    @Test
    fun everyDimensionHasThreeExercises() {
        dimensions.forEach { dimension ->
            assertEquals(
                "Unexpected exercise count for $dimension",
                3,
                GrowthCatalog.forDimension(dimension).size
            )
        }
    }

    @Test
    fun exerciseIdsAreUniqueAndContentIsActionable() {
        val all = allExercises

        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.steps.size >= 3 })
        assertTrue(all.all { it.title.isNotBlank() && it.summary.isNotBlank() })
        assertTrue(all.all { it.estimatedMinutes in 5..15 })
    }

    @Test
    fun eachExerciseHasValidModeSpecificPayload() {
        allExercises.forEach { exercise ->
            when (exercise.mode) {
                GrowthExerciseMode.GUIDED_REFLECTION -> {
                    assertTrue(
                        "${exercise.id} must have reflection prompts",
                        exercise.reflectionPrompts.size >= 2
                    )
                    assertTrue(exercise.reflectionPrompts.all { it.id.isNotBlank() })
                    assertTrue(exercise.reflectionPrompts.all { it.label.isNotBlank() })
                    assertTrue(exercise.reflectionPrompts.all { it.minChars >= 3 })
                    assertTrue(exercise.challenge == null)
                }

                GrowthExerciseMode.INTERACTIVE_CHALLENGE -> {
                    val challenge = requireNotNull(exercise.challenge)
                    assertTrue(challenge.choices.size >= 2)
                    assertTrue(
                        challenge.choices.any { it.value == challenge.correctValue }
                    )
                    assertTrue(challenge.successExplanation.isNotBlank())
                    assertTrue(challenge.retryHint.isNotBlank())
                    assertTrue(exercise.reflectionPrompts.isEmpty())
                }
            }
        }
    }

    @Test
    fun catalogContainsBothGuidedReflectionAndVerifiedChallenges() {
        val modes = allExercises.map { it.mode }.toSet()

        assertTrue(GrowthExerciseMode.GUIDED_REFLECTION in modes)
        assertTrue(GrowthExerciseMode.INTERACTIVE_CHALLENGE in modes)
        assertTrue(
            allExercises.count { it.mode == GrowthExerciseMode.INTERACTIVE_CHALLENGE } >= 4
        )
        assertFalse(allExercises.isEmpty())
    }
}
