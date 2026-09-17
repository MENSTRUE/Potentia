package com.potentia.growth

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields

object GrowthStorage {
    private const val KEY_PROGRESS = "growth_progress_v2"

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
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return empty(weekKey, dimensionId)

        return runCatching {
            val json = JSONObject(raw)
            val storedWeek = json.optString("week_key")
            val storedDimension = json.optString("dimension_id")

            if (storedWeek != weekKey || storedDimension != dimensionId) {
                return@runCatching empty(weekKey, dimensionId)
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
                updatedAt = json.optLong("updated_at", System.currentTimeMillis())
            )
        }.getOrElse {
            empty(weekKey, dimensionId)
        }
    }

    fun save(prefs: SharedPreferences, progress: GrowthProgress) {
        val json = JSONObject().apply {
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
            put("updated_at", progress.updatedAt)
        }

        prefs.edit().putString(KEY_PROGRESS, json.toString()).apply()
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit().remove(KEY_PROGRESS).apply()
    }

    private fun empty(weekKey: String, dimensionId: String) = GrowthProgress(
        weekKey = weekKey,
        dimensionId = dimensionId
    )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null
        else optString(key).takeIf { it.isNotBlank() }
}
