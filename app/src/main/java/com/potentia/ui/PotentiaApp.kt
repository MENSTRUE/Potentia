package com.potentia.ui

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.potentia.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class Screen {
    SPLASH,
    ONBOARDING_1, ONBOARDING_2, ONBOARDING_3,
    HOME, ASSESSMENT_INTRO,
    Q_SITUATIONAL, Q_LOGIC, Q_PATTERN, Q_SPATIAL, Q_CREATIVE, Q_REFLECTION,
    PROCESSING, RESULT_OVERVIEW, POTENTIAL_DETAIL, POTENTIAL_MAP,
    GROWTH, HISTORY, COMPARISON, DIMENSION_LIBRARY,
    PROFILE, SETTINGS, ABOUT
}

private enum class MainTab { HOME, ASSESSMENT, GROWTH, PROFILE }

private data class PotentialScore(
    val label: String,
    val value: Int
)

private val DemoScores = listOf(
    PotentialScore("Penalaran Logis", 82),
    PotentialScore("Kreatif", 71),
    PotentialScore("Verbal", 65),
    PotentialScore("Spasial", 78),
    PotentialScore("Sosial", 58),
    PotentialScore("Praktis", 74),
)

private val DarkScreens = setOf(Screen.SPLASH, Screen.PROCESSING)
private val NoBottomBarScreens = setOf(
    Screen.SPLASH, Screen.ONBOARDING_1, Screen.ONBOARDING_2, Screen.ONBOARDING_3,
    Screen.Q_SITUATIONAL, Screen.Q_LOGIC, Screen.Q_PATTERN,
    Screen.Q_SPATIAL, Screen.Q_CREATIVE, Screen.Q_REFLECTION, Screen.PROCESSING
)

@Composable
fun PotentiaApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("potentia_prefs", Context.MODE_PRIVATE)
    }

    var screen by remember { mutableStateOf(Screen.SPLASH) }
    var activeTab by remember { mutableStateOf(MainTab.HOME) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var reflectionScore by remember { mutableStateOf<Int?>(null) }
    var assessmentComplete by remember {
        mutableStateOf(prefs.getBoolean("assessment_complete", false))
    }
    val onboardingComplete = remember {
        prefs.getBoolean("onboarding_complete", false)
    }

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
        when (screen) {
            Screen.SPLASH -> {
                delay(1500)
                screen = if (onboardingComplete) Screen.HOME else Screen.ONBOARDING_1
            }
            Screen.PROCESSING -> {
                delay(2200)
                assessmentComplete = true
                prefs.edit().putBoolean("assessment_complete", true).apply()
                screen = Screen.RESULT_OVERVIEW
                activeTab = MainTab.ASSESSMENT
            }
            else -> Unit
        }
    }

    fun navigate(target: Screen) {
        selectedAnswer = null
        reflectionScore = null
        screen = target
        activeTab = when (target) {
            Screen.HOME, Screen.DIMENSION_LIBRARY -> MainTab.HOME
            Screen.ASSESSMENT_INTRO, Screen.RESULT_OVERVIEW,
            Screen.POTENTIAL_DETAIL, Screen.POTENTIAL_MAP -> MainTab.ASSESSMENT
            Screen.GROWTH -> MainTab.GROWTH
            Screen.HISTORY, Screen.COMPARISON, Screen.PROFILE,
            Screen.SETTINGS, Screen.ABOUT -> MainTab.PROFILE
            else -> activeTab
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        navigate(Screen.HOME)
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
                        onStartAssessment = { navigate(Screen.ASSESSMENT_INTRO) },
                        onResult = { navigate(Screen.RESULT_OVERVIEW) },
                        onPotentialMap = { navigate(Screen.POTENTIAL_MAP) },
                        onLibrary = { navigate(Screen.DIMENSION_LIBRARY) },
                        onHistory = { navigate(Screen.HISTORY) }
                    )
                    Screen.ASSESSMENT_INTRO -> AssessmentIntroScreen(
                        onBack = { navigate(Screen.HOME) },
                        onStart = { navigate(Screen.Q_SITUATIONAL) }
                    )
                    Screen.Q_SITUATIONAL -> SituationalQuestionScreen(
                        selectedAnswer,
                        { selectedAnswer = it },
                        { navigate(Screen.ASSESSMENT_INTRO) },
                        { navigate(Screen.Q_LOGIC) }
                    )
                    Screen.Q_LOGIC -> LogicQuestionScreen(
                        selectedAnswer,
                        { selectedAnswer = it },
                        { navigate(Screen.Q_SITUATIONAL) },
                        { navigate(Screen.Q_PATTERN) }
                    )
                    Screen.Q_PATTERN -> PatternQuestionScreen(
                        selectedAnswer,
                        { selectedAnswer = it },
                        { navigate(Screen.Q_LOGIC) },
                        { navigate(Screen.Q_SPATIAL) }
                    )
                    Screen.Q_SPATIAL -> SpatialQuestionScreen(
                        selectedAnswer,
                        { selectedAnswer = it },
                        { navigate(Screen.Q_PATTERN) },
                        { navigate(Screen.Q_CREATIVE) }
                    )
                    Screen.Q_CREATIVE -> CreativeQuestionScreen(
                        selectedAnswer,
                        { selectedAnswer = it },
                        { navigate(Screen.Q_SPATIAL) },
                        { navigate(Screen.Q_REFLECTION) }
                    )
                    Screen.Q_REFLECTION -> ReflectionQuestionScreen(
                        reflectionScore,
                        { reflectionScore = it },
                        { navigate(Screen.Q_CREATIVE) },
                        { navigate(Screen.PROCESSING) }
                    )
                    Screen.PROCESSING -> ProcessingScreen()
                    Screen.RESULT_OVERVIEW -> ResultOverviewScreen(
                        onMap = { navigate(Screen.POTENTIAL_MAP) },
                        onDetail = { navigate(Screen.POTENTIAL_DETAIL) }
                    )
                    Screen.POTENTIAL_DETAIL -> PotentialDetailScreen(
                        onBack = { navigate(Screen.RESULT_OVERVIEW) }
                    )
                    Screen.POTENTIAL_MAP -> PotentialMapScreen(
                        onBack = { navigate(Screen.RESULT_OVERVIEW) },
                        onDetail = { navigate(Screen.POTENTIAL_DETAIL) }
                    )
                    Screen.GROWTH -> GrowthScreen()
                    Screen.HISTORY -> HistoryScreen(
                        assessmentComplete = assessmentComplete,
                        onBack = { navigate(Screen.PROFILE) },
                        onStartAssessment = { navigate(Screen.ASSESSMENT_INTRO) },
                        onResult = { navigate(Screen.RESULT_OVERVIEW) },
                        onCompare = { navigate(Screen.COMPARISON) }
                    )
                    Screen.COMPARISON -> ComparisonScreen(
                        onBack = { navigate(Screen.HISTORY) }
                    )
                    Screen.DIMENSION_LIBRARY -> DimensionLibraryScreen(
                        onBack = { navigate(Screen.HOME) },
                        onDetail = { navigate(Screen.POTENTIAL_DETAIL) }
                    )
                    Screen.PROFILE -> ProfileScreen(
                        assessmentComplete = assessmentComplete,
                        onHistory = { navigate(Screen.HISTORY) },
                        onComparison = { navigate(Screen.COMPARISON) },
                        onResult = { navigate(Screen.RESULT_OVERVIEW) },
                        onSettings = { navigate(Screen.SETTINGS) },
                        onAbout = { navigate(Screen.ABOUT) }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        onBack = { navigate(Screen.PROFILE) },
                        onAbout = { navigate(Screen.ABOUT) },
                        onDeleteHistory = {
                            prefs.edit().remove("assessment_complete").apply()
                            assessmentComplete = false
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
    onStartAssessment: () -> Unit,
    onResult: () -> Unit,
    onPotentialMap: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit
) {
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
                    "10–15 menit · 6 dimensi · tidak ada jawaban salah",
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
                        Text("Agustus 2026", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("Kecenderungan kuat: Logika & Spasial", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        DemoScores.take(3).forEach { score ->
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
            if (assessmentComplete) "1 asesmen tersimpan" else "Belum ada asesmen",
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
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val sections = listOf(
        Triple("01", "Situasi & Preferensi", "4 mnt"),
        Triple("02", "Pola & Logika", "3 mnt"),
        Triple("03", "Visual & Spasial", "4 mnt"),
        Triple("04", "Refleksi", "3 mnt")
    )
    val desc = listOf(
        "Pilihan berdasarkan situasi sehari-hari",
        "Tantangan angka dan urutan sederhana",
        "Mengenali pola dan rotasi bentuk",
        "Pertanyaan mengenali diri sendiri"
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(12.dp))
        Overline("Asesmen Potensi")
        Heading("Siap menjelajahi\npotensimu?", 28)
        Spacer(Modifier.height(10.dp))
        Text(
            "Asesmen ini terdiri dari empat bagian, sekitar 10–15 menit. Tidak ada jawaban sempurna.",
            color = Muted,
            lineHeight = 22.sp,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        sections.forEachIndexed { i, s ->
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
                    Text(s.first, color = Gold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.second, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(desc[i], color = Muted, fontSize = 12.sp)
                }
                Text(s.third, color = Muted.copy(alpha = .7f), fontSize = 11.sp)
            }
            if (i != sections.lastIndex) HorizontalDivider(color = Stone.copy(alpha = .55f))
        }

        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Gunakan hasil sebagai refleksi diri, bukan diagnosis psikologis atau penilaian kemampuan absolut.",
                modifier = Modifier.padding(15.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Mulai Asesmen", onClick = onStart)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionFrame(
    progress: Int,
    section: String,
    trailing: String,
    question: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        TopBack(onBack, trailing)
        ProgressLine(progress, 6)
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
private fun SituationalQuestionScreen(
    selected: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val answers = listOf(
        "Mencari pola atau aturan yang mungkin berlaku, lalu mencoba menerapkannya",
        "Mengumpulkan informasi dari berbagai sumber sebelum membuat keputusan",
        "Bertanya kepada seseorang yang lebih berpengalaman untuk mendapat perspektif",
        "Langsung mencoba berbagai pendekatan dan belajar dari hasilnya"
    )
    QuestionFrame(
        1, "Situasi & Preferensi", "Bagian 1 · 1/3",
        "Ketika menghadapi masalah baru yang belum pernah kamu temui, apa yang paling sering kamu lakukan pertama kali?",
        onBack
    ) {
        answers.forEachIndexed { i, text ->
            AnswerRow(i, text, selected == i) { onSelect(i) }
            Spacer(Modifier.height(10.dp))
        }
        PrimaryButton(
            if (selected == null) "Pilih jawaban" else "Lanjut",
            enabled = selected != null,
            onClick = onNext
        )
    }
}

@Composable
private fun LogicQuestionScreen(
    selected: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    QuestionFrame(
        2, "Pola & Logika", "Bagian 2 · 1/2",
        "Angka manakah yang melanjutkan pola berikut?",
        onBack
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone),
            shape = RoundedCornerShape(13.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("2", "6", "18", "54", "?").forEachIndexed { i, n ->
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (n == "?") Charcoal else Gold.copy(alpha = .12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            n,
                            color = if (n == "?") Gold else Charcoal,
                            fontFamily = FontFamily.Serif,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (i < 4) Text("→", color = Muted.copy(alpha = .4f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        listOf("108", "162", "216", "270").forEachIndexed { i, text ->
            AnswerRow(i, text, selected == i) { onSelect(i) }
            Spacer(Modifier.height(10.dp))
        }
        PrimaryButton(
            if (selected == null) "Pilih jawaban" else "Lanjut",
            enabled = selected != null,
            onClick = onNext
        )
    }
}

@Composable
private fun DotPattern(count: Int, radiusScale: Float, selected: Boolean = false) {
    Canvas(Modifier.size(64.dp)) {
        val r = size.minDimension * .27f
        if (count == 1) {
            drawCircle(if (selected) Gold else Muted, size.minDimension * radiusScale, center)
        } else {
            repeat(count) { i ->
                val a = 2f * PI.toFloat() * i / count - PI.toFloat() / 2f
                val c = Offset(
                    center.x + r * cos(a),
                    center.y + r * sin(a)
                )
                drawCircle(
                    if (selected) Gold else Muted,
                    size.minDimension * radiusScale,
                    c
                )
            }
        }
    }
}

@Composable
private fun PatternQuestionScreen(
    selected: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    QuestionFrame(
        3, "Pola Visual", "Bagian 2 · 2/2",
        "Gambar mana yang seharusnya mengisi posisi terakhir?",
        onBack
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                val cells = listOf(1, 2, 3, 2, 3, 0)
                repeat(2) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { col ->
                            val v = cells[row * 3 + col]
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (v == 0) Charcoal else Gold.copy(alpha = .09f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (v == 0) {
                                    Text("?", color = Gold, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                                } else {
                                    DotPattern(v, if (row == 0) .06f else .07f)
                                }
                            }
                        }
                    }
                    if (row == 0) Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(4, 3).forEachIndexed { i, count ->
                PatternOption(i, count, selected == i) { onSelect(i) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(4, 5).forEachIndexed { j, count ->
                val i = j + 2
                PatternOption(i, count, selected == i) { onSelect(i) }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            if (selected == null) "Pilih jawaban" else "Lanjut",
            enabled = selected != null,
            onClick = onNext
        )
    }
}

@Composable
private fun RowScope.PatternOption(index: Int, count: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Gold.copy(alpha = .10f) else Color.White)
            .border(1.5.dp, if (selected) Gold else Stone, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DotPattern(count, if (index == 2) .075f else .07f, selected)
        Text(('A'.code + index).toChar().toString(), color = if (selected) Gold else Muted, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArrowShape(rotation: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        rotate(rotation, center) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * .5f, h * .08f)
                lineTo(w * .76f, h * .42f)
                lineTo(w * .60f, h * .42f)
                lineTo(w * .60f, h * .88f)
                lineTo(w * .40f, h * .88f)
                lineTo(w * .40f, h * .42f)
                lineTo(w * .24f, h * .42f)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
private fun SpatialQuestionScreen(
    selected: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    QuestionFrame(
        4, "Visual & Spasial", "Bagian 3 · 1/2",
        "Jika bentuk ini diputar 90° searah jarum jam, pilihan mana yang benar?",
        onBack
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(13.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArrowShape(0f, Charcoal, Modifier.size(54.dp))
                Spacer(Modifier.width(24.dp))
                Text("→", color = Gold, fontSize = 24.sp)
                Spacer(Modifier.width(24.dp))
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Charcoal),
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", color = Gold, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val rots = listOf(0f, 90f, 180f, 45f)
        repeat(2) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) { col ->
                    val i = row * 2 + col
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected == i) Gold.copy(alpha = .10f) else Color.White)
                            .border(1.5.dp, if (selected == i) Gold else Stone, RoundedCornerShape(10.dp))
                            .clickable { onSelect(i) }
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ArrowShape(rots[i], if (selected == i) Gold else Muted, Modifier.size(58.dp))
                        Text(('A'.code + i).toChar().toString(), color = if (selected == i) Gold else Muted, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (row == 0) Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            if (selected == null) "Pilih jawaban" else "Lanjut",
            enabled = selected != null,
            onClick = onNext
        )
    }
}

@Composable
private fun CreativeQuestionScreen(
    selected: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val answers = listOf(
        "Mencari sudut pandang dari topik itu yang relevan dengan hal yang kamu sukai",
        "Membuat struktur yang jelas dan logis agar presentasinya tetap efektif",
        "Berkolaborasi dengan orang lain yang lebih tertarik dengan topik tersebut",
        "Mengerjakan dengan standar yang cukup, lalu menyimpan energi untuk hal lain"
    )
    QuestionFrame(
        5, "Skenario Kreatif", "Bagian 3 · 2/2",
        "Kamu mendapat tugas membuat presentasi tentang topik yang sama sekali tidak kamu minati. Apa yang paling mungkin kamu lakukan?",
        onBack
    ) {
        AssistChip(
            onClick = {},
            label = { Text("Tidak ada jawaban benar atau salah", fontSize = 11.sp) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Gold.copy(alpha = .10f),
                labelColor = Gold
            ),
            border = null
        )
        Spacer(Modifier.height(14.dp))
        answers.forEachIndexed { i, text ->
            AnswerRow(i, text, selected == i) { onSelect(i) }
            Spacer(Modifier.height(10.dp))
        }
        PrimaryButton(
            if (selected == null) "Pilih jawaban" else "Lanjut",
            enabled = selected != null,
            onClick = onNext
        )
    }
}

@Composable
private fun ReflectionQuestionScreen(
    score: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    QuestionFrame(
        6, "Refleksi", "Bagian 4 · 1/3",
        "Seberapa sering kamu merasa lebih nyaman bekerja sendiri daripada dalam kelompok?",
        onBack
    ) {
        Text("Pilih yang paling menggambarkan kebiasaanmu", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { n ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (score == n) Gold else Color.White)
                        .border(2.dp, if (score == n) Gold else Stone, RoundedCornerShape(10.dp))
                        .clickable { onSelect(n) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        n.toString(),
                        color = if (score == n) Ivory else Muted,
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Hampir tidak\npernah", modifier = Modifier.weight(1f), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text("Jarang", modifier = Modifier.weight(1f), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text("Kadang", modifier = Modifier.weight(1f), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text("Sering", modifier = Modifier.weight(1f), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text("Sangat\nsering", modifier = Modifier.weight(1f), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(30.dp))
        PrimaryButton(
            if (score == null) "Pilih nilai" else "Selesai & Lihat Hasil",
            enabled = score != null,
            onClick = onFinish
        )
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
    onMap: () -> Unit,
    onDetail: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Overline("Profil Potensi")
        Heading("Agustus 2026", 28)
        Text("Berdasarkan pola responsmu dalam asesmen ini", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        RadarChart(DemoScores, Modifier.fillMaxWidth().height(290.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .09f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Prototipe UI: indeks di layar ini masih contoh tampilan dan belum merupakan scoring psikometrik tervalidasi.",
                modifier = Modifier.padding(14.dp),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Potensi yang Paling Menonjol")
        ResultHighlight("Penalaran Logis", "Kecenderungan kuat", "Kamu cenderung mengenali pola, menyusun informasi, dan memecah persoalan menjadi bagian yang lebih kecil.")
        Spacer(Modifier.height(10.dp))
        ResultHighlight("Spasial", "Kecenderungan kuat", "Kamu menunjukkan kenyamanan dalam memahami hubungan visual dan perubahan orientasi.")
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Lihat Peta Lengkap", onClick = onMap)
        Spacer(Modifier.height(10.dp))
        GhostButton("Jelajahi Detail Dimensi", onClick = onDetail)
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
private fun PotentialDetailScreen(onBack: () -> Unit) {
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
                Modifier
                    .size(170.dp)
                    .align(Alignment.CenterEnd),
                color = Ivory.copy(alpha = .10f)
            )
            Column {
                Text("DIMENSI POTENSI", color = Ivory.copy(alpha = .42f), fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Heading("Penalaran Logis", 30, Ivory)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("82", color = Gold, fontFamily = FontFamily.Serif, fontSize = 58.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text("Indeks kecenderungan", color = Ivory.copy(alpha = .45f), fontSize = 12.sp)
                        Text("Kecenderungan kuat", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(Modifier.padding(22.dp)) {
            SectionLabel("Pola yang Teramati")
            ScoreBar("Pengenalan pola angka", 88)
            Spacer(Modifier.height(16.dp))
            ScoreBar("Analisis situasi bertahap", 79)
            Spacer(Modifier.height(16.dp))
            ScoreBar("Konsistensi respons logis", 80)

            Spacer(Modifier.height(28.dp))
            SectionLabel("Tentang Kecenderungan Ini")
            Text(
                "Responsmu menunjukkan kecenderungan untuk mendekati masalah secara sistematis—mencari pola, menyusun langkah, dan memverifikasi kesimpulan. Ini adalah alat refleksi, bukan label permanen.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(26.dp))
            SectionLabel("Area Pengembangan")
            DevelopmentTip("Latihan deduksi informal dan teka-teki logika.")
            DevelopmentTip("Menulis alur argumen untuk memperjelas pemikiran.")
            DevelopmentTip("Mencoba permainan strategi yang melatih antisipasi multi-langkah.")
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
    onBack: () -> Unit,
    onDetail: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Overline("Agustus 2026")
        Heading("Peta Potensi", 27)
        Spacer(Modifier.height(20.dp))

        DemoScores.forEach { score ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDetail)
                    .padding(vertical = 14.dp)
            ) {
                ScoreBar(
                    score.label,
                    score.value,
                    when {
                        score.value >= 75 -> "Kecenderungan kuat"
                        score.value >= 62 -> "Kecenderungan sedang"
                        else -> "Area pengembangan"
                    }
                )
            }
            HorizontalDivider(color = Stone.copy(alpha = .55f))
        }

        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = .10f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Indeks ini adalah tampilan prototipe dan bukan ukuran kemampuan absolut. Semua dimensi dapat berkembang.",
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
private fun GrowthScreen() {
    val exercises = listOf(
        Triple("Sen", "Mulai percakapan baru", "Ajak bicara seseorang yang belum kamu kenal baik"),
        Triple("Rab", "Latihan mendengar aktif", "Fokus bertanya dan memahami sebelum menjawab"),
        Triple("Jum", "Refleksi interaksi sosial", "Tulis satu hal yang kamu pelajari minggu ini"),
        Triple("Ming", "Tantangan mini: bergabung", "Hadiri satu aktivitas sosial, meski singkat")
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Overline("Minggu ini")
        Heading("Rekomendasi\nPengembangan", 27)
        Spacer(Modifier.height(22.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Charcoal)
                .padding(20.dp)
        ) {
            PotentiaRings(
                Modifier
                    .size(130.dp)
                    .align(Alignment.BottomEnd),
                color = Ivory.copy(alpha = .10f)
            )
            Column(Modifier.fillMaxWidth(.78f)) {
                Text("FOKUS MINGGU INI", color = Ivory.copy(alpha = .42f), fontSize = 11.sp, letterSpacing = 1.3.sp)
                Heading("Dimensi Sosial", 21, Ivory)
                Text(
                    "Area ini memiliki ruang pengembangan terbesar pada profil contoh.",
                    color = Ivory.copy(alpha = .55f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Latihan Minggu Ini")
        exercises.forEachIndexed { i, ex ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Stone, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (i < 2) Gold else Stone.copy(alpha = .55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ex.first, color = if (i < 2) Ivory else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(ex.second, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(ex.third, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Tantangan Singkat")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Stone, RoundedCornerShape(12.dp))
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Gold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Tanya kabar seseorang", fontWeight = FontWeight.Bold)
                Text("3 menit · Dimensi Sosial", color = Muted, fontSize = 12.sp)
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Coba", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HistoryScreen(
    assessmentComplete: Boolean,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onResult: () -> Unit,
    onCompare: () -> Unit
) {
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
            "Asesmen yang telah kamu selesaikan akan tersimpan di sini.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(22.dp))

        if (!assessmentComplete) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onResult)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Gold.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("82", fontFamily = FontFamily.Serif, fontSize = 17.sp, color = Gold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Agustus 2026", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("TERBARU", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Asesmen Lengkap · Logika & Spasial", color = Muted, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Muted.copy(alpha = .3f))
            }
            HorizontalDivider(color = Stone.copy(alpha = .45f))
            Spacer(Modifier.height(18.dp))
            GhostButton("Bandingkan Asesmen →", onClick = onCompare)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ComparisonScreen(onBack: () -> Unit) {
    val previous = listOf(78, 74, 62, 70, 55, 68)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        TopBack(onBack)
        Spacer(Modifier.height(10.dp))
        Heading("Perbandingan Asesmen", 27)
        Text("Agustus 2026 dibanding Mei 2026", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        DemoScores.forEachIndexed { i, current ->
            Column(Modifier.padding(bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(current.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(previous[i].toString(), color = Muted, fontSize = 12.sp)
                    Text("  →  ", color = Muted.copy(alpha = .55f))
                    Text(current.value.toString(), color = Gold, fontFamily = FontFamily.Serif, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    val diff = current.value - previous[i]
                    Text(
                        if (diff >= 0) "+$diff" else "$diff",
                        color = if (diff >= 0) Success else Color(0xFFB5451B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Stone.copy(alpha = .55f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(previous[i] / 100f)
                            .background(Muted.copy(alpha = .25f))
                    )
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(current.value / 100f)
                            .padding(vertical = 1.5.dp)
                            .clip(CircleShape)
                            .background(Gold)
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Stone)
        ) {
            Text(
                "Perubahan dapat dipengaruhi pengalaman, kondisi saat menjawab, dan cara pengguna memahami pertanyaan. Jangan membaca selisih sebagai bukti peningkatan kemampuan absolut.",
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
    onDetail: () -> Unit
) {
    val dims = listOf(
        Triple("Penalaran Logis", "◈", "Mencari pola, struktur, hubungan sebab-akibat, dan langkah penyelesaian."),
        Triple("Kreatif", "◌", "Menghubungkan ide, menghasilkan alternatif, dan melihat sudut pandang baru."),
        Triple("Verbal", "◎", "Memahami dan mengekspresikan ide melalui bahasa."),
        Triple("Spasial", "⬡", "Memvisualisasikan bentuk, orientasi, dan hubungan dalam ruang."),
        Triple("Sosial", "○", "Membaca konteks interpersonal dan bekerja bersama orang lain."),
        Triple("Pemecahan Masalah Praktis", "⊕", "Mengubah ide menjadi tindakan dan solusi yang dapat diterapkan.")
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
            "Enam dimensi ini bukan kategori eksklusif. Setiap orang dapat memiliki kombinasi yang berbeda.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(20.dp))

        dims.forEach { d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Stone, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDetail)
                    .padding(15.dp)
            ) {
                Box(
                    Modifier
                        .size(43.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Gold.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(d.second, color = Gold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.first, fontWeight = FontWeight.Bold)
                    Text(d.third, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ProfileScreen(
    assessmentComplete: Boolean,
    onHistory: () -> Unit,
    onComparison: () -> Unit,
    onResult: () -> Unit,
    onSettings: () -> Unit,
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
                Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                PotentiaRings(Modifier.size(38.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Heading("Pengguna", 21)
                Text(
                    if (assessmentComplete) "1 asesmen selesai" else "Belum ada asesmen",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Asesmen")
        SettingsRow("Riwayat Asesmen", onClick = onHistory)
        if (assessmentComplete) {
            SettingsRow("Perbandingan Kemajuan", onClick = onComparison)
            SettingsRow("Profil Terbaru", onClick = onResult)
        }
        SettingsRow("Cara Membaca Hasil", onClick = onAbout)

        Spacer(Modifier.height(24.dp))
        SectionLabel("Preferensi")
        SettingsRow("Pengaturan", onClick = onSettings)
        SettingsRow("Privasi & Data", onClick = {})

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
    onBack: () -> Unit,
    onAbout: () -> Unit,
    onDeleteHistory: () -> Unit
) {
    var reminder by remember { mutableStateOf(true) }
    var recommendations by remember { mutableStateOf(true) }

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
        SectionLabel("Notifikasi")
        ToggleRow("Pengingat latihan mingguan", reminder) { reminder = it }
        ToggleRow("Rekomendasi pengembangan", recommendations) { recommendations = it }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Data")
        SettingsRow("Ekspor data saya", onClick = {})
        SettingsRow("Hapus riwayat asesmen", danger = true, onClick = onDeleteHistory)

        Spacer(Modifier.height(24.dp))
        SectionLabel("Tentang")
        SettingsRow("Tentang POTENTIA", onClick = onAbout)
        SettingsRow("Versi", info = "1.0.0", onClick = {})
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
            "Hasil tidak mewakili kemampuan intelektual, bakat bawaan, atau kondisi psikologis. Indeks pada prototipe mencerminkan rancangan tampilan dan belum merupakan instrumen psikometrik tervalidasi."
        )
        ArticleSection(
            "Potensi dapat berkembang",
            "Dimensi yang digunakan dirancang sebagai bahasa refleksi. Pengalaman, pembelajaran, latihan, dan konteks dapat memengaruhi cara seseorang merespons."
        )
        ArticleSection(
            "Privasi data",
            "Versi Android prototipe ini hanya menyimpan status onboarding dan asesmen secara lokal menggunakan SharedPreferences. Belum ada sinkronisasi server."
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
