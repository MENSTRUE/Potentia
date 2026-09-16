package com.potentia.assessment

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object AssessmentStorage {
    private const val KEY_HISTORY = "assessment_history_v2"
    private const val MAX_HISTORY = 20

    fun loadHistory(prefs: SharedPreferences): List<AssessmentResult> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(parseResult(array.getJSONObject(index)))
                }
            }.sortedBy { it.completedAt }
        }.getOrElse { emptyList() }
    }

    fun append(
        prefs: SharedPreferences,
        result: AssessmentResult
    ) {
        val updated = (loadHistory(prefs) + result)
            .sortedBy { it.completedAt }
            .takeLast(MAX_HISTORY)

        val array = JSONArray()
        updated.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit()
            .remove(KEY_HISTORY)
            .remove("assessment_complete")
            .apply()
    }

    fun exportJson(history: List<AssessmentResult>): String {
        val root = JSONObject().apply {
            put("exportVersion", "potentia-local-export-v1")
            put("generatedAt", System.currentTimeMillis())
            put("notice", "PILOT / RESEARCH ONLY. Scores are not percentiles, population norms, IQ, or psychological diagnoses.")
            put("assessmentCount", history.size)
            put("assessments", JSONArray().apply {
                history.sortedBy { it.completedAt }.forEach { put(toJson(it)) }
            })
        }
        return root.toString(2)
    }

    private fun toJson(result: AssessmentResult): JSONObject {
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
