package com.potentia.research

import android.content.SharedPreferences
import com.potentia.assessment.AssessmentResult
import com.potentia.assessment.DimensionResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PilotResearchStorage {
    const val CURRENT_CONSENT_VERSION = "potentia-pilot-consent-v1"

    private const val KEY_MODE = "pilot_participation_mode_v1"
    private const val KEY_CONSENT_VERSION = "pilot_consent_version_v1"
    private const val KEY_DECIDED_AT = "pilot_consent_decided_at_v1"
    private const val KEY_PARTICIPANT_ID = "pilot_participant_id_v1"
    private const val KEY_SESSIONS = "pilot_sessions_v1"
    private const val MAX_SESSIONS = 50

    fun readConsentState(prefs: SharedPreferences): PilotConsentState {
        val mode = PilotParticipationMode.fromStorage(prefs.getString(KEY_MODE, null))
        val decidedAt = prefs.getLong(KEY_DECIDED_AT, 0L).takeIf { it > 0L }

        return PilotConsentState(
            mode = mode,
            consentVersion = prefs.getString(KEY_CONSENT_VERSION, null),
            decidedAt = decidedAt,
            participantId = prefs.getString(KEY_PARTICIPANT_ID, null)
        )
    }

    fun optIn(prefs: SharedPreferences): PilotConsentState {
        val participantId = prefs.getString(KEY_PARTICIPANT_ID, null)
            ?.takeIf { it.isNotBlank() }
            ?: newParticipantId()
        val now = System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_MODE, PilotParticipationMode.RESEARCH_OPT_IN.storageValue)
            .putString(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
            .putLong(KEY_DECIDED_AT, now)
            .putString(KEY_PARTICIPANT_ID, participantId)
            .commit()

        return readConsentState(prefs)
    }

    fun choosePersonalOnly(prefs: SharedPreferences): PilotConsentState {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_MODE, PilotParticipationMode.PERSONAL_ONLY.storageValue)
            .remove(KEY_CONSENT_VERSION)
            .putLong(KEY_DECIDED_AT, now)
            .commit()
        return readConsentState(prefs)
    }

    fun clearPilotDataAndWithdraw(prefs: SharedPreferences): Boolean =
        prefs.edit()
            .putString(KEY_MODE, PilotParticipationMode.PERSONAL_ONLY.storageValue)
            .remove(KEY_CONSENT_VERSION)
            .putLong(KEY_DECIDED_AT, System.currentTimeMillis())
            .remove(KEY_PARTICIPANT_ID)
            .remove(KEY_SESSIONS)
            .commit()

    fun loadSessions(prefs: SharedPreferences): List<PilotSessionRecord> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(parseSession(array.getJSONObject(index)))
                }
            }.sortedBy { it.completedAt }
        }.getOrElse { emptyList() }
    }

    fun upsertSession(
        prefs: SharedPreferences,
        record: PilotSessionRecord
    ): Boolean {
        val existing = loadSessions(prefs)
            .filterNot { it.sessionId == record.sessionId }
        val updated = (existing + record)
            .sortedBy { it.completedAt }
            .takeLast(MAX_SESSIONS)

        val array = JSONArray()
        updated.forEach { array.put(toJson(it)) }

        return prefs.edit()
            .putString(KEY_SESSIONS, array.toString())
            .commit()
    }

    fun exportJson(
        consentState: PilotConsentState,
        sessions: List<PilotSessionRecord>
    ): String {
        val root = JSONObject().apply {
            put("exportVersion", "potentia-pilot-export-v1")
            put("generatedAt", System.currentTimeMillis())
            put("consentVersion", consentState.consentVersion ?: JSONObject.NULL)
            put("participantId", consentState.participantId ?: JSONObject.NULL)
            put("notice", "PILOT / RESEARCH ONLY. Raw item responses may include free text. No automatic upload is performed by the app.")
            put("sessionCount", sessions.size)
            put("sessions", JSONArray().apply {
                sessions.sortedBy { it.completedAt }.forEach { put(toJson(it)) }
            })
        }
        return root.toString(2)
    }

    fun exportLongCsv(sessions: List<PilotSessionRecord>): String {
        val header = listOf(
            "participant_id",
            "session_id",
            "consent_version",
            "app_version",
            "started_at",
            "completed_at",
            "assessment_version",
            "scoring_version",
            "language",
            "item_id",
            "dimension_id",
            "response_type",
            "response",
            "dimension_score",
            "experimental",
            "out_of_domain"
        ).joinToString(",")

        val rows = sessions.sortedBy { it.completedAt }.flatMap { session ->
            session.responses.map { item ->
                val dimension = session.result.dimensions[item.dimensionId]
                listOf(
                    session.participantId,
                    session.sessionId,
                    session.consentVersion,
                    session.appVersion,
                    session.startedAt.toString(),
                    session.completedAt.toString(),
                    session.assessmentVersion,
                    session.scoringVersion,
                    session.language,
                    item.itemId,
                    item.dimensionId,
                    item.responseType,
                    item.response,
                    dimension?.score?.toString().orEmpty(),
                    dimension?.experimental?.toString().orEmpty(),
                    dimension?.outOfDomain?.toString().orEmpty()
                ).joinToString(",", transform = ::csvCell)
            }
        }

        return buildString {
            appendLine(header)
            rows.forEach { appendLine(it) }
        }
    }

    fun newSessionId(): String = "S-${UUID.randomUUID()}"

    private fun newParticipantId(): String = "P-${UUID.randomUUID()}"

    private fun csvCell(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    private fun toJson(record: PilotSessionRecord): JSONObject = JSONObject().apply {
        put("schemaVersion", 1)
        put("participantId", record.participantId)
        put("sessionId", record.sessionId)
        put("consentVersion", record.consentVersion)
        put("appVersion", record.appVersion)
        put("startedAt", record.startedAt)
        put("completedAt", record.completedAt)
        put("assessmentVersion", record.assessmentVersion)
        put("scoringVersion", record.scoringVersion)
        put("language", record.language)
        put("responses", JSONArray().apply {
            record.responses.forEach { response ->
                put(JSONObject().apply {
                    put("itemId", response.itemId)
                    put("dimensionId", response.dimensionId)
                    put("responseType", response.responseType)
                    put("response", response.response)
                })
            }
        })
        put("result", resultToJson(record.result))
    }

    private fun parseSession(json: JSONObject): PilotSessionRecord {
        require(json.optInt("schemaVersion", -1) == 1) {
            "Unsupported pilot session schema."
        }

        val responsesArray = json.getJSONArray("responses")
        val responses = buildList {
            for (index in 0 until responsesArray.length()) {
                val response = responsesArray.getJSONObject(index)
                add(
                    PilotItemResponse(
                        itemId = response.getString("itemId"),
                        dimensionId = response.getString("dimensionId"),
                        responseType = response.getString("responseType"),
                        response = response.getString("response")
                    )
                )
            }
        }

        return PilotSessionRecord(
            participantId = json.getString("participantId"),
            sessionId = json.getString("sessionId"),
            consentVersion = json.getString("consentVersion"),
            appVersion = json.optString("appVersion", "unknown"),
            startedAt = json.getLong("startedAt"),
            completedAt = json.getLong("completedAt"),
            assessmentVersion = json.getString("assessmentVersion"),
            scoringVersion = json.getString("scoringVersion"),
            language = json.optString("language", "unknown"),
            responses = responses,
            result = parseResult(json.getJSONObject("result"))
        )
    }

    private fun resultToJson(result: AssessmentResult): JSONObject {
        val dimensions = JSONObject()
        result.dimensions.forEach { (id, value) ->
            dimensions.put(
                id,
                JSONObject().apply {
                    put("id", value.id)
                    if (value.score == null) put("score", JSONObject.NULL) else put("score", value.score)
                    put("answered", value.answered)
                    put("expected", value.expected)
                    put("answeredRatio", value.answeredRatio)
                    if (value.qualityFlag == null) put("qualityFlag", JSONObject.NULL) else put("qualityFlag", value.qualityFlag)
                    put("experimental", value.experimental)
                    put("outOfDomain", value.outOfDomain)
                }
            )
        }

        return JSONObject().apply {
            put("completedAt", result.completedAt)
            put("assessmentVersion", result.assessmentVersion)
            put("scoringVersion", result.scoringVersion)
            put("dimensions", dimensions)
        }
    }

    private fun parseResult(json: JSONObject): AssessmentResult {
        val dimensionsJson = json.getJSONObject("dimensions")
        val dimensions = LinkedHashMap<String, DimensionResult>()
        val keys = dimensionsJson.keys()

        while (keys.hasNext()) {
            val id = keys.next()
            val value = dimensionsJson.getJSONObject(id)
            dimensions[id] = DimensionResult(
                id = value.optString("id", id),
                score = if (value.isNull("score")) null else value.getDouble("score"),
                answered = value.optInt("answered", 0),
                expected = value.optInt("expected", 0),
                answeredRatio = value.optDouble("answeredRatio", 0.0),
                qualityFlag = if (value.isNull("qualityFlag")) null else value.getString("qualityFlag"),
                experimental = value.optBoolean("experimental", false),
                outOfDomain = value.optBoolean("outOfDomain", false)
            )
        }

        return AssessmentResult(
            completedAt = json.getLong("completedAt"),
            assessmentVersion = json.optString("assessmentVersion", "unknown"),
            scoringVersion = json.optString("scoringVersion", "unknown"),
            dimensions = dimensions
        )
    }
}
