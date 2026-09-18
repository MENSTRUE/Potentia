package com.potentia.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potentia.assessment.*
import com.potentia.growth.*
import com.potentia.reminder.WeeklyReminderPolicy
import com.potentia.reminder.WeeklyReminderScheduler
import com.potentia.research.*
import com.potentia.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Screen {
    SPLASH,
    ONBOARDING_1, ONBOARDING_2, ONBOARDING_3,
    HOME, ASSESSMENT_INTRO, ASSESSMENT_SESSION, PILOT_CONSENT,
    PROCESSING, RESULT_OVERVIEW, POTENTIAL_DETAIL, POTENTIAL_MAP,
    GROWTH, HISTORY, COMPARISON, DIMENSION_LIBRARY,
    PROFILE, SETTINGS, PILOT_DATA, ABOUT
}

private enum class PilotConsentPurpose { START_ASSESSMENT, RESTART_ASSESSMENT, SETTINGS }

private enum class MainTab { HOME, ASSESSMENT, GROWTH, PROFILE }

private data class PotentialScore(
    val label: String,
    val value: Int
)

private val DimensionOrder = listOf(
    "logical", "creative", "verbal", "spatial", "social", "practical"
)

private val DimensionLabels = mapOf(
    "logical" to "Penalaran Logis",
    "creative" to "Kreatif",
    "verbal" to "Verbal",
    "spatial" to "Spasial",
    "social" to "Sosial",
    "practical" to "Praktis"
)

private data class DimensionInfo(
    val description: String,
    val tips: List<String>
)

private val DimensionInfos = mapOf(
    "logical" to DimensionInfo(
        "Kecenderungan mengenali pola, aturan, hubungan, dan menyusun langkah penyelesaian secara sistematis.",
        listOf("Latihan deduksi informal dan teka-teki logika.", "Tulis alur argumen sebelum mengambil kesimpulan.", "Coba permainan strategi yang membutuhkan beberapa langkah ke depan.")
    ),
    "creative" to DimensionInfo(
        "Kecenderungan menghasilkan alternatif, menghubungkan ide, dan melihat kemungkinan baru.",
        listOf("Cari tiga penggunaan alternatif untuk benda sehari-hari.", "Biasakan membuat lebih dari satu solusi sebelum memilih.", "Gabungkan dua ide yang tidak biasa dalam satu latihan kecil.")
    ),
    "verbal" to DimensionInfo(
        "Kecenderungan memahami hubungan makna, inferensi bahasa, dan menyampaikan gagasan secara terstruktur.",
        listOf("Ringkas bacaan menjadi tiga kalimat inti.", "Latih sinonim, antonim, dan analogi kata.", "Jelaskan satu konsep rumit dengan bahasa sederhana.")
    ),
    "spatial" to DimensionInfo(
        "Kecenderungan memahami rotasi, orientasi, bentuk, dan hubungan visual dalam ruang.",
        listOf("Latih rotasi mental dengan bentuk sederhana.", "Coba puzzle visual atau tangram.", "Gambar ulang objek dari sudut pandang berbeda.")
    ),
    "social" to DimensionInfo(
        "Kecenderungan beradaptasi dalam interaksi, bekerja sama, dan membaca kebutuhan orang lain.",
        listOf("Latihan mendengar aktif sebelum memberi respons.", "Mulai percakapan singkat dengan orang baru.", "Catat satu perspektif orang lain yang berbeda dari milikmu.")
    ),
    "practical" to DimensionInfo(
        "Kecenderungan membuat keputusan yang dapat dijalankan dengan mempertimbangkan waktu, risiko, dan sumber daya.",
        listOf("Prioritaskan tugas berdasarkan dampak dan batas waktu.", "Buat rencana cadangan untuk satu keputusan penting.", "Evaluasi hasil keputusan berdasarkan data nyata, bukan asumsi.")
    )
)

private fun AssessmentResult.toPotentialScores(): List<PotentialScore> =
    DimensionOrder.map { id ->
        PotentialScore(
            label = DimensionLabels[id] ?: id,
            value = dimensions[id]?.score?.roundToInt()?.coerceIn(0, 100) ?: 0
        )
    }

private fun formatAssessmentDate(timestamp: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(timestamp))

private val DarkScreens = setOf(Screen.SPLASH, Screen.PROCESSING)
private val NoBottomBarScreens = setOf(
    Screen.SPLASH, Screen.ONBOARDING_1, Screen.ONBOARDING_2, Screen.ONBOARDING_3,
    Screen.ASSESSMENT_SESSION, Screen.PILOT_CONSENT, Screen.PROCESSING
)

@Composable
fun PotentiaApp(growthOpenRequest: Int = 0) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("potentia_prefs", Context.MODE_PRIVATE)
    }

    val assessmentBankResult = remember {
        runCatching { AssessmentRepository.load(context) }
    }
    val assessmentBank = assessmentBankResult.getOrNull()

    var history by remember {
        mutableStateOf(AssessmentStorage.loadHistory(prefs))
    }
    var assessmentComplete by remember {
        mutableStateOf(history.isNotEmpty())
    }
    var developmentRecommendationsEnabled by remember {
        mutableStateOf(prefs.getBoolean("development_recommendations", true))
    }
    var pilotConsentState by remember {
        mutableStateOf(PilotResearchStorage.readConsentState(prefs))
    }
    var pilotSessions by remember {
        mutableStateOf(PilotResearchStorage.loadSessions(prefs))
    }
    var pilotConsentPurpose by rememberSaveable {
        mutableStateOf(PilotConsentPurpose.START_ASSESSMENT)
    }
    var selectedResultTimestamp by rememberSaveable {
        mutableStateOf(history.lastOrNull()?.completedAt)
    }
    var selectedDimensionId by rememberSaveable { mutableStateOf("logical") }

    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    val assessmentSessionViewModel = if (assessmentBank != null) {
        val factory = remember(assessmentBank, prefs, context.applicationContext) {
            AssessmentSessionViewModelFactory(
                bank = assessmentBank,
                prefs = prefs,
                context = context.applicationContext
            )
        }
        viewModel<AssessmentSessionViewModel>(factory = factory)
    } else {
        null
    }

    val onboardingComplete = remember {
        prefs.getBoolean("onboarding_complete", false)
    }

    LaunchedEffect(Unit) {
        WeeklyReminderScheduler.sync(
            context = context.applicationContext,
            enabled = prefs.getBoolean("reminder_weekly", false)
        )
    }

    LaunchedEffect(growthOpenRequest) {
        if (growthOpenRequest > 0 && onboardingComplete) {
            activeTab = MainTab.GROWTH
            screen = Screen.GROWTH
        }
    }

    val selectedResult = history.firstOrNull { it.completedAt == selectedResultTimestamp }
        ?: history.lastOrNull()

    val isDark = screen in DarkScreens
    val view = LocalView.current

    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        activity.window.statusBarColor =
            if (isDark) android.graphics.Color.rgb(23, 23, 20)
            else android.graphics.Color.rgb(250, 249, 246)
        activity.window.navigationBarColor =
            if (isDark) android.graphics.Color.rgb(23, 23, 20)
            else android.graphics.Color.rgb(250, 249, 246)
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    LaunchedEffect(screen) {
        if (screen == Screen.SPLASH) {
            delay(1500)
            screen = if (onboardingComplete) Screen.HOME else Screen.ONBOARDING_1
        }
    }

    LaunchedEffect(
        assessmentSessionViewModel?.completedResult?.completedAt,
        assessmentSessionViewModel?.status,
        assessmentSessionViewModel?.draftAvailability,
        screen
    ) {
        val session = assessmentSessionViewModel

        if (session == null) {
            if (screen == Screen.PROCESSING) screen = Screen.ASSESSMENT_INTRO
            return@LaunchedEffect
        }

        session.completedResult?.let { result ->
            history = AssessmentStorage.loadHistory(prefs)
            pilotSessions = PilotResearchStorage.loadSessions(prefs)
            assessmentComplete = history.isNotEmpty()
            selectedResultTimestamp = result.completedAt
            activeTab = MainTab.ASSESSMENT
            screen = Screen.RESULT_OVERVIEW
            session.consumeCompletedResult()
            return@LaunchedEffect
        }

        if (screen == Screen.ASSESSMENT_SESSION &&
            session.status == AssessmentSessionStatus.IDLE
        ) {
            screen = Screen.ASSESSMENT_INTRO
            return@LaunchedEffect
        }

        if (screen == Screen.PROCESSING) {
            when (session.status) {
                AssessmentSessionStatus.IN_PROGRESS -> session.submitAssessment()
                AssessmentSessionStatus.ERROR -> screen = Screen.ASSESSMENT_SESSION
                AssessmentSessionStatus.IDLE -> screen = Screen.ASSESSMENT_INTRO
                AssessmentSessionStatus.PROCESSING,
                AssessmentSessionStatus.COMPLETED -> Unit
            }
        }
    }

    DisposableEffect(context, assessmentSessionViewModel) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                assessmentSessionViewModel?.forceSaveDraft(synchronous = true)
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    fun navigate(target: Screen) {
        screen = target
        activeTab = when (target) {
            Screen.HOME, Screen.DIMENSION_LIBRARY -> MainTab.HOME
            Screen.ASSESSMENT_INTRO, Screen.ASSESSMENT_SESSION, Screen.PILOT_CONSENT,
            Screen.RESULT_OVERVIEW, Screen.POTENTIAL_DETAIL, Screen.POTENTIAL_MAP -> MainTab.ASSESSMENT
            Screen.GROWTH -> MainTab.GROWTH
            Screen.HISTORY, Screen.COMPARISON, Screen.PROFILE,
            Screen.SETTINGS, Screen.PILOT_DATA, Screen.ABOUT -> MainTab.PROFILE
            else -> activeTab
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        navigate(Screen.HOME)
    }

    fun startAssessmentNow() {
        assessmentSessionViewModel?.startNewAssessment()
        navigate(Screen.ASSESSMENT_SESSION)
    }

    fun restartAssessment() {
        assessmentSessionViewModel?.restartAssessment()
        navigate(Screen.ASSESSMENT_SESSION)
    }

    fun requestStartAssessment() {
        pilotConsentState = PilotResearchStorage.readConsentState(prefs)
        if (pilotConsentState.requiresDecision) {
            pilotConsentPurpose = PilotConsentPurpose.START_ASSESSMENT
            navigate(Screen.PILOT_CONSENT)
        } else {
            startAssessmentNow()
        }
    }

    fun requestRestartAssessment() {
        pilotConsentState = PilotResearchStorage.readConsentState(prefs)
        if (pilotConsentState.requiresDecision) {
            pilotConsentPurpose = PilotConsentPurpose.RESTART_ASSESSMENT
            navigate(Screen.PILOT_CONSENT)
        } else {
            restartAssessment()
        }
    }

    fun applyPilotChoice(optIn: Boolean) {
        pilotConsentState = if (optIn) {
            PilotResearchStorage.optIn(prefs)
        } else {
            PilotResearchStorage.choosePersonalOnly(prefs)
        }

        when (pilotConsentPurpose) {
            PilotConsentPurpose.START_ASSESSMENT -> startAssessmentNow()
            PilotConsentPurpose.RESTART_ASSESSMENT -> restartAssessment()
            PilotConsentPurpose.SETTINGS -> navigate(Screen.PILOT_DATA)
        }
    }

    fun resumeAssessment() {
        if (assessmentSessionViewModel?.resumeAssessment() == true) {
            navigate(Screen.ASSESSMENT_SESSION)
        }
    }

    fun openResult(timestamp: Long) {
        selectedResultTimestamp = timestamp
        navigate(Screen.RESULT_OVERVIEW)
    }

    fun openDimension(dimensionId: String) {
        selectedDimensionId = dimensionId
        navigate(Screen.POTENTIAL_DETAIL)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDark) Charcoal else Ivory
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (screen) {
                    Screen.SPLASH -> SplashScreen()
                    Screen.ONBOARDING_1 -> OnboardingScreen(
                        step = 1,
                        title = "Potensi tidak selalu terlihat sejak awal.",
                        body = "Kenali pola berpikir, preferensi, dan cara kamu menghadapi berbagai situasi.",
                        illustration = 1,
                        onNext = { navigate(Screen.ONBOARDING_2) },
                        onSkip = { completeOnboarding() }
                    )
                    Screen.ONBOARDING_2 -> OnboardingScreen(
                        step = 2,
                        title = "Bukan sekadar menjawab pertanyaan.",
                        body = "POTENTIA menggabungkan pertanyaan situasional dengan tantangan singkat untuk membentuk profil potensimu.",
                        illustration = 2,
                        onBack = { navigate(Screen.ONBOARDING_1) },
                        onNext = { navigate(Screen.ONBOARDING_3) },
                        onSkip = { completeOnboarding() }
                    )
                    Screen.ONBOARDING_3 -> OnboardingScreen(
                        step = 3,
                        title = "Potensi dapat berkembang.",
                        body = "Hasil bukan label permanen. Gunakan profilmu sebagai titik awal untuk mencoba, belajar, dan berkembang.",
                        illustration = 3,
                        nextLabel = "Mulai Jelajahi Potensiku",
                        onBack = { navigate(Screen.ONBOARDING_2) },
                        onNext = { completeOnboarding() },
                        onSkip = { completeOnboarding() }
                    )
                    Screen.HOME -> HomeScreen(
                        assessmentComplete = assessmentComplete,
                        assessmentCount = history.size,
                        latestResult = history.lastOrNull(),
                        onStartAssessment = { navigate(Screen.ASSESSMENT_INTRO) },
                        onResult = {
                            history.lastOrNull()?.let { openResult(it.completedAt) }
                        },
                        onPotentialMap = {
                            selectedResultTimestamp = history.lastOrNull()?.completedAt
                            navigate(Screen.POTENTIAL_MAP)
                        },
                        onLibrary = { navigate(Screen.DIMENSION_LIBRARY) },
                        onHistory = { navigate(Screen.HISTORY) }
                    )
                    Screen.ASSESSMENT_INTRO -> AssessmentIntroScreen(
                        draftAvailability = assessmentSessionViewModel?.draftAvailability
                            ?: AssessmentDraftAvailability.NONE,
                        draftAnsweredCount = assessmentSessionViewModel?.draftAnsweredCount ?: 0,
                        draftQuestionNumber = assessmentSessionViewModel?.draftQuestionNumber ?: 1,
                        pilotConsentState = pilotConsentState,
                        onBack = { navigate(Screen.HOME) },
                        onStart = { requestStartAssessment() },
                        onResume = { resumeAssessment() },
                        onRestart = { requestRestartAssessment() }
                    )
                    Screen.PILOT_CONSENT -> PilotConsentScreen(
                        consentState = pilotConsentState,
                        purpose = pilotConsentPurpose,
                        onBack = {
                            if (pilotConsentPurpose == PilotConsentPurpose.SETTINGS) {
                                navigate(Screen.PILOT_DATA)
                            } else {
                                navigate(Screen.ASSESSMENT_INTRO)
                            }
                        },
                        onOptIn = { applyPilotChoice(true) },
                        onPersonalOnly = { applyPilotChoice(false) }
                    )
                    Screen.ASSESSMENT_SESSION -> {
                        val bank = assessmentBank
                        val session = assessmentSessionViewModel
                        val items = bank?.items.orEmpty()

                        when {
                            bank == null || session == null -> {
                                AssessmentLoadErrorScreen(
                                    message = assessmentBankResult.exceptionOrNull()?.message
                                        ?: "Item bank tidak dapat dimuat.",
                                    onBack = { navigate(Screen.ASSESSMENT_INTRO) }
                                )
                            }

                            items.isEmpty() -> {
                                AssessmentLoadErrorScreen(
                                    message = "Item bank tidak memiliki item aktif.",
                                    onBack = { navigate(Screen.ASSESSMENT_INTRO) }
                                )
                            }

                            else -> {
                                val safeIndex = session.currentQuestionIndex.coerceIn(0, items.lastIndex)
                                val item = items[safeIndex]

                                AssessmentSessionScreen(
                                    bank = bank,
                                    item = item,
                                    questionIndex = safeIndex,
                                    response = session.responses[item.itemId].orEmpty(),
                                    errorMessage = session.processingError,
                                    onResponse = { value -> session.answer(item.itemId, value) },
                                    onBack = {
                                        if (!session.goBack()) {
                                            session.forceSaveDraft()
                                            navigate(Screen.ASSESSMENT_INTRO)
                                        }
                                    },
                                    onNext = {
                                        if (safeIndex < items.lastIndex) {
                                            session.goNext()
                                        } else {
                                            session.submitAssessment()
                                            screen = Screen.PROCESSING
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Screen.PROCESSING -> ProcessingScreen()
                    Screen.RESULT_OVERVIEW -> ResultOverviewScreen(
                        result = selectedResult,
                        onMap = { navigate(Screen.POTENTIAL_MAP) },
                        onDetail = { dimensionId -> openDimension(dimensionId) }
                    )
                    Screen.POTENTIAL_DETAIL -> PotentialDetailScreen(
                        result = selectedResult,
                        dimensionId = selectedDimensionId,
                        onBack = { navigate(Screen.RESULT_OVERVIEW) }
                    )
                    Screen.POTENTIAL_MAP -> PotentialMapScreen(
                        result = selectedResult,
                        onBack = { navigate(Screen.RESULT_OVERVIEW) },
                        onDetail = { dimensionId -> openDimension(dimensionId) }
                    )
                    Screen.GROWTH -> GrowthScreen(
                        result = history.lastOrNull(),
                        recommendationsEnabled = developmentRecommendationsEnabled
                    )
                    Screen.HISTORY -> HistoryScreen(
                        history = history,
                        onBack = { navigate(Screen.PROFILE) },
                        onStartAssessment = { navigate(Screen.ASSESSMENT_INTRO) },
                        onResult = { timestamp -> openResult(timestamp) },
                        onCompare = { navigate(Screen.COMPARISON) }
                    )
                    Screen.COMPARISON -> ComparisonScreen(
                        history = history,
                        onBack = { navigate(Screen.HISTORY) }
                    )
                    Screen.DIMENSION_LIBRARY -> DimensionLibraryScreen(
                        onBack = { navigate(Screen.HOME) },
                        onDetail = { dimensionId -> openDimension(dimensionId) }
                    )
                    Screen.PROFILE -> ProfileScreen(
                        assessmentComplete = assessmentComplete,
                        assessmentCount = history.size,
                        onHistory = { navigate(Screen.HISTORY) },
                        onComparison = { navigate(Screen.COMPARISON) },
                        onResult = { navigate(Screen.RESULT_OVERVIEW) },
                        onSettings = { navigate(Screen.SETTINGS) },
                        onPrivacyData = { navigate(Screen.PILOT_DATA) },
                        onAbout = { navigate(Screen.ABOUT) }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        history = history,
                        recommendationsEnabled = developmentRecommendationsEnabled,
                        onRecommendationsEnabledChange = { enabled ->
                            developmentRecommendationsEnabled = enabled
                            prefs.edit().putBoolean("development_recommendations", enabled).apply()
                        },
                        onBack = { navigate(Screen.PROFILE) },
                        onAbout = { navigate(Screen.ABOUT) },
                        onPilotData = { navigate(Screen.PILOT_DATA) },
                        onDeleteHistory = {
                            AssessmentStorage.clear(prefs)
                            GrowthStorage.clear(prefs)
                            history = emptyList()
                            assessmentComplete = false
                            selectedResultTimestamp = null
                        }
                    )
                    Screen.PILOT_DATA -> PilotDataScreen(
                        consentState = pilotConsentState,
                        sessions = pilotSessions,
                        onBack = { navigate(Screen.PROFILE) },
                        onChangeParticipation = {
                            pilotConsentPurpose = PilotConsentPurpose.SETTINGS
                            navigate(Screen.PILOT_CONSENT)
                        },
                        onWithdraw = {
                            pilotConsentState = PilotResearchStorage.choosePersonalOnly(prefs)
                        },
                        onDeletePilotData = {
                            PilotResearchStorage.clearPilotDataAndWithdraw(prefs)
                            pilotConsentState = PilotResearchStorage.readConsentState(prefs)
                            pilotSessions = emptyList()
                        }
                    )
                    Screen.ABOUT -> AboutScreen(
                        onBack = { navigate(Screen.PROFILE) }
                    )
                }
            }

            if (screen !in NoBottomBarScreens) {
                PotentiaBottomBar(
                    activeTab = activeTab,
                    onTab = { tab ->
                        activeTab = tab
                        when (tab) {
                            MainTab.HOME -> navigate(Screen.HOME)
                            MainTab.ASSESSMENT -> navigate(Screen.ASSESSMENT_INTRO)
                            MainTab.GROWTH -> navigate(Screen.GROWTH)
                            MainTab.PROFILE -> navigate(Screen.PROFILE)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Shared UI
// ─────────────────────────────────────────────

@Composable
private fun Heading(
    text: String,
    size: Int = 28,
    color: Color = Charcoal,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = (size + 7).sp,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Gold,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 9.dp)
    )
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Ivory,
            disabledContainerColor = Gold.copy(alpha = .28f),
            disabledContentColor = Ivory.copy(alpha = .65f)
        )
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Stone)
    ) {
        Text(label, color = Charcoal, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TopBack(
    onBack: () -> Unit,
    trailing: String? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Charcoal)
        }
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(it, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProgressLine(step: Int, total: Int) {
    LinearProgressIndicator(
        progress = { step.toFloat() / total.coerceAtLeast(1) },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape),
        color = Gold,
        trackColor = Stone.copy(alpha = .6f)
    )
}

@Composable
private fun AnswerRow(
    index: Int,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val letters = listOf("A", "B", "C", "D", "E")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Gold.copy(alpha = .10f) else Color.White)
            .border(
                1.5.dp,
                if (selected) Gold else Stone.copy(alpha = .85f),
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Gold else Ivory),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letters.getOrElse(index) { "${index + 1}" },
                color = if (selected) Ivory else Muted,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = Charcoal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun PotentiaRings(
    modifier: Modifier = Modifier,
    color: Color = Gold,
    animated: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "rings")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (animated) 9000 else 1000000, easing = LinearEasing)
        ),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        val c = center
        val unit = size.minDimension
        drawCircle(color, radius = unit * .027f, center = c)
        drawCircle(
            color = color,
            radius = unit * .14f,
            center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(unit * .012f)
        )
        rotate(phase * .4f, c) {
            drawArc(
                color.copy(alpha = .75f),
                startAngle = 12f,
                sweepAngle = 286f,
                useCenter = false,
                topLeft = Offset(c.x - unit * .26f, c.y - unit * .26f),
                size = Size(unit * .52f, unit * .52f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(unit * .009f)
            )
        }
        rotate(-phase * .25f, c) {
            drawArc(
                color.copy(alpha = .48f),
                startAngle = 70f,
                sweepAngle = 226f,
                useCenter = false,
                topLeft = Offset(c.x - unit * .38f, c.y - unit * .38f),
                size = Size(unit * .76f, unit * .76f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(unit * .007f)
            )
        }
        drawArc(
            color.copy(alpha = .25f),
            startAngle = 185f,
            sweepAngle = 165f,
            useCenter = false,
            topLeft = Offset(c.x - unit * .46f, c.y - unit * .46f),
            size = Size(unit * .92f, unit * .92f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(unit * .005f)
        )
    }
}

@Composable
private fun ScoreBar(label: String, value: Int, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Charcoal, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value.toString(), color = Gold, fontFamily = FontFamily.Serif, fontSize = 18.sp)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = Gold,
            trackColor = Stone.copy(alpha = .65f)
        )
        subtitle?.let {
            Spacer(Modifier.height(5.dp))
            Text(it, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PotentiaBottomBar(
    activeTab: MainTab,
    onTab: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = Ivory,
        tonalElevation = 0.dp
    ) {
        val tabs = listOf(
            Triple(MainTab.HOME, "Beranda", Icons.Outlined.Home),
            Triple(MainTab.ASSESSMENT, "Asesmen", Icons.Outlined.Assignment),
            Triple(MainTab.GROWTH, "Tumbuh", Icons.Outlined.TrendingUp),
            Triple(MainTab.PROFILE, "Profil", Icons.Outlined.Person)
        )
        tabs.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onTab(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold,
                    selectedTextColor = Gold,
                    indicatorColor = Gold.copy(alpha = .12f),
                    unselectedIconColor = Muted.copy(alpha = .55f),
                    unselectedTextColor = Muted.copy(alpha = .7f)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
// Screens
// ─────────────────────────────────────────────

@Composable
private fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Charcoal),
        contentAlignment = Alignment.Center
    ) {
        PotentiaRings(
            modifier = Modifier.size(360.dp),
            color = Gold.copy(alpha = .22f),
            animated = true
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PotentiaRings(
                modifier = Modifier.size(130.dp),
                color = Ivory.copy(alpha = .92f),
                animated = true
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "POTENTIA",
                color = Ivory,
                fontSize = 38.sp,
                letterSpacing = 7.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Discover what can grow.",
                color = Ivory.copy(alpha = .38f),
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun OnboardingScreen(
    step: Int,
    title: String,
    body: String,
    illustration: Int,
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    onSkip: () -> Unit,
    nextLabel: String = "Lanjut"
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ivory)
            .padding(horizontal = 26.dp, vertical = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text("Lewati", color = Muted.copy(alpha = .72f), fontSize = 13.sp)
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (illustration) {
                1 -> PotentiaRings(Modifier.size(210.dp))
                2 -> OnboardingBlocks()
                else -> GrowthIllustration()
            }
        }

        Heading(title, 27)
        Spacer(Modifier.height(10.dp))
        Text(body, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            (1..3).forEach {
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(if (it == step) 20.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (it == step) Gold else Stone)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(nextLabel, onClick = onNext)

        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Kembali", color = Muted)
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingBlocks() {
    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        PotentiaRings(Modifier.fillMaxSize(), color = Gold.copy(alpha = .22f))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MiniBlock("LOGIKA", Icons.Outlined.Calculate)
                MiniBlock("KREATIF", Icons.Outlined.Lightbulb)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MiniBlock("SPASIAL", Icons.Outlined.ViewInAr)
                MiniBlock("SOSIAL", Icons.Outlined.Groups)
            }
        }
    }
}

@Composable
private fun MiniBlock(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Stone, RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Gold)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 9.sp, color = Muted, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GrowthIllustration() {
    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        PotentiaRings(Modifier.fillMaxSize(), color = Gold.copy(alpha = .35f), animated = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Gold, modifier = Modifier.size(46.dp))
            Spacer(Modifier.height(8.dp))
            Text("GROW", color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
    }
}

@Composable
private fun HomeScreen(
    assessmentComplete: Boolean,
    assessmentCount: Int,
    latestResult: AssessmentResult?,
    onStartAssessment: () -> Unit,
    onResult: () -> Unit,
    onPotentialMap: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit
) {
    val latestScores = latestResult?.toPotentialScores().orEmpty()
    val topLabels = latestResult?.let { result ->
        DimensionOrder.mapNotNull { id ->
            result.dimensions[id]?.score?.let { id to it }
        }
            .sortedByDescending { it.second }
            .take(2)
            .joinToString(" & ") { (id, _) -> DimensionLabels[id] ?: id }
    }.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(Ivory)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Selamat datang", color = Muted.copy(alpha = .75f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Heading("Jelajahi\npotensimu hari ini", 26)
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                PotentiaRings(Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(22.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Gold.copy(alpha = .68f))
        )
        Spacer(Modifier.height(24.dp))

        Overline("Asesmen Potensi")
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.Top) {
            PotentiaRings(Modifier.size(52.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Heading("Temukan pola potensimu", 21)
                Spacer(Modifier.height(5.dp))
                Text(
                    "±45–55 menit · 47 item pilot · 6 dimensi",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Mulai Asesmen", onClick = onStartAssessment)

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Stone.copy(alpha = .55f))
        Spacer(Modifier.height(20.dp))

        SectionLabel("Profil Terakhir")
        if (assessmentComplete) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onResult)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            latestResult?.let { formatAssessmentDate(it.completedAt) } ?: "Profil terakhir",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (topLabels.isNotBlank()) "Skor tertinggi: $topLabels" else "Hasil pilot tersedia",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        latestScores.take(3).forEach { score ->
                            Row(
                                Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(score.label, color = Muted, fontSize = 10.sp, modifier = Modifier.width(78.dp))
                                LinearProgressIndicator(
                                    progress = { score.value / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = Gold,
                                    trackColor = Stone.copy(alpha = .55f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(score.value.toString(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Muted.copy(alpha = .3f))
                }
                HorizontalDivider(color = Stone.copy(alpha = .42f))
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onResult,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, Gold.copy(alpha = .28f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                ) {
                    Text("Lihat Profil", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PotentiaRings(Modifier.size(64.dp), color = Gold.copy(alpha = .58f))
                Spacer(Modifier.height(14.dp))
                Heading("Belum ada profil potensi", 17)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Selesaikan asesmen pertamamu untuk melihat pola potensimu.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Jelajahi")
        FlatExploreRow("Peta Potensi", "Semua 6 dimensi", onPotentialMap)
        FlatExploreRow("Dimensi Potensi", "Kenali lebih dalam", onLibrary)
        FlatExploreRow(
            "Riwayat Asesmen",
            if (assessmentComplete) "$assessmentCount asesmen tersimpan" else "Belum ada asesmen",
            onHistory,
            showDivider = false
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FlatExploreRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, color = Muted.copy(alpha = .85f), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted.copy(alpha = .3f))
        }
        if (showDivider) HorizontalDivider(color = Stone.copy(alpha = .45f))
    }
}

@Composable
private fun AssessmentIntroScreen(
    draftAvailability: AssessmentDraftAvailability,
    draftAnsweredCount: Int,
    draftQuestionNumber: Int,
    pilotConsentState: PilotConsentState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    val sections = listOf(
        listOf("01", "Penalaran Logis", "10 item · ±9 mnt", "Mengenali pola, aturan, relasi, dan kesimpulan logis."),
        listOf("02", "Kreatif", "3 item · ±8 mnt", "Menghasilkan beberapa ide alternatif melalui jawaban bebas."),
        listOf("03", "Verbal", "8 item · ±8 mnt", "Memahami hubungan makna dan inferensi berbasis bahasa."),
        listOf("04", "Spasial", "8 item · ±9 mnt", "Memahami rotasi dan hubungan visual-spasial."),
        listOf("05", "Sosial", "10 item · ±10 mnt", "Merespons pernyataan dan situasi kerja sama/interpersonal."),
        listOf("06", "Praktis", "8 item · ±10 mnt", "Mengambil keputusan nyata dengan risiko, waktu, dan sumber daya.")
    )

    var showRestartConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showRestartConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestartConfirmation = false },
            title = { Text("Mulai ulang asesmen?") },
            text = {
                Text(
                    "Draf asesmen yang belum selesai akan diganti dengan sesi baru. Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartConfirmation = false
                        onRestart()
                    }
                ) {
                    Text("Mulai Ulang", color = Color(0xFFB5451B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirmation = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(12.dp))
        Overline("Asesmen Potensi · Pilot")
        Heading("Siap menjelajahi\npotensimu?", 28)
        Spacer(Modifier.height(10.dp))
        Text(
            "47 item dalam 6 bagian, estimasi sekitar 45–55 menit. Jawaban disimpan lokal sebagai draf agar sesi dapat dilanjutkan setelah rotasi, keluar aplikasi, atau proses aplikasi dibuat ulang.",
            color = Muted,
            lineHeight = 22.sp,
            fontSize = 14.sp
        )

        when (draftAvailability) {
            AssessmentDraftAvailability.VALID -> {
                Spacer(Modifier.height(18.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Text("Draf asesmen ditemukan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$draftAnsweredCount jawaban tersimpan · posisi terakhir sekitar item $draftQuestionNumber dari 47.",
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            AssessmentDraftAvailability.OUTDATED -> {
                Spacer(Modifier.height(18.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Ada draf dari versi asesmen yang berbeda. Draf lama tidak dapat dilanjutkan dengan item bank saat ini. Potentia tidak akan menghapusnya sampai kamu mengonfirmasi mulai ulang.",
                        modifier = Modifier.padding(15.dp),
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            AssessmentDraftAvailability.CORRUPT -> {
                Spacer(Modifier.height(18.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Draf sesi lama tidak dapat dibaca dengan aman. Draf tersebut belum dihapus. Mulai ulang hanya setelah kamu siap menggantinya dengan sesi baru.",
                        modifier = Modifier.padding(15.dp),
                        color = Color(0xFF8A2F16),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            AssessmentDraftAvailability.NONE -> Unit
        }

        Spacer(Modifier.height(24.dp))

        sections.forEachIndexed { index, section ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Gold.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(section[0], color = Gold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(section[1], fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(section[3], color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Text(section[2], color = Muted.copy(alpha = .7f), fontSize = 10.sp)
            }
            if (index != sections.lastIndex) HorizontalDivider(color = Stone.copy(alpha = .55f))
        }

        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Status: pilot/research. Hasil bukan diagnosis, bukan IQ, bukan percentile/norma populasi. Skor Kreatif memakai AI eksperimental yang belum divalidasi untuk respons Bahasa Indonesia.",
                modifier = Modifier.padding(15.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone),
            shape = RoundedCornerShape(12.dp)
        ) {
            val modeText = when {
                pilotConsentState.requiresDecision ->
                    "Sebelum memulai sesi baru, kamu akan memilih apakah ingin berkontribusi data pilot pseudonim atau menggunakan mode pribadi."
                pilotConsentState.researchEnabled ->
                    "Mode pilot pseudonim aktif untuk sesi baru. Respons per-item dapat disimpan lokal untuk ekspor penelitian manual."
                else ->
                    "Mode pribadi aktif. Respons per-item tidak dipertahankan setelah scoring selesai."
            }
            Text(
                modeText,
                modifier = Modifier.padding(15.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.height(20.dp))

        when (draftAvailability) {
            AssessmentDraftAvailability.VALID -> {
                PrimaryButton("Lanjutkan Asesmen", onClick = onResume)
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    "Mulai Ulang",
                    onClick = { showRestartConfirmation = true }
                )
            }

            AssessmentDraftAvailability.OUTDATED,
            AssessmentDraftAvailability.CORRUPT -> {
                PrimaryButton(
                    "Mulai Ulang",
                    onClick = { showRestartConfirmation = true }
                )
            }

            AssessmentDraftAvailability.NONE -> {
                PrimaryButton("Mulai 47 Item", onClick = onStart)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PilotConsentScreen(
    consentState: PilotConsentState,
    purpose: PilotConsentPurpose,
    onBack: () -> Unit,
    onOptIn: () -> Unit,
    onPersonalOnly: () -> Unit
) {
    var understood by rememberSaveable(consentState.mode) {
        mutableStateOf(consentState.researchEnabled)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(12.dp))
        Overline("Pilot Penelitian · Pilihan Data")
        Heading(
            if (purpose == PilotConsentPurpose.SETTINGS)
                "Partisipasi pilot penelitian"
            else
                "Pilih cara datamu digunakan",
            27
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Pilihan ini tidak memengaruhi akses ke asesmen atau hasil pribadi. POTENTIA tetap dapat digunakan tanpa ikut pengumpulan data pilot.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(22.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Jika ikut pilot pseudonim", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aplikasi menyimpan lokal ID partisipan acak, ID sesi, waktu sesi, versi instrumen, respons per-item (termasuk jawaban bebas), dan hasil dimensi. Nama, email, nomor telepon, dan ID perangkat tidak dikumpulkan oleh fitur ini.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tidak ada unggah otomatis. Data pilot hanya keluar dari perangkat jika kamu sendiri memilih ekspor/bagikan. Kamu dapat menghentikan kontribusi data baru atau menghapus data pilot lokal kapan saja.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Jika memilih mode pribadi", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Hanya hasil dimensi yang disimpan sebagai riwayat lokal. Respons per-item tidak dipertahankan setelah scoring selesai dan tidak masuk ke dataset pilot.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { understood = !understood }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = understood,
                onCheckedChange = { understood = it }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Saya memahami bahwa ini adalah pilot/research, bukan tes psikologis tervalidasi; jawaban bebas dapat tersimpan lokal jika saya memilih ikut pilot; dan persetujuan ini tidak menggantikan persetujuan etik/institusional yang mungkin diperlukan untuk penelitian formal.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOptIn,
            enabled = understood,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Charcoal,
                contentColor = Ivory,
                disabledContainerColor = Stone,
                disabledContentColor = Muted
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (consentState.researchEnabled) "Tetap Ikut Pilot Pseudonim" else "Ikut Pilot Pseudonim",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        GhostButton(
            if (purpose == PilotConsentPurpose.SETTINGS)
                "Gunakan Mode Pribadi"
            else
                "Lanjut Mode Pribadi",
            onClick = onPersonalOnly
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PilotDataScreen(
    consentState: PilotConsentState,
    sessions: List<PilotSessionRecord>,
    onBack: () -> Unit,
    onChangeParticipation: () -> Unit,
    onWithdraw: () -> Unit,
    onDeletePilotData: () -> Unit
) {
    val context = LocalContext.current
    var showWithdrawDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExport by rememberSaveable { mutableStateOf<String?>(null) }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Berhenti berkontribusi data baru?") },
            text = {
                Text(
                    "Sesi asesmen baru tidak akan disimpan sebagai data pilot. Data pilot yang sudah tersimpan lokal tidak dihapus; kamu dapat menghapusnya secara terpisah."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWithdraw()
                        showWithdrawDialog = false
                    }
                ) { Text("Berhenti", color = Color(0xFFB5451B)) }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus semua data pilot lokal?") },
            text = {
                Text(
                    "ID partisipan acak/pseudonim dan seluruh rekaman respons pilot di perangkat ini akan dihapus. Riwayat skor pribadi tetap dipertahankan. Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePilotData()
                        showDeleteDialog = false
                    }
                ) { Text("Hapus Data Pilot", color = Color(0xFFB5451B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    pendingExport?.let { format ->
        AlertDialog(
            onDismissRequest = { pendingExport = null },
            title = { Text("Ekspor data pilot?") },
            text = {
                Text(
                    "Ekspor ini berisi respons per-item dan dapat memuat jawaban bebas. Bagikan hanya ke tujuan penelitian yang kamu percaya. POTENTIA tidak mengunggahnya otomatis."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (format == "json") {
                            PilotExportShare.shareJson(context, consentState, sessions)
                        } else {
                            PilotExportShare.shareCsv(context, sessions)
                        }
                        pendingExport = null
                    }
                ) { Text("Ekspor") }
            },
            dismissButton = {
                TextButton(onClick = { pendingExport = null }) { Text("Batal") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Heading("Privasi & Data Pilot", 27)
        Text(
            "Kontrol partisipasi pilot dan data mentah yang tersimpan lokal di perangkat ini.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(22.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (consentState.researchEnabled) Gold.copy(alpha = .10f) else Color.White
            ),
            border = BorderStroke(1.dp, if (consentState.researchEnabled) Gold.copy(alpha = .35f) else Stone),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Status partisipasi", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        consentState.requiresDecision -> "Perlu keputusan/persetujuan baru"
                        consentState.researchEnabled -> "Pilot pseudonim aktif"
                        else -> "Mode pribadi"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                consentState.participantId?.let { participantId ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ID partisipan: $participantId",
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${sessions.size} sesi pilot tersimpan lokal",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        if (consentState.researchEnabled) {
            GhostButton("Ubah Persetujuan", onClick = onChangeParticipation)
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { showWithdrawDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Berhenti kontribusi data baru", color = Color(0xFFB5451B))
            }
        } else {
            PrimaryButton("Atur Partisipasi Pilot", onClick = onChangeParticipation)
        }

        Spacer(Modifier.height(26.dp))
        SectionLabel("Ekspor penelitian")
        SettingsRow(
            "Ekspor JSON lengkap",
            info = if (sessions.isEmpty()) "Kosong" else "${sessions.size} sesi",
            onClick = { if (sessions.isNotEmpty()) pendingExport = "json" }
        )
        SettingsRow(
            "Ekspor CSV respons item",
            info = if (sessions.isEmpty()) "Kosong" else "Long format",
            onClick = { if (sessions.isNotEmpty()) pendingExport = "csv" }
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("Kontrol data")
        SettingsRow(
            "Hapus data pilot lokal",
            info = if (sessions.isEmpty() && consentState.participantId == null) "Kosong" else null,
            danger = true,
            onClick = {
                if (sessions.isNotEmpty() || consentState.participantId != null) {
                    showDeleteDialog = true
                }
            }
        )

        Spacer(Modifier.height(22.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Data pilot tidak dikirim otomatis dan aplikasi tidak meminta izin INTERNET. Fitur persetujuan ini membantu transparansi di aplikasi, tetapi bukan pengganti telaah etik, informed-consent resmi, atau prosedur institusi jika POTENTIA digunakan dalam penelitian formal.",
                modifier = Modifier.padding(16.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AssessmentLoadErrorScreen(
    message: String,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PotentiaRings(Modifier.size(72.dp), color = Gold.copy(alpha = .55f))
        Spacer(Modifier.height(18.dp))
        Heading("Item bank belum dapat dimuat", 20)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        GhostButton("Kembali", onClick = onBack)
    }
}

@Composable
private fun SpatialAssetImage(asset: String) {
    val context = LocalContext.current
    val bitmap = remember(asset) {
        runCatching {
            context.assets.open("potentia_assessment/$asset").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (bitmap != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone),
            shape = RoundedCornerShape(13.dp)
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Soal spasial",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp, max = 300.dp)
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AssessmentSessionScreen(
    bank: AssessmentBank,
    item: AssessmentItem,
    questionIndex: Int,
    response: String,
    errorMessage: String?,
    onResponse: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val section = bank.sectionFor(item.dimensionId)
    val isLast = questionIndex == bank.items.lastIndex

    QuestionFrame(
        progress = questionIndex + 1,
        section = section?.title ?: (DimensionLabels[item.dimensionId] ?: item.dimensionId),
        trailing = "${questionIndex + 1}/${bank.totalItems}",
        question = item.prompt,
        onBack = onBack,
        total = bank.totalItems
    ) {
        if (!section?.instruction.isNullOrBlank()) {
            Text(
                section?.instruction.orEmpty(),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
        }

        if (item.responseType == "single_choice_image" && item.asset != null) {
            SpatialAssetImage(item.asset)
        }

        when (item.responseType) {
            "single_choice", "single_choice_image", "sjt_single_choice", "likert_1_5" -> {
                item.choices.forEachIndexed { index, choice ->
                    AnswerRow(
                        index = index,
                        text = choice.label,
                        selected = response == choice.value,
                        onClick = { onResponse(choice.value) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            "free_text", "free_text_list" -> {
                val helper = if (item.responseType == "free_text_list") {
                    "Tulis satu ide per baris. Maksimal ${item.maxResponses ?: 3} ide."
                } else {
                    "Tuliskan jawabanmu secara singkat tetapi cukup jelas."
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Skor Kreatif saat ini memakai model AI eksperimental dan belum merupakan norma/diagnosis psikologis.",
                        modifier = Modifier.padding(12.dp),
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = response,
                    onValueChange = onResponse,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (item.responseType == "free_text_list") 6 else 5,
                    maxLines = 10,
                    placeholder = { Text("Tulis jawaban di sini…") },
                    supportingText = { Text(helper) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        focusedLabelColor = Gold,
                        cursorColor = Gold
                    )
                )
            }

            else -> {
                Text(
                    "Tipe respons '${item.responseType}' belum didukung.",
                    color = Color(0xFFB5451B),
                    fontSize = 13.sp
                )
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E2)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Proses skor sebelumnya gagal: $errorMessage",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF8A2F16),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            label = if (isLast) "Selesai & Proses Hasil" else "Lanjut",
            enabled = response.isNotBlank(),
            onClick = onNext
        )
    }
}

@Composable
private fun QuestionFrame(
    progress: Int,
    section: String,
    trailing: String,
    question: String,
    onBack: () -> Unit,
    total: Int = 6,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        TopBack(onBack, trailing)
        ProgressLine(progress, total)
        Spacer(Modifier.height(20.dp))
        Overline(section)
        Spacer(Modifier.height(5.dp))
        Heading(question, 21)
        Spacer(Modifier.height(20.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProcessingScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Charcoal),
        contentAlignment = Alignment.Center
    ) {
        PotentiaRings(
            Modifier.size(290.dp),
            color = Gold.copy(alpha = .65f),
            animated = true
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(80.dp))
            Heading("Menyusun profil\npotensimu…", 23, Ivory)
            Spacer(Modifier.height(8.dp))
            Text("Membaca pola respons", color = Ivory.copy(alpha = .42f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ResultOverviewScreen(
    result: AssessmentResult?,
    onMap: () -> Unit,
    onDetail: (String) -> Unit
) {
    if (result == null) {
        Column(
            Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PotentiaRings(Modifier.size(72.dp), color = Gold.copy(alpha = .5f))
            Spacer(Modifier.height(16.dp))
            Heading("Belum ada hasil asesmen", 20)
            Spacer(Modifier.height(6.dp))
            Text("Selesaikan asesmen terlebih dahulu untuk melihat profil potensimu.", color = Muted, textAlign = TextAlign.Center)
        }
        return
    }

    val scores = result.toPotentialScores()
    val ranked = DimensionOrder.mapNotNull { id ->
        result.dimensions[id]?.score?.let { id to it }
    }.sortedByDescending { it.second }
    val creative = result.dimensions["creative"]

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Overline("Profil Potensi · Pilot")
        Heading(formatAssessmentDate(result.completedAt), 28)
        Text("Berdasarkan respons yang tersimpan pada sesi asesmen ini", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        RadarChart(scores, Modifier.fillMaxWidth().height(290.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .09f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Indeks 0–100 pada layar ini adalah transformasi scoring pilot V4.1, bukan percentile, norma populasi, IQ, atau diagnosis psikologis.",
                modifier = Modifier.padding(14.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        if (creative?.experimental == true) {
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (creative.outOfDomain) {
                        "Skor Kreatif menggunakan model AI eksperimental. Respons Potentia Bahasa Indonesia berada di luar domain training model (Cambridge AUT berbahasa Inggris), sehingga skor ini belum boleh dianggap hasil kreativitas tervalidasi."
                    } else {
                        "Skor Kreatif menggunakan model AI eksperimental dan belum tervalidasi sebagai ukuran psikometrik."
                    },
                    modifier = Modifier.padding(14.dp),
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Indeks Tertinggi pada Sesi Ini")
        ranked.take(2).forEachIndexed { index, pair ->
            val id = pair.first
            val value = pair.second.roundToInt()
            val info = DimensionInfos[id]
            ResultHighlight(
                DimensionLabels[id] ?: id,
                "Indeks $value/100",
                info?.description ?: "Dimensi pilot POTENTIA."
            )
            if (index == 0 && ranked.size > 1) Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(22.dp))
        PrimaryButton("Lihat Peta Lengkap", onClick = onMap)
        Spacer(Modifier.height(10.dp))
        GhostButton(
            "Jelajahi Detail Dimensi",
            onClick = { onDetail(ranked.firstOrNull()?.first ?: "logical") }
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ResultHighlight(title: String, tag: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Stone, RoundedCornerShape(12.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(tag, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun RadarChart(
    data: List<PotentialScore>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val n = data.size
        val c = center
        val r = size.minDimension * .33f

        fun point(index: Int, value: Float): Offset {
            val angle = (2.0 * PI * index / n - PI / 2.0).toFloat()
            val d = r * value
            return Offset(
                c.x + d * cos(angle),
                c.y + d * sin(angle)
            )
        }

        repeat(5) { level ->
            val rr = (level + 1) / 5f
            val path = Path()
            repeat(n) { i ->
                val p = point(i, rr)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, Stone.copy(alpha = .8f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }

        repeat(n) { i ->
            val p = point(i, 1f)
            drawLine(Stone, c, p, 1.dp.toPx())
        }

        val fillPath = Path()
        data.forEachIndexed { i, d ->
            val p = point(i, d.value / 100f)
            if (i == 0) fillPath.moveTo(p.x, p.y) else fillPath.lineTo(p.x, p.y)
        }
        fillPath.close()
        drawPath(fillPath, Gold.copy(alpha = .14f))
        drawPath(fillPath, Gold, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        data.forEachIndexed { i, d ->
            val p = point(i, d.value / 100f)
            drawCircle(Gold, 4.dp.toPx(), p)
        }

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(80, 78, 72)
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            data.forEachIndexed { i, d ->
                val p = point(i, 1.20f)
                canvas.nativeCanvas.drawText(d.label, p.x, p.y + 4.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun PotentialDetailScreen(
    result: AssessmentResult?,
    dimensionId: String,
    onBack: () -> Unit
) {
    val label = DimensionLabels[dimensionId] ?: dimensionId
    val info = DimensionInfos[dimensionId]
    val dimensionResult = result?.dimensions?.get(dimensionId)
    val score = dimensionResult?.score?.roundToInt()?.coerceIn(0, 100)
    val scoreLabel = when {
        score == null -> "Belum ada skor yang cukup"
        score >= 75 -> "Indeks relatif tinggi pada sesi ini"
        score >= 60 -> "Indeks menengah pada sesi ini"
        else -> "Area yang dapat dieksplorasi"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            TopBack(onBack)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(Charcoal)
                .padding(24.dp)
        ) {
            PotentiaRings(
                Modifier.size(170.dp).align(Alignment.CenterEnd),
                color = Ivory.copy(alpha = .10f)
            )
            Column {
                Text("DIMENSI POTENSI · PILOT", color = Ivory.copy(alpha = .42f), fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Heading(label, 30, Ivory)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(score?.toString() ?: "—", color = Gold, fontFamily = FontFamily.Serif, fontSize = 58.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text("Indeks pilot 0–100", color = Ivory.copy(alpha = .45f), fontSize = 12.sp)
                        Text(scoreLabel, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(Modifier.padding(22.dp)) {
            if (score != null) {
                SectionLabel("Ringkasan Skor")
                ScoreBar(label, score, "Transformasi skor pilot; bukan percentile/norma populasi")
                Spacer(Modifier.height(12.dp))
                Text(
                    "Respons terhitung: ${dimensionResult?.answered ?: 0}/${dimensionResult?.expected ?: 0}",
                    color = Muted,
                    fontSize = 12.sp
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E2)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Skor belum ditampilkan karena respons yang valid belum memenuhi ambang minimum.",
                        modifier = Modifier.padding(14.dp),
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            if (dimensionId == "creative" && dimensionResult?.experimental == true) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .09f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (dimensionResult.outOfDomain) {
                            "AI Kreatif aktif, tetapi respons ini out-of-domain terhadap data training berbahasa Inggris. Gunakan hanya sebagai sinyal eksperimen."
                        } else {
                            "AI Kreatif aktif dan masih berstatus eksperimen."
                        },
                        modifier = Modifier.padding(14.dp),
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("Tentang Dimensi Ini")
            Text(
                info?.description ?: "Dimensi pilot POTENTIA untuk eksplorasi diri.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(26.dp))
            SectionLabel("Ide Pengembangan")
            info?.tips.orEmpty().forEach { DevelopmentTip(it) }

            Spacer(Modifier.height(8.dp))
            Text(
                "Jangan membaca skor sebagai kemampuan absolut atau label permanen. Interpretasi dapat berubah setelah instrumen diuji pada responden nyata.",
                color = Muted.copy(alpha = .8f),
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DevelopmentTip(text: String) {
    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Gold.copy(alpha = .12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = Gold, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Charcoal, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun PotentialMapScreen(
    result: AssessmentResult?,
    onBack: () -> Unit,
    onDetail: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Overline(result?.let { formatAssessmentDate(it.completedAt) } ?: "Profil Potensi")
        Heading("Peta Potensi", 27)
        Spacer(Modifier.height(20.dp))

        if (result == null) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PotentiaRings(Modifier.size(64.dp), color = Gold.copy(alpha = .45f))
                Spacer(Modifier.height(12.dp))
                Text("Belum ada hasil asesmen.", color = Muted)
            }
        } else {
            DimensionOrder.forEach { id ->
                val dimension = result.dimensions[id]
                val value = dimension?.score?.roundToInt()?.coerceIn(0, 100)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDetail(id) }
                        .padding(vertical = 14.dp)
                ) {
                    if (value != null) {
                        ScoreBar(
                            DimensionLabels[id] ?: id,
                            value,
                            when {
                                value >= 75 -> "Indeks relatif tinggi pada sesi ini"
                                value >= 60 -> "Indeks menengah pada sesi ini"
                                else -> "Area yang dapat dieksplorasi"
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(DimensionLabels[id] ?: id, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("Belum cukup respons", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                HorizontalDivider(color = Stone.copy(alpha = .55f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Semua indeks masih berstatus pilot/research. Nilai 0–100 bukan percentile, norma populasi, atau ukuran kemampuan absolut.",
                modifier = Modifier.padding(15.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GrowthScreen(
    result: AssessmentResult?,
    recommendationsEnabled: Boolean
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("potentia_prefs", Context.MODE_PRIVATE)
    }

    val focus = result?.let { assessment ->
        DimensionOrder.mapNotNull { id ->
            assessment.dimensions[id]?.score?.let { id to it }
        }.minByOrNull { it.second }
    }
    val focusId = focus?.first
    val focusLabel = focusId?.let { DimensionLabels[it] } ?: "Belum ditentukan"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Overline("Minggu ini")
        Heading("Eksplorasi &\nRefleksi", 27)
        Spacer(Modifier.height(22.dp))

        if (!recommendationsEnabled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Charcoal)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "REKOMENDASI DINONAKTIFKAN",
                        color = Ivory.copy(alpha = .45f),
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Heading("Mode Tumbuh sedang nonaktif", 19, Ivory)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Aktifkan kembali melalui Profil → Pengaturan → Rekomendasi pengembangan.",
                        color = Ivory.copy(alpha = .62f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Hasil asesmenmu tetap tersimpan. Pengaturan ini hanya mengatur apakah latihan dan refleksi ditampilkan.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            return@Column
        }

        if (focus == null || focusId == null) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PotentiaRings(Modifier.size(70.dp), color = Gold.copy(alpha = .5f))
                Spacer(Modifier.height(14.dp))
                Heading("Selesaikan asesmen dulu", 18)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Setelah asesmen selesai, Potentia akan menawarkan aktivitas untuk mengeksplorasi cara kamu berpikir dan mengambil keputusan.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val exercises = remember(focusId) {
            GrowthCatalog.forDimension(focusId)
        }
        val weekKey = remember {
            GrowthStorage.currentWeekKey()
        }
        var progress by remember(focusId, weekKey) {
            mutableStateOf(GrowthStorage.load(prefs, weekKey, focusId))
        }

        fun saveProgress(next: GrowthProgress) {
            progress = next.copy(updatedAt = System.currentTimeMillis())
            GrowthStorage.save(prefs, progress)
        }

        val selectedExercise = exercises.firstOrNull { it.id == progress.selectedExerciseId }
        val activeExercise = exercises.firstOrNull { it.id == progress.activeExerciseId }
        val selectedRecord = selectedExercise?.let { progress.practiceRecords[it.id] }
        val completedCount = progress.completedExerciseIds.count { completedId ->
            exercises.any { it.id == completedId }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Charcoal)
                .padding(20.dp)
        ) {
            PotentiaRings(
                Modifier.size(130.dp).align(Alignment.BottomEnd),
                color = Ivory.copy(alpha = .10f)
            )
            Column(Modifier.fillMaxWidth(.82f)) {
                Text(
                    "FOKUS EKSPLORASI",
                    color = Ivory.copy(alpha = .42f),
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp
                )
                Heading(focusLabel, 21, Ivory)
                Text(
                    "Berdasarkan pola respons pada sesi terakhirmu, area ini bisa dipakai sebagai bahan eksplorasi. Tujuannya bukan mengejar angka, tetapi mengamati cara kamu berpikir dan bertindak.",
                    color = Ivory.copy(alpha = .58f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$completedCount/${exercises.size} aktivitas dijalani minggu ini",
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .08f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Potentia tidak dapat memastikan apakah refleksi dilakukan dengan jujur. Sistem hanya memverifikasi interaksi yang diminta. Catatan aktivitas tidak berarti kemampuanmu meningkat.",
                modifier = Modifier.padding(14.dp),
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Pilih Aktivitas")
        Text(
            "Mini Challenge memiliki jawaban yang dapat diperiksa sistem. Refleksi Terpandu meminta kamu menuliskan pengalaman atau cara berpikirmu sebelum aktivitas dicatat.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(10.dp))

        exercises.forEachIndexed { index, exercise ->
            GrowthExerciseCard(
                exercise = exercise,
                number = index + 1,
                selected = progress.selectedExerciseId == exercise.id,
                active = progress.activeExerciseId == exercise.id,
                completed = exercise.id in progress.completedExerciseIds,
                enabled = progress.activeExerciseId == null || progress.activeExerciseId == exercise.id,
                onSelect = {
                    saveProgress(
                        progress.copy(
                            selectedExerciseId = exercise.id
                        )
                    )
                }
            )
            if (index != exercises.lastIndex) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Latihan & Refleksi")

        GrowthChallengePanel(
            focusLabel = focusLabel,
            selectedExercise = selectedExercise,
            activeExercise = activeExercise,
            selectedRecord = selectedRecord,
            onStart = {
                val exercise = selectedExercise ?: return@GrowthChallengePanel
                saveProgress(
                    progress.copy(
                        selectedExerciseId = exercise.id,
                        activeExerciseId = exercise.id
                    )
                )
            },
            onPracticeCompleted = { record ->
                val exercise = activeExercise ?: return@GrowthChallengePanel
                saveProgress(
                    progress.copy(
                        selectedExerciseId = exercise.id,
                        activeExerciseId = null,
                        completedExerciseIds = progress.completedExerciseIds + exercise.id,
                        practiceRecords = progress.practiceRecords + (exercise.id to record)
                    )
                )
            }
        )

        Spacer(Modifier.height(14.dp))
        Text(
            "Aktivitas ini bersifat reflektif dan edukatif, bukan intervensi psikologis. Potentia mencatat bahwa kamu telah berinteraksi dengan latihan; aplikasi tidak mengklaim bahwa skor atau kemampuanmu berubah karena satu aktivitas.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GrowthExerciseCard(
    exercise: GrowthExercise,
    number: Int,
    selected: Boolean,
    active: Boolean,
    completed: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = when {
        completed -> Success.copy(alpha = .65f)
        selected -> Gold
        else -> Stone
    }
    val backgroundColor = when {
        selected -> Gold.copy(alpha = .06f)
        else -> Color.White
    }
    val modeLabel = when (exercise.mode) {
        GrowthExerciseMode.INTERACTIVE_CHALLENGE -> "MINI CHALLENGE"
        GrowthExerciseMode.GUIDED_REFLECTION -> "REFLEKSI TERPANDU"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        completed -> Success.copy(alpha = .12f)
                        selected -> Gold.copy(alpha = .18f)
                        else -> Gold.copy(alpha = .10f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    number.toString(),
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.title,
                    color = Charcoal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (active) {
                    Text(
                        "AKTIF",
                        color = Gold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .8.sp
                    )
                } else if (completed) {
                    Text(
                        "DIREKAM",
                        color = Success,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .8.sp
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                modeLabel,
                color = if (exercise.mode == GrowthExerciseMode.INTERACTIVE_CHALLENGE) Gold else Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                exercise.summary,
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "± ${exercise.estimatedMinutes} menit",
                color = Muted.copy(alpha = .8f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun GrowthChallengePanel(
    focusLabel: String,
    selectedExercise: GrowthExercise?,
    activeExercise: GrowthExercise?,
    selectedRecord: GrowthPracticeRecord?,
    onStart: () -> Unit,
    onPracticeCompleted: (GrowthPracticeRecord) -> Unit
) {
    val exerciseToShow = activeExercise ?: selectedExercise

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Stone, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        when {
            exerciseToShow == null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Explore, null, tint = Gold)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Belum ada aktivitas dipilih", fontWeight = FontWeight.Bold)
                        Text(
                            "Pilih satu kartu di atas untuk melihat latihan dan refleksinya.",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text("Mulai")
                }
            }

            activeExercise != null -> {
                GrowthActivePractice(
                    focusLabel = focusLabel,
                    exercise = activeExercise,
                    onPracticeCompleted = onPracticeCompleted
                )
            }

            selectedRecord != null -> {
                GrowthCompletedPractice(
                    exercise = exerciseToShow,
                    record = selectedRecord,
                    onRepeat = onStart
                )
            }

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PlayCircleOutline, null, tint = Gold)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (exerciseToShow.mode == GrowthExerciseMode.INTERACTIVE_CHALLENGE) "Mini Challenge" else "Refleksi Terpandu",
                            color = Gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(exerciseToShow.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("± ${exerciseToShow.estimatedMinutes} menit · $focusLabel", color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    exerciseToShow.summary,
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    when (exerciseToShow.mode) {
                        GrowthExerciseMode.INTERACTIVE_CHALLENGE ->
                            "Sistem akan memeriksa jawabanmu dan memberi umpan balik. Ini latihan, bukan pengukuran ulang kemampuan."
                        GrowthExerciseMode.GUIDED_REFLECTION ->
                            "Kamu perlu menuliskan respons refleksi sebelum aktivitas dapat disimpan."
                    },
                    color = Charcoal,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Icon(Icons.Outlined.PlayCircleOutline, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Mulai Aktivitas")
                }
            }
        }
    }
}

@Composable
private fun GrowthActivePractice(
    focusLabel: String,
    exercise: GrowthExercise,
    onPracticeCompleted: (GrowthPracticeRecord) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Explore, null, tint = Gold)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (exercise.mode == GrowthExerciseMode.INTERACTIVE_CHALLENGE) "Mini Challenge aktif" else "Refleksi aktif",
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(exercise.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("± ${exercise.estimatedMinutes} menit · $focusLabel", color = Muted, fontSize = 11.sp)
        }
    }

    Spacer(Modifier.height(14.dp))
    Text("Langkah aktivitas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Charcoal)
    Spacer(Modifier.height(8.dp))
    exercise.steps.forEachIndexed { index, step ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 9.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = .13f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (index + 1).toString(),
                    color = Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                step,
                color = Charcoal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    when (exercise.mode) {
        GrowthExerciseMode.GUIDED_REFLECTION -> {
            GrowthReflectionForm(
                exercise = exercise,
                onSave = { responses ->
                    onPracticeCompleted(
                        GrowthPracticeRecord(
                            exerciseId = exercise.id,
                            responses = responses,
                            verifiedInteraction = false,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                }
            )
        }

        GrowthExerciseMode.INTERACTIVE_CHALLENGE -> {
            GrowthInteractiveChallenge(
                exercise = exercise,
                onVerified = { answer ->
                    onPracticeCompleted(
                        GrowthPracticeRecord(
                            exerciseId = exercise.id,
                            responses = mapOf("answer" to answer),
                            verifiedInteraction = true,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun GrowthReflectionForm(
    exercise: GrowthExercise,
    onSave: (Map<String, String>) -> Unit
) {
    val answers = remember(exercise.id) {
        mutableStateMapOf<String, String>().apply {
            exercise.reflectionPrompts.forEach { put(it.id, "") }
        }
    }
    val valid = exercise.reflectionPrompts.isNotEmpty() &&
        exercise.reflectionPrompts.all { prompt ->
            answers[prompt.id].orEmpty().trim().length >= prompt.minChars
        }

    Text(
        "Catatan refleksi",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Charcoal
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Tidak ada jawaban benar atau salah. Tulis secukupnya agar kamu bisa melihat kembali cara berpikirmu.",
        color = Muted,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
    Spacer(Modifier.height(10.dp))

    exercise.reflectionPrompts.forEach { prompt ->
        Text(
            prompt.label,
            color = Charcoal,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = answers[prompt.id].orEmpty(),
            onValueChange = { answers[prompt.id] = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(prompt.placeholder, fontSize = 11.sp, color = Muted.copy(alpha = .7f))
            },
            minLines = 2,
            maxLines = 5,
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(10.dp))
    }

    Button(
        onClick = {
            onSave(
                exercise.reflectionPrompts.associate { prompt ->
                    prompt.id to answers[prompt.id].orEmpty().trim()
                }
            )
        },
        enabled = valid,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Success),
        shape = RoundedCornerShape(9.dp)
    ) {
        Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text("Simpan Refleksi")
    }
}

@Composable
private fun GrowthInteractiveChallenge(
    exercise: GrowthExercise,
    onVerified: (String) -> Unit
) {
    val challenge = exercise.challenge ?: return
    var selectedValue by remember(exercise.id) { mutableStateOf<String?>(null) }
    var retryMessage by remember(exercise.id) { mutableStateOf<String?>(null) }

    Text(
        "Soal latihan",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Charcoal
    )
    Spacer(Modifier.height(8.dp))
    Text(
        challenge.prompt,
        color = Charcoal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(10.dp))

    challenge.choices.forEach { choice ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .clickable {
                    selectedValue = choice.value
                    retryMessage = null
                }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedValue == choice.value,
                onClick = {
                    selectedValue = choice.value
                    retryMessage = null
                }
            )
            Text(
                choice.label,
                color = Charcoal,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }

    retryMessage?.let { message ->
        Spacer(Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .08f)),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(
                message,
                modifier = Modifier.padding(11.dp),
                color = Charcoal,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = {
            val selected = selectedValue ?: return@Button
            if (selected == challenge.correctValue) {
                onVerified(selected)
            } else {
                retryMessage = "Belum tepat. ${challenge.retryHint}"
            }
        },
        enabled = selectedValue != null,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold),
        shape = RoundedCornerShape(9.dp)
    ) {
        Icon(Icons.Outlined.FactCheck, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text("Periksa Jawaban")
    }
}

@Composable
private fun GrowthCompletedPractice(
    exercise: GrowthExercise,
    record: GrowthPracticeRecord,
    onRepeat: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CheckCircle, null, tint = Success)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (record.verifiedInteraction) "Challenge terverifikasi" else "Refleksi tersimpan",
                color = Success,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(exercise.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Aktivitas tercatat minggu ini. Ini bukan bukti bahwa kemampuan meningkat.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    if (exercise.mode == GrowthExerciseMode.INTERACTIVE_CHALLENGE) {
        val challenge = exercise.challenge
        if (challenge != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = .08f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Mengapa jawaban ini tepat?",
                        color = Success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        challenge.successExplanation,
                        color = Charcoal,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    } else {
        Text(
            "Catatanmu",
            color = Charcoal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        exercise.reflectionPrompts.forEach { prompt ->
            val value = record.responses[prompt.id].orEmpty()
            if (value.isNotBlank()) {
                Text(
                    prompt.label,
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    value,
                    color = Charcoal,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        TextButton(onClick = onRepeat) {
            Text("Ulangi aktivitas", color = Gold)
        }
    }
}

@Composable
private fun HistoryScreen(
    history: List<AssessmentResult>,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onResult: (Long) -> Unit,
    onCompare: () -> Unit
) {
    val ordered = history.sortedByDescending { it.completedAt }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Heading("Riwayat Asesmen", 27)
        Text(
            "Hasil asesmen yang selesai disimpan lokal di perangkat ini.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(22.dp))

        if (ordered.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PotentiaRings(Modifier.size(60.dp), color = Gold.copy(alpha = .48f))
                Spacer(Modifier.height(14.dp))
                Heading("Belum ada riwayat asesmen", 17)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Asesmen yang telah kamu selesaikan akan muncul di sini.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Mulai Asesmen", onClick = onStartAssessment)
            }
        } else {
            ordered.forEachIndexed { index, assessment ->
                val values = assessment.dimensions.values.mapNotNull { it.score }
                val average = values.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
                val topLabels = DimensionOrder.mapNotNull { id ->
                    assessment.dimensions[id]?.score?.let { id to it }
                }.sortedByDescending { it.second }
                    .take(2)
                    .joinToString(" & ") { (id, _) -> DimensionLabels[id] ?: id }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onResult(assessment.completedAt) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)).background(Gold.copy(alpha = .12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(average?.toString() ?: "—", fontFamily = FontFamily.Serif, fontSize = 17.sp, color = Gold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatAssessmentDate(assessment.completedAt), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (index == 0) {
                                Spacer(Modifier.width(8.dp))
                                Text("TERBARU", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            if (topLabels.isBlank()) "Hasil pilot" else "Indeks tertinggi: $topLabels",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted.copy(alpha = .3f))
                }
                if (index != ordered.lastIndex) HorizontalDivider(color = Stone.copy(alpha = .45f))
            }

            Spacer(Modifier.height(18.dp))
            if (ordered.size >= 2) {
                GhostButton("Bandingkan 2 Asesmen Terbaru →", onClick = onCompare)
            } else {
                Text(
                    "Selesaikan minimal dua asesmen untuk melihat perbandingan.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ComparisonScreen(
    history: List<AssessmentResult>,
    onBack: () -> Unit
) {
    val ordered = history.sortedByDescending { it.completedAt }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Heading("Perbandingan Asesmen", 27)

        if (ordered.size < 2) {
            Spacer(Modifier.height(8.dp))
            Text("Diperlukan minimal dua asesmen yang selesai.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            PotentiaRings(Modifier.size(72.dp).align(Alignment.CenterHorizontally), color = Gold.copy(alpha = .45f))
            return@Column
        }

        val current = ordered[0]
        val previous = ordered[1]
        Text(
            "${formatAssessmentDate(current.completedAt)} dibanding ${formatAssessmentDate(previous.completedAt)}",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))

        DimensionOrder.forEach { id ->
            val currentRaw = current.dimensions[id]?.score
            val previousRaw = previous.dimensions[id]?.score
            val currentValue = currentRaw?.roundToInt()?.coerceIn(0, 100)
            val previousValue = previousRaw?.roundToInt()?.coerceIn(0, 100)
            val diff = if (currentValue != null && previousValue != null) currentValue - previousValue else null

            Column(Modifier.padding(bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DimensionLabels[id] ?: id, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(previousValue?.toString() ?: "—", color = Muted, fontSize = 12.sp)
                    Text("  →  ", color = Muted.copy(alpha = .55f))
                    Text(currentValue?.toString() ?: "—", color = Gold, fontFamily = FontFamily.Serif, fontSize = 17.sp)
                    if (diff != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (diff >= 0) "+$diff" else "$diff",
                            color = if (diff >= 0) Success else Color(0xFFB5451B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Stone.copy(alpha = .55f))
                ) {
                    if (previousValue != null) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(previousValue / 100f).background(Muted.copy(alpha = .25f))
                        )
                    }
                    if (currentValue != null) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(currentValue / 100f).padding(vertical = 1.5.dp).clip(CircleShape).background(Gold)
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone)
        ) {
            Text(
                "Selisih antar-sesi tidak membuktikan peningkatan atau penurunan kemampuan absolut. Kondisi saat menjawab, pemahaman item, dan status instrumen yang masih pilot dapat memengaruhi hasil.",
                modifier = Modifier.padding(16.dp),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun DimensionLibraryScreen(
    onBack: () -> Unit,
    onDetail: (String) -> Unit
) {
    val symbols = mapOf(
        "logical" to "◈",
        "creative" to "◌",
        "verbal" to "◎",
        "spatial" to "⬡",
        "social" to "○",
        "practical" to "⊕"
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Heading("Dimensi Potensi", 27)
        Text(
            "Enam dimensi ini dipakai sebagai bahasa eksplorasi pada pilot POTENTIA, bukan kategori manusia yang eksklusif.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(20.dp))

        DimensionOrder.forEach { id ->
            val title = DimensionLabels[id] ?: id
            val description = DimensionInfos[id]?.description.orEmpty()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Stone, RoundedCornerShape(12.dp))
                    .clickable { onDetail(id) }
                    .padding(15.dp)
            ) {
                Box(
                    Modifier.size(43.dp).clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(symbols[id] ?: "•", color = Gold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(description, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ProfileScreen(
    assessmentComplete: Boolean,
    assessmentCount: Int,
    onHistory: () -> Unit,
    onComparison: () -> Unit,
    onResult: () -> Unit,
    onSettings: () -> Unit,
    onPrivacyData: () -> Unit,
    onAbout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(Gold.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                PotentiaRings(Modifier.size(38.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Heading("Pengguna", 21)
                Text(
                    if (assessmentComplete) "$assessmentCount asesmen selesai" else "Belum ada asesmen",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Asesmen")
        SettingsRow("Riwayat Asesmen", onClick = onHistory)
        if (assessmentCount >= 2) {
            SettingsRow("Perbandingan Kemajuan", onClick = onComparison)
        }
        if (assessmentComplete) {
            SettingsRow("Profil Terbaru", onClick = onResult)
        }
        SettingsRow("Cara Membaca Hasil", onClick = onAbout)

        Spacer(Modifier.height(24.dp))
        SectionLabel("Preferensi & Data")
        SettingsRow("Pengaturan", onClick = onSettings)
        SettingsRow("Privasi & Data", onClick = onPrivacyData)

        Spacer(Modifier.height(24.dp))
        SectionLabel("Lainnya")
        SettingsRow("Tentang POTENTIA", onClick = onAbout)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsRow(
    label: String,
    info: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (danger) Color(0xFFB5451B) else Charcoal,
            fontWeight = FontWeight.Medium
        )
        if (info != null) Text(info, color = Muted, fontSize = 12.sp)
        else Icon(Icons.Default.ChevronRight, null, tint = Muted.copy(alpha = .35f))
    }
    HorizontalDivider(color = Stone.copy(alpha = .55f))
}

@Composable
private fun SettingsScreen(
    history: List<AssessmentResult>,
    recommendationsEnabled: Boolean,
    onRecommendationsEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAbout: () -> Unit,
    onPilotData: () -> Unit,
    onDeleteHistory: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("potentia_prefs", Context.MODE_PRIVATE) }
    var reminder by remember { mutableStateOf(prefs.getBoolean("reminder_weekly", false)) }
    var reminderPermissionDenied by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        reminderPermissionDenied = !granted
        reminder = granted
        prefs.edit().putBoolean("reminder_weekly", granted).apply()
        WeeklyReminderScheduler.sync(context.applicationContext, granted)
    }

    fun updateReminder(enabled: Boolean) {
        reminderPermissionDenied = false

        if (!enabled) {
            reminder = false
            prefs.edit().putBoolean("reminder_weekly", false).apply()
            WeeklyReminderScheduler.cancel(context.applicationContext)
            return
        }

        if (WeeklyReminderScheduler.canPostNotifications(context)) {
            reminder = true
            prefs.edit().putBoolean("reminder_weekly", true).apply()
            WeeklyReminderScheduler.schedule(context.applicationContext)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminder = true
            prefs.edit().putBoolean("reminder_weekly", true).apply()
            WeeklyReminderScheduler.schedule(context.applicationContext)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus riwayat asesmen?") },
            text = { Text("Riwayat skor pribadi dan progres Tumbuh akan dihapus. Data pilot mentah, jika ada, tidak ikut terhapus dan dikelola terpisah melalui Privasi & Data. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteHistory()
                        showDeleteDialog = false
                    }
                ) { Text("Hapus", color = Color(0xFFB5451B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Heading("Pengaturan", 27)

        Spacer(Modifier.height(24.dp))
        SectionLabel("Preferensi")
        ToggleRow("Pengingat latihan mingguan", reminder) { enabled ->
            updateReminder(enabled)
        }
        Text(
            WeeklyReminderPolicy.DISPLAY_LABEL + " · notifikasi membuka tab Tumbuh.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (reminderPermissionDenied) {
            Text(
                "Izin notifikasi belum diberikan. Pengingat tetap nonaktif sampai izin disetujui.",
                color = Color(0xFFB5451B),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        ToggleRow("Rekomendasi pengembangan", recommendationsEnabled) {
            onRecommendationsEnabledChange(it)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Data")
        SettingsRow(
            "Data pilot penelitian",
            onClick = onPilotData
        )
        SettingsRow(
            "Ekspor data saya",
            info = if (history.isEmpty()) "Kosong" else "${history.size} asesmen",
            onClick = {
                val payload = AssessmentStorage.exportJson(history)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Data asesmen POTENTIA")
                    putExtra(Intent.EXTRA_TEXT, payload)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Ekspor data POTENTIA"))
            }
        )
        SettingsRow("Hapus riwayat asesmen", danger = true, onClick = { showDeleteDialog = true })

        Spacer(Modifier.height(24.dp))
        SectionLabel("Tentang")
        SettingsRow("Tentang POTENTIA", onClick = onAbout)
        SettingsRow("Versi", info = "0.1.0 · Core V2", onClick = {})
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ivory,
                checkedTrackColor = Gold
            )
        )
    }
    HorizontalDivider(color = Stone.copy(alpha = .55f))
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(8.dp))
        PotentiaRings(
            Modifier
                .size(88.dp)
                .align(Alignment.CenterHorizontally)
        )
        Heading("Tentang POTENTIA", 27, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))

        ArticleSection(
            "Apa itu POTENTIA?",
            "POTENTIA adalah alat eksplorasi diri untuk membantu pengguna mengenali kecenderungan berpikir dan cara memproses informasi. Aplikasi ini bukan alat diagnosis psikologis profesional."
        )
        ArticleSection(
            "Bukan tes IQ atau diagnosis",
            "Hasil tidak mewakili IQ, bakat bawaan, atau kondisi psikologis. Indeks 0–100 sekarang dihitung dari scoring pilot V4.1, tetapi instrumennya belum tervalidasi secara psikometrik dan bukan norma populasi."
        )
        ArticleSection(
            "Potensi dapat berkembang",
            "Dimensi yang digunakan dirancang sebagai bahasa refleksi. Pengalaman, pembelajaran, latihan, dan konteks dapat memengaruhi cara seseorang merespons."
        )
        ArticleSection(
            "Privasi data",
            "Riwayat hasil asesmen disimpan lokal di perangkat. Dalam mode pribadi, jawaban per-item tidak dipertahankan setelah scoring selesai. Jika kamu secara eksplisit memilih ikut pilot penelitian, respons per-item (termasuk jawaban bebas) disimpan lokal dengan ID partisipan acak/pseudonim untuk ekspor manual. Tidak ada unggah otomatis ke server. Model Kreatif tetap berjalan on-device."
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Untuk asesmen psikologis profesional, gunakan layanan dan instrumen yang dikelola oleh tenaga profesional yang berwenang.",
                modifier = Modifier.padding(16.dp),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ArticleSection(title: String, body: String) {
    Heading(title, 18)
    Spacer(Modifier.height(7.dp))
    Text(body, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
    Spacer(Modifier.height(22.dp))
}
