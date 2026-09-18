package com.potentia.growth

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields

object GrowthStorage {
    private const val KEY_PROGRESS = "growth_progress_v3"
    private const val LEGACY_KEY_PROGRESS = "growth_progress_v2"

    fun currentWeekKey(now: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val weekFields = WeekFields.ISO
        val weekYear = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        return "%04d-W%02d".format(weekYear, week)
    }

    fun load(
        prefs: SharedPreferences,
        weekKey: String,
        dimensionId: String
    ): GrowthProgress {
        val entryKey = entryKey(weekKey, dimensionId)
        val raw = prefs.getString(KEY_PROGRESS, null)

        if (raw != null) {
            return runCatching {
                val root = JSONObject(raw)
                val entries = root.optJSONObject("entries") ?: JSONObject()
                val entry = entries.optJSONObject(entryKey)
                    ?: return@runCatching empty(weekKey, dimensionId)
                parseProgress(entry, weekKey, dimensionId)
            }.getOrElse {
                empty(weekKey, dimensionId)
            }
        }

        // One-time compatibility path for Growth V2. We intentionally do not
        // delete the old value during reads. The next save writes V3.
        return loadLegacy(prefs, weekKey, dimensionId)
            ?: empty(weekKey, dimensionId)
    }

    fun save(prefs: SharedPreferences, progress: GrowthProgress) {
        val root = runCatching {
            prefs.getString(KEY_PROGRESS, null)?.let(::JSONObject)
        }.getOrNull() ?: JSONObject()

        val entries = root.optJSONObject("entries") ?: JSONObject().also {
            root.put("entries", it)
        }

        entries.put(entryKey(progress.weekKey, progress.dimensionId), progressToJson(progress))
        root.put("schema_version", 3)

        prefs.edit().putString(KEY_PROGRESS, root.toString()).apply()
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit()
            .remove(KEY_PROGRESS)
            .remove(LEGACY_KEY_PROGRESS)
            .apply()
    }

    private fun progressToJson(progress: GrowthProgress): JSONObject =
        JSONObject().apply {
            put("week_key", progress.weekKey)
            put("dimension_id", progress.dimensionId)
            put("selected_exercise_id", progress.selectedExerciseId ?: JSONObject.NULL)
            put("active_exercise_id", progress.activeExerciseId ?: JSONObject.NULL)

            put(
                "completed_exercise_ids",
                JSONArray().apply {
                    progress.completedExerciseIds.sorted().forEach(::put)
                }
            )

            put(
                "practice_records",
                JSONObject().apply {
                    progress.practiceRecords.toSortedMap().forEach { (exerciseId, record) ->
                        put(
                            exerciseId,
                            JSONObject().apply {
                                put("exercise_id", record.exerciseId)
                                put("verified_interaction", record.verifiedInteraction)
                                put("completed_at", record.completedAt)
                                put(
                                    "responses",
                                    JSONObject().apply {
                                        record.responses.toSortedMap().forEach { (key, value) ->
                                            put(key, value)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )

            put("updated_at", progress.updatedAt)
        }

    private fun parseProgress(
        json: JSONObject,
        expectedWeekKey: String,
        expectedDimensionId: String
    ): GrowthProgress {
        val storedWeek = json.optString("week_key")
        val storedDimension = json.optString("dimension_id")

        if (storedWeek != expectedWeekKey || storedDimension != expectedDimensionId) {
            return empty(expectedWeekKey, expectedDimensionId)
        }

        val completed = buildSet {
            val array = json.optJSONArray("completed_exercise_ids") ?: JSONArray()
            for (index in 0 until array.length()) {
                val id = array.optString(index)
                if (id.isNotBlank()) add(id)
            }
        }

        val recordsJson = json.optJSONObject("practice_records") ?: JSONObject()
        val records = buildMap {
            val keys = recordsJson.keys()
            while (keys.hasNext()) {
                val exerciseId = keys.next()
                val recordJson = recordsJson.optJSONObject(exerciseId) ?: continue
                val responsesJson = recordJson.optJSONObject("responses") ?: JSONObject()
                val responses = buildMap {
                    val responseKeys = responsesJson.keys()
                    while (responseKeys.hasNext()) {
                        val key = responseKeys.next()
                        val value = responsesJson.optString(key)
                        if (value.isNotBlank()) put(key, value)
                    }
                }

                put(
                    exerciseId,
                    GrowthPracticeRecord(
                        exerciseId = recordJson.optString("exercise_id", exerciseId),
                        responses = responses,
                        verifiedInteraction = recordJson.optBoolean("verified_interaction", false),
                        completedAt = recordJson.optLong("completed_at", 0L)
                    )
                )
            }
        }

        return GrowthProgress(
            weekKey = storedWeek,
            dimensionId = storedDimension,
            selectedExerciseId = json.nullableString("selected_exercise_id"),
            activeExerciseId = json.nullableString("active_exercise_id"),
            completedExerciseIds = completed,
            practiceRecords = records,
            updatedAt = json.optLong("updated_at", System.currentTimeMillis())
        )
    }

    private fun loadLegacy(
        prefs: SharedPreferences,
        weekKey: String,
        dimensionId: String
    ): GrowthProgress? {
        val raw = prefs.getString(LEGACY_KEY_PROGRESS, null) ?: return null

        return runCatching {
            val json = JSONObject(raw)
            val storedWeek = json.optString("week_key")
            val storedDimension = json.optString("dimension_id")

            if (storedWeek != weekKey || storedDimension != dimensionId) {
                return@runCatching null
            }

            val completed = buildSet {
                val array = json.optJSONArray("completed_exercise_ids") ?: JSONArray()
                for (index in 0 until array.length()) {
                    val id = array.optString(index)
                    if (id.isNotBlank()) add(id)
                }
            }

            GrowthProgress(
                weekKey = storedWeek,
                dimensionId = storedDimension,
                selectedExerciseId = json.nullableString("selected_exercise_id"),
                activeExerciseId = json.nullableString("active_exercise_id"),
                completedExerciseIds = completed,
                practiceRecords = emptyMap(),
                updatedAt = json.optLong("updated_at", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    private fun entryKey(weekKey: String, dimensionId: String): String =
        "$weekKey|$dimensionId"

    private fun empty(weekKey: String, dimensionId: String) = GrowthProgress(
        weekKey = weekKey,
        dimensionId = dimensionId
    )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null
        else optString(key).takeIf { it.isNotBlank() }
}
