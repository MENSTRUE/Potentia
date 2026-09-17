package com.potentia.growth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthCatalogTest {

    private val dimensions = listOf(
        "logical", "creative", "verbal", "spatial", "social", "practical"
    )

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
    fun exerciseIdsAreUniqueAndStepsAreActionable() {
        val all = dimensions.flatMap(GrowthCatalog::forDimension)

        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.steps.size >= 3 })
        assertTrue(all.all { it.title.isNotBlank() && it.summary.isNotBlank() })
        assertTrue(all.all { it.estimatedMinutes in 5..15 })
    }
}
