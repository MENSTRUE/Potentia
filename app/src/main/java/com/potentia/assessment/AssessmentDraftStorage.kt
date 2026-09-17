package com.potentia.assessment

import android.content.SharedPreferences
import org.json.JSONObject

object AssessmentDraftStorage {
    private const val KEY_DRAFT = "assessment_draft_v1"

    sealed interface ReadResult {
        data object None : ReadResult
        data class Success(val draft: AssessmentDraft) : ReadResult
        data object Corrupt : ReadResult
    }

    fun read(prefs: SharedPreferences): ReadResult {
        val raw = prefs.getString(KEY_DRAFT, null) ?: return ReadResult.None

        return runCatching {
            val json = JSONObject(raw)
            require(json.optInt("schemaVersion", -1) == 1) {
                "Unsupported assessment draft schema."
            }
            val responsesJson = json.getJSONObject("responses")
            val responses = LinkedHashMap<String, String>()
            val keys = responsesJson.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                responses[key] = responsesJson.getString(key)
            }

            AssessmentDraft(
                assessmentVersion = json.getString("assessmentVersion"),
                totalItems = json.getInt("totalItems"),
                startedAt = json.getLong("startedAt"),
                currentQuestionIndex = json.getInt("currentQuestionIndex"),
                responses = responses
            )
        }.fold(
            onSuccess = { ReadResult.Success(it) },
            onFailure = { ReadResult.Corrupt }
        )
    }

    fun hasStoredDraft(prefs: SharedPreferences): Boolean =
        prefs.contains(KEY_DRAFT)

    fun save(
        prefs: SharedPreferences,
        draft: AssessmentDraft,
        synchronous: Boolean = false
    ): Boolean {
        val responses = JSONObject()
        draft.responses.forEach { (itemId, response) ->
            responses.put(itemId, response)
        }

        val root = JSONObject().apply {
            put("schemaVersion", 1)
            put("assessmentVersion", draft.assessmentVersion)
            put("totalItems", draft.totalItems)
            put("startedAt", draft.startedAt)
            put("currentQuestionIndex", draft.currentQuestionIndex)
            put("responses", responses)
        }

        val editor = prefs.edit().putString(KEY_DRAFT, root.toString())
        return if (synchronous) {
            editor.commit()
        } else {
            editor.apply()
            true
        }
    }

    fun clear(
        prefs: SharedPreferences,
        synchronous: Boolean = false
    ): Boolean {
        val editor = prefs.edit().remove(KEY_DRAFT)
        return if (synchronous) {
            editor.commit()
        } else {
            editor.apply()
            true
        }
    }
}
