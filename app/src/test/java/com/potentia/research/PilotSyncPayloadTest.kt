package com.potentia.research

import com.potentia.assessment.AssessmentResult
import com.potentia.assessment.DimensionResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PilotSyncPayloadTest {

    @Test
    fun payloadContainsSessionResponsesAndDimensions() {
        val record = PilotSessionRecord(
            participantId = "P-test",
            sessionId = "S-test",
            consentVersion = "consent-v2",
            appVersion = "0.2.0-rc1.2",
            startedAt = 1000L,
            completedAt = 2000L,
            assessmentVersion = "assessment-v1",
            scoringVersion = "scoring-v1",
            language = "id",
            responses = listOf(
                PilotItemResponse(
                    itemId = "LOG_001",
                    dimensionId = "logical",
                    responseType = "single_choice",
                    response = "B"
                )
            ),
            result = AssessmentResult(
                completedAt = 2000L,
                assessmentVersion = "assessment-v1",
                scoringVersion = "scoring-v1",
                dimensions = mapOf(
                    "logical" to DimensionResult(
                        id = "logical",
                        score = 80.0,
                        answered = 1,
                        expected = 1,
                        answeredRatio = 1.0,
                        experimental = false,
                        outOfDomain = false
                    )
                )
            )
        )

        val json = JSONObject(PilotSyncPayload.create(record, "token-123"))
        val session = json.getJSONObject("session")

        assertEquals("token-123", json.getString("uploadToken"))
        assertEquals("potentia-sync-v1", json.getString("schemaVersion"))
        assertEquals("S-test", session.getString("sessionId"))
        assertEquals(1, session.getJSONArray("responses").length())
        assertEquals(80.0, session.getJSONObject("dimensions").getJSONObject("logical").getDouble("score"), 0.0)
        assertTrue(session.getJSONObject("dimensions").has("logical"))
    }
}
