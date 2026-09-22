package com.potentia.research

import org.json.JSONArray
import org.json.JSONObject

object PilotSyncPayload {

    fun create(
        record: PilotSessionRecord,
        uploadToken: String,
        dataKind: String = "pilot"
    ): String = JSONObject().apply {
        put("uploadToken", uploadToken)
        put("schemaVersion", "potentia-sync-v1")
        put("source", "android")
        put("dataKind", dataKind)
        put("session", sessionJson(record))
    }.toString()

    fun sessionJson(record: PilotSessionRecord): JSONObject = JSONObject().apply {
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

        put("dimensions", JSONObject().apply {
            record.result.dimensions.forEach { (dimensionId, dimension) ->
                put(dimensionId, JSONObject().apply {
                    put("score", dimension.score ?: JSONObject.NULL)
                    put("answered", dimension.answered)
                    put("expected", dimension.expected)
                    put("answeredRatio", dimension.answeredRatio)
                    put("qualityFlag", dimension.qualityFlag ?: JSONObject.NULL)
                    put("experimental", dimension.experimental)
                    put("outOfDomain", dimension.outOfDomain)
                })
            }
        })
    }
}
