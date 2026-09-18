package com.potentia.research

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.potentia.assessment.AssessmentResult
import com.potentia.assessment.DimensionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PilotResearchStorageInstrumentedTest {

    @Test
    fun consentAndPilotSessionRoundTripPreservesAnonymousRawResponses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(
            "potentia_test_pilot_research",
            Context.MODE_PRIVATE
        )
        prefs.edit().clear().commit()

        val consent = PilotResearchStorage.optIn(prefs)
        assertTrue(consent.researchEnabled)
        assertTrue(consent.participantId?.startsWith("P-") == true)

        val result = AssessmentResult(
            completedAt = 2000L,
            assessmentVersion = "pilot-v1",
            scoringVersion = "score-v1",
            dimensions = mapOf(
                "creative" to DimensionResult(
                    id = "creative",
                    score = 42.0,
                    answered = 1,
                    expected = 1,
                    answeredRatio = 1.0,
                    experimental = true,
                    outOfDomain = true
                )
            )
        )

        val record = PilotSessionRecord(
            participantId = requireNotNull(consent.participantId),
            sessionId = "S-test-session",
            consentVersion = PilotResearchStorage.CURRENT_CONSENT_VERSION,
            appVersion = "test",
            startedAt = 1000L,
            completedAt = 2000L,
            assessmentVersion = "pilot-v1",
            scoringVersion = "score-v1",
            language = "id",
            responses = listOf(
                PilotItemResponse(
                    itemId = "CRE_001",
                    dimensionId = "creative",
                    responseType = "free_text",
                    response = "jawaban bebas pilot"
                )
            ),
            result = result
        )

        assertTrue(PilotResearchStorage.upsertSession(prefs, record))
        assertTrue(PilotResearchStorage.upsertSession(prefs, record))
        val loaded = PilotResearchStorage.loadSessions(prefs)

        // Upsert by sessionId keeps scoring retries idempotent.
        assertEquals(1, loaded.size)
        assertEquals(record, loaded.single())
        assertTrue(
            PilotResearchStorage.exportJson(consent, loaded)
                .contains("jawaban bebas pilot")
        )
        assertTrue(
            PilotResearchStorage.exportLongCsv(loaded)
                .contains("CRE_001")
        )

        val personal = PilotResearchStorage.choosePersonalOnly(prefs)
        assertFalse(personal.researchEnabled)
        assertEquals(1, PilotResearchStorage.loadSessions(prefs).size)

        assertTrue(PilotResearchStorage.clearPilotDataAndWithdraw(prefs))
        assertTrue(PilotResearchStorage.loadSessions(prefs).isEmpty())
        assertEquals(null, PilotResearchStorage.readConsentState(prefs).participantId)
    }
}
