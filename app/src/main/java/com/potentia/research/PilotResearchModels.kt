package com.potentia.research

import com.potentia.assessment.AssessmentResult

enum class PilotParticipationMode(val storageValue: String) {
    UNDECIDED("undecided"),
    PERSONAL_ONLY("personal_only"),
    RESEARCH_OPT_IN("research_opt_in");

    companion object {
        fun fromStorage(value: String?): PilotParticipationMode =
            entries.firstOrNull { it.storageValue == value } ?: UNDECIDED
    }
}

data class PilotConsentState(
    val mode: PilotParticipationMode,
    val consentVersion: String?,
    val decidedAt: Long?,
    val participantId: String?
) {
    val researchEnabled: Boolean
        get() = mode == PilotParticipationMode.RESEARCH_OPT_IN &&
            consentVersion == PilotResearchStorage.CURRENT_CONSENT_VERSION &&
            !participantId.isNullOrBlank()

    val requiresDecision: Boolean
        get() = mode == PilotParticipationMode.UNDECIDED ||
            (mode == PilotParticipationMode.RESEARCH_OPT_IN &&
                consentVersion != PilotResearchStorage.CURRENT_CONSENT_VERSION)
}

data class PilotItemResponse(
    val itemId: String,
    val dimensionId: String,
    val responseType: String,
    val response: String
)

data class PilotSessionRecord(
    val participantId: String,
    val sessionId: String,
    val consentVersion: String,
    val appVersion: String,
    val startedAt: Long,
    val completedAt: Long,
    val assessmentVersion: String,
    val scoringVersion: String,
    val language: String,
    val responses: List<PilotItemResponse>,
    val result: AssessmentResult
)
