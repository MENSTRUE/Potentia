package com.potentia.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreativeScorerParityTest {

    @Test
    fun allExportedParityVectorsMatchAndroidScorer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val testContext = instrumentation.context
        val scorer = CreativeScorer.fromAssets(targetContext)
        val raw = testContext.assets
            .open("potentia_ai/parity_vectors.json")
            .bufferedReader()
            .use { it.readText() }
        val vectors = JSONArray(raw)

        assertEquals(25, vectors.length())

        for (index in 0 until vectors.length()) {
            val vector = vectors.getJSONObject(index)
            val task = vector.getString("task")
            val text = vector.getString("text")
            val expectedRaw = vector.getDouble("expected_raw_score")
            val expectedUi = vector.getDouble("expected_ui_score")
            val tolerance = vector.optDouble("tolerance", 1e-6)

            val actual = scorer.score(task = task, text = text)

            assertEquals("raw parity #$index ($task)", expectedRaw, actual.rawScore, tolerance)
            assertEquals("ui parity #$index ($task)", expectedUi, actual.uiScore, 1e-4)
            assertFalse("training task should be in-domain: $task", actual.outOfDomain)
            assertTrue(actual.experimental)
        }
    }
}
