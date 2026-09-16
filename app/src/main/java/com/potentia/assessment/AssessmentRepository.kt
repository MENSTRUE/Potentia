package com.potentia.assessment

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AssessmentRepository {
    const val DEFAULT_ASSET_PATH = "potentia_assessment/item_bank_v1_1.json"

    fun load(
        context: Context,
        assetPath: String = DEFAULT_ASSET_PATH
    ): AssessmentBank {
        val json = context.assets
            .open(assetPath)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        return parse(JSONObject(json))
    }

    private fun parse(root: JSONObject): AssessmentBank {
        val sectionsJson = root.getJSONArray("sections")
        val sections = buildList {
            for (index in 0 until sectionsJson.length()) {
                add(parseSection(sectionsJson.getJSONObject(index)))
            }
        }

        return AssessmentBank(
            assessmentVersion = root.getString("assessmentVersion"),
            scoringVersion = root.getString("scoringVersion"),
            language = root.optString("language", "id"),
            status = root.optString("status", "PILOT_ONLY"),
            productionValidated = root.optBoolean("productionValidated", false),
            totalItems = root.getInt("totalItems"),
            sections = sections
        )
    }

    private fun parseSection(json: JSONObject): AssessmentSection {
        val itemsJson = json.getJSONArray("items")
        val items = buildList {
            for (index in 0 until itemsJson.length()) {
                add(parseItem(itemsJson.getJSONObject(index)))
            }
        }

        return AssessmentSection(
            id = json.getString("id"),
            title = json.getString("title"),
            instruction = json.optString("instruction", ""),
            items = items
        )
    }

    private fun parseItem(json: JSONObject): AssessmentItem {
        return AssessmentItem(
            assessmentVersion = json.getString("assessmentVersion"),
            itemId = json.getString("itemId"),
            dimensionId = json.getString("dimensionId"),
            subconstruct = json.optString("subconstruct", ""),
            prompt = json.getString("prompt"),
            responseType = json.getString("responseType"),
            pilotOnly = json.optBoolean("pilotOnly", true),
            active = json.optBoolean("active", true),
            choices = parseChoices(json.optJSONArray("choices")),
            correctValue = json.optNullableString("correctValue"),
            choiceScores = parseScoreMap(json.optJSONObject("choiceScores")),
            reverseScored = json.optBoolean("reverseScored", false),
            asset = json.optNullableString("asset"),
            rubricId = json.optNullableString("rubricId"),
            maxResponses = if (json.has("maxResponses") && !json.isNull("maxResponses")) {
                json.getInt("maxResponses")
            } else {
                null
            }
        )
    }

    private fun parseChoices(array: JSONArray?): List<AssessmentChoice> {
        if (array == null) return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val choice = array.getJSONObject(index)
                add(
                    AssessmentChoice(
                        value = choice.get("value").toString(),
                        label = choice.getString("label")
                    )
                )
            }
        }
    }

    private fun parseScoreMap(json: JSONObject?): Map<String, Double> {
        if (json == null) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.getDouble(key)
        }
        return result
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return getString(name)
    }
}
