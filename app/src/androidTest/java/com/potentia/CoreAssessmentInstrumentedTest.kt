package com.potentia

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.potentia.ai.CreativeScorer
import com.potentia.assessment.AssessmentDraft
import com.potentia.assessment.AssessmentDraftStorage
import com.potentia.assessment.AssessmentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreAssessmentInstrumentedTest {

    @Test
    fun itemBankLoadsWithExpectedV41Structure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bank = AssessmentRepository.load(context)

        assertEquals(47, bank.totalItems)
        assertEquals(47, bank.items.size)
        assertFalse(bank.productionValidated)
        assertEquals(
            listOf("logical", "creative", "verbal", "spatial", "social", "practical"),
            bank.sections.map { it.id }
        )
        assertEquals(listOf(10, 3, 8, 8, 10, 8), bank.sections.map { it.items.size })
    }

    @Test
    fun creativeScorerMatchesKnownAndroidParityVector() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scorer = CreativeScorer.fromAssets(context)

        val result = scorer.score(
            task = "bowl",
            text = "put salads in it"
        )

        assertEquals(1.0771679735257573, result.rawScore, 1e-6)
        assertEquals(26.929199338143935, result.uiScore, 1e-4)
        assertFalse(result.outOfDomain)
        assertTrue(result.experimental)
    }

    @Test
    fun draftStorageRoundTripsWithoutLosingResponses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(
            "potentia_test_assessment_draft",
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().clear().commit()

        val draft = AssessmentDraft(
            assessmentVersion = "potentia-pilot-v1.1",
            totalItems = 47,
            startedAt = 123456789L,
            currentQuestionIndex = 12,
            responses = mapOf(
                "LOG_001" to "B",
                "CRE_001" to "contoh jawaban kreatif"
            ),
            sessionId = "S-test-draft",
            researchEligibleAtStart = true,
            researchConsentVersionAtStart = "potentia-pilot-consent-v1"
        )

        assertTrue(AssessmentDraftStorage.save(prefs, draft, synchronous = true))
        val loaded = AssessmentDraftStorage.read(prefs)

        assertTrue(loaded is AssessmentDraftStorage.ReadResult.Success)
        loaded as AssessmentDraftStorage.ReadResult.Success
        assertEquals(draft, loaded.draft)

        assertTrue(AssessmentDraftStorage.clear(prefs, synchronous = true))
        assertTrue(AssessmentDraftStorage.read(prefs) is AssessmentDraftStorage.ReadResult.None)
    }
    @Test
    fun legacyDraftSchemaOneStillLoadsAsPersonalSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(
            "potentia_test_legacy_assessment_draft",
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().clear().commit()
        prefs.edit().putString(
            "assessment_draft_v1",
            """{
              "schemaVersion":1,
              "assessmentVersion":"potentia-pilot-v1.1",
              "totalItems":47,
              "startedAt":123456789,
              "currentQuestionIndex":3,
              "responses":{"LOG_001":"B"}
            }""".trimIndent()
        ).commit()

        val loaded = AssessmentDraftStorage.read(prefs)
        assertTrue(loaded is AssessmentDraftStorage.ReadResult.Success)
        loaded as AssessmentDraftStorage.ReadResult.Success
        assertFalse(loaded.draft.researchEligibleAtStart)
        assertTrue(loaded.draft.sessionId.startsWith("S-LEGACY-"))
        assertEquals("B", loaded.draft.responses["LOG_001"])

        prefs.edit().clear().commit()
    }

}
