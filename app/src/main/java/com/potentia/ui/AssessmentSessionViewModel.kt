package com.potentia.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.potentia.ai.CreativeScorer
import com.potentia.assessment.AssessmentBank
import com.potentia.assessment.AssessmentDraft
import com.potentia.assessment.AssessmentDraftStorage
import com.potentia.assessment.AssessmentResult
import com.potentia.assessment.AssessmentScoringEngine
import com.potentia.assessment.AssessmentStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

internal enum class AssessmentSessionStatus {
    IDLE,
    IN_PROGRESS,
    PROCESSING,
    ERROR,
    COMPLETED
}

internal enum class AssessmentDraftAvailability {
    NONE,
    VALID,
    OUTDATED,
    CORRUPT
}

internal class AssessmentSessionViewModel(
    private val bank: AssessmentBank,
    private val prefs: SharedPreferences,
    private val applicationContext: Context
) : ViewModel() {

    var currentQuestionIndex by mutableIntStateOf(0)
        private set

    var responses by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var startedAt by mutableLongStateOf(0L)
        private set

    var status by mutableStateOf(AssessmentSessionStatus.IDLE)
        private set

    var processingError by mutableStateOf<String?>(null)
        private set

    var draftAvailability by mutableStateOf(AssessmentDraftAvailability.NONE)
        private set

    var completedResult by mutableStateOf<AssessmentResult?>(null)
        private set

    private var loadedDraft: AssessmentDraft? = null
    private var draftSaveJob: Job? = null

    @Volatile
    private var draftClosed = false

    val draftAnsweredCount: Int
        get() = when (draftAvailability) {
            AssessmentDraftAvailability.VALID -> responses.count { it.value.isNotBlank() }
            else -> loadedDraft?.responses?.count { it.value.isNotBlank() } ?: 0
        }

    val draftQuestionNumber: Int
        get() = (currentQuestionIndex + 1).coerceIn(1, bank.totalItems.coerceAtLeast(1))

    val hasStoredDraft: Boolean
        get() = draftAvailability != AssessmentDraftAvailability.NONE ||
            AssessmentDraftStorage.hasStoredDraft(prefs)

    init {
        restoreDraftMetadataAndSession()
    }

    fun startNewAssessment() {
        draftSaveJob?.cancel()
        draftClosed = false
        responses = emptyMap()
        currentQuestionIndex = 0
        startedAt = System.currentTimeMillis()
        processingError = null
        completedResult = null
        status = AssessmentSessionStatus.IN_PROGRESS
        draftAvailability = AssessmentDraftAvailability.VALID
        loadedDraft = null
        saveDraftNow()
    }

    fun resumeAssessment(): Boolean {
        if (draftAvailability != AssessmentDraftAvailability.VALID) return false
        processingError = null
        status = AssessmentSessionStatus.IN_PROGRESS
        return true
    }

    fun restartAssessment() {
        AssessmentDraftStorage.clear(prefs)
        startNewAssessment()
    }

    fun answer(itemId: String, value: String) {
        responses = responses.toMutableMap().apply {
            this[itemId] = value
        }
        processingError = null

        val item = bank.items.firstOrNull { it.itemId == itemId }
        val isFreeText = item?.responseType == "free_text" ||
            item?.responseType == "free_text_list"

        if (isFreeText) {
            scheduleDebouncedDraftSave()
        } else {
            saveDraftNow()
        }
    }

    fun goNext(): Boolean {
        if (bank.items.isEmpty()) return false
        if (currentQuestionIndex >= bank.items.lastIndex) return false

        currentQuestionIndex += 1
        saveDraftNow()
        return true
    }

    fun goBack(): Boolean {
        if (currentQuestionIndex <= 0) {
            saveDraftNow()
            return false
        }

        currentQuestionIndex -= 1
        saveDraftNow()
        return true
    }

    fun forceSaveDraft(synchronous: Boolean = false) {
        draftSaveJob?.cancel()
        val draft = snapshotDraft() ?: return

        if (synchronous) {
            // Drafts are tiny (< 47 responses). A synchronous commit at lifecycle
            // boundaries avoids losing the latest text when the process is stopped.
            AssessmentDraftStorage.save(
                prefs = prefs,
                draft = draft,
                synchronous = true
            )
        } else {
            AssessmentDraftStorage.save(prefs, draft)
        }
    }

    fun submitAssessment() {
        if (status == AssessmentSessionStatus.PROCESSING) return
        if (bank.items.isEmpty()) {
            processingError = "Item bank kosong."
            status = AssessmentSessionStatus.ERROR
            return
        }

        draftSaveJob?.cancel()
        saveDraftNow()
        processingError = null
        status = AssessmentSessionStatus.PROCESSING

        val responseSnapshot = responses.toMap()

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val creativeScorer = CreativeScorer.fromAssets(applicationContext)
                    AssessmentScoringEngine(creativeScorer).score(
                        bank = bank,
                        responses = responseSnapshot
                    )
                }

                val persisted = withContext(Dispatchers.IO) {
                    AssessmentStorage.append(prefs, result)
                }

                if (!persisted) {
                    throw IOException("Hasil asesmen gagal disimpan.")
                }

                draftClosed = true
                withContext(Dispatchers.IO) {
                    AssessmentDraftStorage.clear(
                        prefs = prefs,
                        synchronous = true
                    )
                }

                draftAvailability = AssessmentDraftAvailability.NONE
                loadedDraft = null
                processingError = null
                status = AssessmentSessionStatus.COMPLETED
                completedResult = result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                processingError = error.message ?: error::class.java.simpleName
                status = AssessmentSessionStatus.ERROR
                forceSaveDraft(synchronous = true)
            }
        }
    }

    fun consumeCompletedResult() {
        completedResult = null
    }

    private fun restoreDraftMetadataAndSession() {
        when (val result = AssessmentDraftStorage.read(prefs)) {
            AssessmentDraftStorage.ReadResult.None -> {
                draftAvailability = AssessmentDraftAvailability.NONE
                loadedDraft = null
            }

            AssessmentDraftStorage.ReadResult.Corrupt -> {
                draftAvailability = AssessmentDraftAvailability.CORRUPT
                loadedDraft = null
            }

            is AssessmentDraftStorage.ReadResult.Success -> {
                val draft = result.draft
                loadedDraft = draft

                val validItemIds = bank.items.mapTo(HashSet()) { it.itemId }
                val versionMatches = draft.assessmentVersion == bank.assessmentVersion
                val countMatches = draft.totalItems == bank.totalItems
                val indexValid = bank.items.isNotEmpty() &&
                    draft.currentQuestionIndex in bank.items.indices
                val responseKeysValid = draft.responses.keys.all { it in validItemIds }
                val startedAtValid = draft.startedAt > 0L

                when {
                    !versionMatches || !countMatches -> {
                        draftAvailability = AssessmentDraftAvailability.OUTDATED
                    }

                    !indexValid || !responseKeysValid || !startedAtValid -> {
                        draftAvailability = AssessmentDraftAvailability.CORRUPT
                    }

                    else -> {
                        draftAvailability = AssessmentDraftAvailability.VALID
                        responses = draft.responses
                        currentQuestionIndex = draft.currentQuestionIndex
                        startedAt = draft.startedAt
                        status = AssessmentSessionStatus.IN_PROGRESS
                    }
                }
            }
        }
    }

    private fun scheduleDebouncedDraftSave() {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(750)
            saveDraftNow()
        }
    }

    private fun saveDraftNow() {
        draftSaveJob?.cancel()
        val draft = snapshotDraft() ?: return
        AssessmentDraftStorage.save(prefs, draft)
        draftAvailability = AssessmentDraftAvailability.VALID
        loadedDraft = draft
    }

    private fun snapshotDraft(): AssessmentDraft? {
        if (draftClosed) return null
        if (startedAt <= 0L) return null
        if (status == AssessmentSessionStatus.COMPLETED) return null

        return AssessmentDraft(
            assessmentVersion = bank.assessmentVersion,
            totalItems = bank.totalItems,
            startedAt = startedAt,
            currentQuestionIndex = currentQuestionIndex.coerceIn(
                0,
                bank.items.lastIndex.coerceAtLeast(0)
            ),
            responses = responses.toMap()
        )
    }

    override fun onCleared() {
        draftSaveJob?.cancel()
        snapshotDraft()?.let { draft ->
            AssessmentDraftStorage.save(
                prefs = prefs,
                draft = draft,
                synchronous = true
            )
        }
        super.onCleared()
    }
}

internal class AssessmentSessionViewModelFactory(
    private val bank: AssessmentBank,
    private val prefs: SharedPreferences,
    context: Context
) : ViewModelProvider.Factory {

    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssessmentSessionViewModel::class.java)) {
            return AssessmentSessionViewModel(
                bank = bank,
                prefs = prefs,
                applicationContext = applicationContext
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
