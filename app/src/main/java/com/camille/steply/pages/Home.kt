package com.camille.steply.pages

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.camille.steply.viewmodel.HomeViewModel
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.camille.steply.viewmodel.HomeVmFactory
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import java.time.YearMonth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut




// -------------------- PALETTE --------------------
private val Bg = Color(0xFFF4F1EC)
private val Card = Color.White
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF8E8E93)
private val Divider = Color(0xFFE6E6EA)
private val Accent = Color(0xFFFF8A00)




@Preview
@Composable
fun Home() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeVmFactory(context.applicationContext as Application)
    )

    val uiState by homeViewModel.uiState.collectAsState()

    val isEmulator = remember {
        val fp = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        fp.contains("generic") ||
                fp.contains("emulator") ||
                model.contains("emulator") ||
                model.contains("sdk") ||
                brand.contains("generic") ||
                device.contains("generic")
    }



    DisposableEffect(Unit) {
        if (isEmulator) {
            homeViewModel.startAccelerometerSimulation()
        } else {
            homeViewModel.startStepUpdates()
        }

        onDispose {
            homeViewModel.stopAccelerometerSimulation()
            homeViewModel.stopStepUpdates()
        }
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) homeViewModel.refreshPlace()
    }

    // ✅ Refresh location EVERY TIME the app/screen is resumed (opened again)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    homeViewModel.refreshPlace()
                } else {
                    // Optional: request permission when opening the app
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dailyGoal = uiState.dailyGoal
    val progress = (uiState.steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        homeViewModel.refreshPlace()
    }

    var showCalendar by remember { mutableStateOf(false) }


    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = selectedTab,
                    onSelect = { index ->
                        selectedTab = index
                        if (index == 0) { // Steps tab
                            homeViewModel.selectToday()
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {

            TopBarLight(
                placeText = when {
                    uiState.locationLoading -> "_"
                    uiState.locationError != null -> "Err: ${uiState.locationError}"
                    else -> uiState.currentPlacename
                },
                weatherText = when {
                    uiState.meteoLoading -> "_"
                    uiState.meteoError != null -> "Weather unavailable"
                    else -> ", ${uiState.meteoTempC}°C ${uiState.meteoDesc}  "
                },
                onSettings = { },
                onCalendar = { showCalendar = true }
            )

            Spacer(Modifier.height(14.dp))

            // -------------------- CARD GRANDE CENTRALE --------------------
            StepsMainCard(
                dateLabel = uiState.currentDayname,
                dateValue = uiState.currentDate,
                steps = uiState.selectedSteps,
                dailyGoal = dailyGoal,
                km = uiState.selectedKm,
                kcal = uiState.selectedKcal,
                onRefresh = { }
            )

            Spacer(Modifier.height(14.dp))

            // -------------------- STREAK --------------------
            StreakCard(streakDays = uiState.streakDays)

            Spacer(Modifier.height(30.dp))

            // -------------------- DASHBOARD --------------------
             WeeklyStepsLight(
                currentDateIso = uiState.currentDateIso,
                selectedDateIso = uiState.selectedDateIso,
                values = uiState.weeklySteps,
                dailyGoal = dailyGoal,
                onSelectDay = { iso -> homeViewModel.selectDay(iso) }
            )


            Spacer(Modifier.height(24.dp))
        }
    }

    BottomSlidePopup(
        visible = showCalendar,
        onDismiss = { showCalendar = false }
    ) {
        StepsCalendarSheetContent(
            onClose = { showCalendar = false },
            dailyGoal = uiState.dailyGoal,
            stepsByDateIso = uiState.stepsByDateIso
        )

    }

}

// -------------------- TOP BAR --------------------
@Composable
private fun TopBarLight(
    placeText: String,
    weatherText: String,
    onCalendar: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                ) {
                    append(placeText)
                }
                withStyle(
                    style = SpanStyle(
                        color = TextSecondary
                    )
                ) {
                    append(weatherText)
                }
            },
            fontSize = 14.sp
        )


        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Card,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // --- CALENDARIO ---
                IconButton(
                    onClick = onCalendar,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        tint = TextPrimary
                    )
                }

                // --- SETTINGS ---
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }
        }

    }
}


// -------------------- CARD CENTRALE --------------------
@Composable
private fun StepsMainCard(
    dateLabel: String,
    dateValue: String,
    steps: Int,
    dailyGoal: Int,
    km: String,
    kcal: String,
    onRefresh: () -> Unit
) {

    val targetProgress = if (dailyGoal <= 0) 0f else (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    // tengo memoria del progress precedente per decidere direzione
    var prevProgress by remember { mutableStateOf(targetProgress) }
    val direction = if (targetProgress >= prevProgress) 1f else -1f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650),
        label = "ringProgress"
    )

    // aggiorno prevProgress quando cambia il target (dopo la composizione)
    LaunchedEffect(targetProgress) {
        prevProgress = targetProgress
    }

    val animatedSteps by animateIntAsState(
        targetValue = steps,
        animationSpec = tween(450),
        label = "steps"
    )

    // km e kcal: meglio animarli come numeri e poi formattarli
    val kmDouble = km.toDoubleOrNull() ?: 0.0
    val animatedKm by animateFloatAsState(
        targetValue = kmDouble.toFloat(),
        animationSpec = tween(450),
        label = "km"
    )

    val kcalInt = kcal.toIntOrNull() ?: 0
    val animatedKcal by animateIntAsState(
        targetValue = kcalInt,
        animationSpec = tween(450),
        label = "kcal"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Card,
        shadowElevation = 18.dp
    ){
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // refresh
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = dateLabel,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "dayLabel"
                ) { label ->
                    Text(label, color = TextSecondary, fontSize = 16.sp)
                }

                AnimatedContent(
                    targetState = dateValue,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "dateValue"
                ) { value ->
                    Text(
                        value,
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(20.dp))

                val progress = if (dailyGoal <= 0) 0f
                else (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

                val progressColor = progressColorForSteps(steps, dailyGoal)

                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // anello di progress
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        val inset = strokeWidth / 2f

                        // background ring (grigio)
                        drawArc(
                            color = Color(0xFFE6E6EA),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // progress ring (arancione) — parte dall'alto e va in senso orario
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress * direction,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // contenuto centrale (testi) dentro il cerchio
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = animatedSteps.toString(),
                            color = TextPrimary,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Steps",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            InfoMini(value = String.format(Locale.getDefault(), "%.2f", animatedKm), label = "km")
                            InfoMini(value = animatedKcal.toString(), label = "calories")
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Goal: $dailyGoal",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMini(value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
    }
}

// -------------------- Streak --------------------
@Composable
fun StreakCard(
    streakDays: Int
) {

    val animatedStreak by animateIntAsState(
        targetValue = streakDays,
        animationSpec = tween(durationMillis = 450),
        label = "streakCount"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = animatedStreak.toString(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A00)
                    )

                    Text(
                        text = "day streak!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF8A00)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF6F6F6)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {


                        val hasStreak = streakDays >= 1

                        AnimatedContent(
                            targetState = hasStreak,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "streakMessage"
                        ) { ok ->
                            Text(
                                text = buildAnnotatedString {
                                    if (ok) {
                                        append("Keep your ")
                                        withStyle(
                                            SpanStyle(
                                                color = Color(0xFFFF8A00),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) { append("Perfect Streak") }
                                        append(" \nby walking every day!")
                                    } else {
                                        withStyle(
                                            SpanStyle(
                                                color = Color(0xFFFF8A00),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) { append("You Can Do It!") }
                                        append(" \nStart your streak now.")
                                    }
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF444444)
                            )
                        }

                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "🔥",
                            fontSize = 32.sp
                        )
                    }
            }

            }
        }
    }
}


// -------------------- DASHBOARD --------------------
@Composable
private fun WeeklyStepsLight(currentDateIso: String, selectedDateIso: String, values: List<Int>, dailyGoal: Int, onSelectDay: (String) -> Unit) {

    val today = remember(currentDateIso) { LocalDate.parse(currentDateIso) }

    // ultimi 7 giorni: 6 giorni fa ... oggi
    val last7Days = remember(today) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }
    }

    val days = remember(last7Days) {
        last7Days.map { d ->
            d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase()
        }
    }

    val dates = remember(last7Days) {
        last7Days.map { it.dayOfMonth.toString() }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val stepsForDay = values.getOrNull(index) ?: 0

                // ✅ frazione rispetto al GOAL (non rispetto al max della settimana)
                val frac = if (dailyGoal <= 0) 0f
                else (stepsForDay.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

                // ✅ altezza minima così la barra non sparisce mai
                val barHeight = (6f + 84f * frac).dp

                val dayIso = last7Days[index].toString()
                val isSelected = dayIso == selectedDateIso


                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // ---- barra (SEMPRE fuori dal quadratino) ----
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(barHeight)
                            .background(
                                color = if (stepsForDay >= dailyGoal) Color(0xFF4CAF50) else Accent,
                                shape = RoundedCornerShape(50)
                            )
                    )

                    Spacer(Modifier.height(10.dp))

                    // ---- quadratino SOLO attorno a giorno + numero ----
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0xFFE2E2E2) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) { onSelectDay(dayIso) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day,
                                color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = dates[index],
                                color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}


// -------------------- NAVBAR --------------------

@Composable
fun BottomPillNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Steps", Icons.Default.DirectionsRun),
        NavItem("Activity", Icons.Default.Whatshot),
        NavItem("Profile", Icons.Default.Person)
    )

    val container = Color(0xFFF3F0EC)
    val selectedBg = Color(0xFFE2E2E2)
    val textNormal = Color(0xFF111111)
    val accent = Accent
    val shadow = 12.dp

    Surface(
        modifier = modifier
            .width(360.dp)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = container,
        shadowElevation = shadow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

        items.forEachIndexed { index, item ->
                val selected = index == selectedIndex

                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) selectedBg else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(min = 96.dp)
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) accent else textNormal,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = if (selected) accent else textNormal,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector
)

// -------------------- CALENDAR --------------------

@Composable
fun BottomSlidePopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    // quanto "sotto" parte il foglio (dp)
    val hiddenOffset = 900f

    val sheetOffset = remember { Animatable(hiddenOffset) }
    val scrimAlpha = remember { Animatable(0f) }

    // per tenere in composizione durante l'animazione di chiusura
    var keepInComposition by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            keepInComposition = true
            // reset (se riapri subito)
            sheetOffset.snapTo(hiddenOffset)
            scrimAlpha.snapTo(0f)

            // anima IN (dal basso verso l'alto)
            scrimAlpha.animateTo(1f, tween(110))
            sheetOffset.animateTo(0f, tween(180))
        } else {
            // anima OUT (verso il basso)
            scrimAlpha.animateTo(0f, tween(140))
            sheetOffset.animateTo(hiddenOffset, tween(240))
            keepInComposition = false
        }
    }

    if (!keepInComposition) return

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // sfondo scuro cliccabile
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.35f * scrimAlpha.value)
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        // sheet
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = sheetOffset.value.dp)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume */ }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                content()
            }
        }
    }
}


@Composable
private fun StepsCalendarSheetContent(
    onClose: () -> Unit,
    dailyGoal: Int,
    stepsByDateIso: Map<String, Int>
) {
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 18.dp)
    ) {
        // Header: X + titolo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = "Steps",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(6.dp))

        val nowYm = remember { java.time.YearMonth.now() }
        val months = remember(nowYm) { (0 until 12).map { nowYm.minusMonths(it.toLong()) } }

        // overlay popup sopra la lista (come screenshot)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            val topPad = if (selectedDate != null) 86.dp else 0.dp

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = true, // mesi recenti in basso
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = topPad, bottom = 24.dp)
            ) {
                items(months, key = { it.toString() }) { ym: java.time.YearMonth ->
                    MonthCalendarCard(
                        month = ym,
                        dailyGoal = dailyGoal,
                        stepsByDateIso = stepsByDateIso,
                        selectedDate = selectedDate,
                        onSelectDate = { date ->
                            // toggle: se clicco lo stesso giorno, deseleziona
                            selectedDate = if (selectedDate == date) null else date
                        }
                    )
                }
            }

            // Popup (se c’è una selezione) — SOLO FADE (semplice)
            val popupAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
            var popupDate by remember { mutableStateOf<java.time.LocalDate?>(null) } // cache per exit

            LaunchedEffect(selectedDate) {
                if (selectedDate != null) {
                    popupDate = selectedDate
                    popupAlpha.animateTo(1f, tween(140)) // niente snapTo
                } else {
                    popupAlpha.animateTo(0f, tween(120))
                    popupDate = null
                }
            }

            if (popupDate != null || popupAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .graphicsLayer(alpha = popupAlpha.value)
                ) {
                    val date = popupDate ?: return@Box
                    FadeSwapPopupContent(
                        date = date,
                        steps = stepsByDateIso[date.toString()] ?: 0,
                        dailyGoal = dailyGoal,
                        onClose = { selectedDate = null }
                    )
                }
            }

        }
    }
}


@Composable
private fun MonthCalendarCard(
    month: java.time.YearMonth,
    dailyGoal: Int,
    stepsByDateIso: Map<String, Int>,
    selectedDate: java.time.LocalDate?,
    onSelectDate: (java.time.LocalDate) -> Unit
) {
    val locale = Locale.getDefault()

    val monthName = remember(month) {
        month.month.getDisplayName(java.time.format.TextStyle.FULL, locale).lowercase(locale)
    }
    val yearText = remember(month) { month.year.toString() }

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Text(
                text = yearText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        MonthGrid(
            month = month,
            dailyGoal = dailyGoal,
            stepsByDateIso = stepsByDateIso,
            selectedDate = selectedDate,
            onSelectDate = onSelectDate
        )
    }
}


@Composable
private fun MonthGrid(
    month: java.time.YearMonth,
    dailyGoal: Int,
    stepsByDateIso: Map<String, Int>,
    selectedDate: java.time.LocalDate?,
    onSelectDate: (java.time.LocalDate) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = remember { java.time.LocalDate.now() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val firstDay = remember(month) { month.atDay(1) }
        val daysInMonth = remember(month) { month.lengthOfMonth() }
        val leadingBlanks = remember(firstDay) { (firstDay.dayOfWeek.value - 1).coerceAtLeast(0) }

        val totalCells = remember(leadingBlanks, daysInMonth) {
            val raw = leadingBlanks + daysInMonth
            val weeks = (raw + 6) / 7
            weeks * 7
        }

        val cells = remember(month, totalCells, leadingBlanks, daysInMonth) {
            (0 until totalCells).map { idx ->
                val dayNum = idx - leadingBlanks + 1
                if (dayNum in 1..daysInMonth) dayNum else null
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                week.forEach { dayNum ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum == null) {
                            Spacer(Modifier.size(1.dp))
                        } else {
                            val date = month.atDay(dayNum)
                            val isFuture = date.isAfter(today)
                            val steps = stepsByDateIso[date.toString()] ?: 0
                            val isSelected = selectedDate == date

                            DayCellColored(
                                date = date,
                                steps = steps,
                                dailyGoal = dailyGoal,
                                isSelected = isSelected,
                                enabled = !isFuture,
                                onClick = { onSelectDate(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DayCellColored(
    date: java.time.LocalDate,
    steps: Int,
    dailyGoal: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val today = remember { java.time.LocalDate.now() }
    val isToday = date == today
    val isFuture = date.isAfter(today)
    val isZeroPast = !isFuture && steps == 0
    val textColor = when {
        isFuture -> Color(0xFF8E8E93)   // futuri grigi
        else -> Color.Black            // tutti i giorni passati + oggi
    }


    val baseColor = if (isFuture || isZeroPast) {
        Color(0xFFF2F2F4)
    } else {
        progressColorForSteps(steps, dailyGoal)
    }


    val brush = if (isFuture) {
        Brush.verticalGradient(listOf(baseColor, baseColor))
    } else {
        Brush.verticalGradient(
            listOf(
                baseColor.copy(alpha = 0.75f),
                baseColor.copy(alpha = 1f)
            )
        )
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .then(
                if (isSelected) Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
        )

    }
}

@Composable
private fun SelectedDayPopup(
    date: java.time.LocalDate,
    steps: Int,
    dailyGoal: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()

    val monthName = remember(date) {
        date.month.getDisplayName(java.time.format.TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    val km = remember(steps) { (steps * 0.74) / 1000.0 }
    val kmText = remember(km) { String.format(Locale.getDefault(), "%.2f", km) }
    val kcal = remember(km) { (70.0 * km * 0.75).roundToInt() }

    val progress = if (dailyGoal <= 0) 0f else (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val ringColor = progressColorForSteps(steps, dailyGoal)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.98f),
        shadowElevation = 18.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // X a destra
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ring + mese sotto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(62.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            val inset = stroke / 2f

                            // background
                            drawArc(
                                color = Color(0xFFE6E6EA),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // progress
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }

                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = monthName,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Stats orizzontali (più grandi, senza emoji)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 44.dp), // spazio per la X
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatPill(value = steps.toString(), label = "steps")
                    StatPill(value = kmText, label = "km")
                    StatPill(value = kcal.toString(), label = "cal")
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF666666)
        )
    }
}


@Composable
private fun FadeSwapPopupContent(
    date: java.time.LocalDate,
    steps: Int,
    dailyGoal: Int,
    onClose: () -> Unit
) {
    var shownDate by remember { mutableStateOf(date) }
    var shownSteps by remember { mutableStateOf(steps) }

    val alpha = remember { androidx.compose.animation.core.Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(date, steps, dailyGoal) {
        // ✅ prima apertura: NON fare fade-out/fade-in, mostra subito
        if (!initialized) {
            initialized = true
            shownDate = date
            shownSteps = steps
            alpha.snapTo(1f)
            return@LaunchedEffect
        }

        // ✅ cambio giorno: crossfade semplice
        alpha.animateTo(0f, tween(80))
        shownDate = date
        shownSteps = steps
        alpha.animateTo(1f, tween(120))
    }

    Box(modifier = Modifier.graphicsLayer(alpha = alpha.value)) {
        SelectedDayPopup(
            date = shownDate,
            steps = shownSteps,
            dailyGoal = dailyGoal,
            onClose = onClose
        )
    }
}







private fun progressColorForSteps(steps: Int, dailyGoal: Int): Color {
    if (dailyGoal <= 0) return Color(0xFFE6E6EA)

    val progress = (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    return if (progress <= 0.5f) {
        lerp(
            start = Color(0xFFE53935),   // rosso
            stop = Color(0xFFFFEB3B),   // giallo
            fraction = progress / 0.5f
        )
    } else {
        lerp(
            start = Color(0xFFFFEB3B),   // giallo
            stop = Color(0xFF4CAF50),   // verde
            fraction = (progress - 0.5f) / 0.5f
        )
    }
}
