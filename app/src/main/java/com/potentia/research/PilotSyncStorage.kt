package com.potentia.research

import android.content.SharedPreferences

object PilotSyncStorage {
    private const val KEY_SYNCED_IDS = "pilot_sync_synced_session_ids_v1"
    private const val KEY_LAST_ATTEMPT_AT = "pilot_sync_last_attempt_at_v1"
    private const val KEY_LAST_SUCCESS_AT = "pilot_sync_last_success_at_v1"
    private const val KEY_LAST_ERROR = "pilot_sync_last_error_v1"

    data class Summary(
        val totalSessions: Int,
        val syncedSessions: Int,
        val pendingSessions: Int,
        val lastAttemptAt: Long?,
        val lastSuccessAt: Long?,
        val lastError: String?
    )

    fun syncedIds(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_SYNCED_IDS, emptySet())?.toSet().orEmpty()

    fun pendingSessions(
        prefs: SharedPreferences,
        sessions: List<PilotSessionRecord>
    ): List<PilotSessionRecord> {
        val synced = syncedIds(prefs)
        return sessions
            .filterNot { it.sessionId in synced }
            .sortedBy { it.completedAt }
    }

    fun summary(
        prefs: SharedPreferences,
        sessions: List<PilotSessionRecord>
    ): Summary {
        val synced = syncedIds(prefs)
        val syncedCount = sessions.count { it.sessionId in synced }
        return Summary(
            totalSessions = sessions.size,
            syncedSessions = syncedCount,
            pendingSessions = (sessions.size - syncedCount).coerceAtLeast(0),
            lastAttemptAt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L).takeIf { it > 0L },
            lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L).takeIf { it > 0L },
            lastError = prefs.getString(KEY_LAST_ERROR, null)?.takeIf { it.isNotBlank() }
        )
    }

    fun markAttempt(prefs: SharedPreferences) {
        prefs.edit()
            .putLong(KEY_LAST_ATTEMPT_AT, System.currentTimeMillis())
            .apply()
    }

    fun markSynced(
        prefs: SharedPreferences,
        sessionId: String
    ) {
        val updated = syncedIds(prefs).toMutableSet().apply { add(sessionId) }
        prefs.edit()
            .putStringSet(KEY_SYNCED_IDS, updated)
            .putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
            .remove(KEY_LAST_ERROR)
            .commit()
    }

    fun markError(
        prefs: SharedPreferences,
        message: String
    ) {
        prefs.edit()
            .putString(KEY_LAST_ERROR, message.take(500))
            .apply()
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit()
            .remove(KEY_SYNCED_IDS)
            .remove(KEY_LAST_ATTEMPT_AT)
            .remove(KEY_LAST_SUCCESS_AT)
            .remove(KEY_LAST_ERROR)
            .commit()
    }
}
