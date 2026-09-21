package com.potentia.research

import com.potentia.BuildConfig

object PilotSyncConfig {
    val endpoint: String
        get() = BuildConfig.PILOT_SYNC_ENDPOINT.trim()

    val uploadToken: String
        get() = BuildConfig.PILOT_SYNC_TOKEN.trim()

    val isConfigured: Boolean
        get() = endpoint.startsWith("https://") && uploadToken.isNotBlank()
}
