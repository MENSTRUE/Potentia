package com.potentia.assessment

data class AssessmentChoice(
    val value: String,
    val label: String
)

data class AssessmentItem(
    val assessmentVersion: String,
    val itemId: String,
    val dimensionId: String,
    val subconstruct: String,
    val prompt: String,
    val responseType: String,
    val pilotOnly: Boolean,
    val active: Boolean,
    val choices: List<AssessmentChoice> = emptyList(),
    val correctValue: String? = null,
    val choiceScores: Map<String, Double> = emptyMap(),
    val reverseScored: Boolean = false,
    val asset: String? = null,
    val rubricId: String? = null,
    val maxResponses: Int? = null
)

data class AssessmentSection(
    val id: String,
    val title: String,
    val instruction: String,
    val items: List<AssessmentItem>
)

data class AssessmentBank(
    val assessmentVersion: String,
    val scoringVersion: String,
    val language: String,
    val status: String,
    val productionValidated: Boolean,
    val totalItems: Int,
    val sections: List<AssessmentSection>
) {
    val items: List<AssessmentItem>
        get() = sections.flatMap { it.items }.filter { it.active }

    fun sectionFor(dimensionId: String): AssessmentSection? =
        sections.firstOrNull { it.id == dimensionId }
}

data class AssessmentDraft(
    val assessmentVersion: String,
    val totalItems: Int,
    val startedAt: Long,
    val currentQuestionIndex: Int,
    val responses: Map<String, String>
)

data class DimensionResult(
    val id: String,
    val score: Double?,
    val answered: Int,
    val expected: Int,
    val answeredRatio: Double,
    val qualityFlag: String? = null,
    val experimental: Boolean = false,
    val outOfDomain: Boolean = false
)

data class AssessmentResult(
    val completedAt: Long,
    val assessmentVersion: String,
    val scoringVersion: String,
    val dimensions: Map<String, DimensionResult>
)
