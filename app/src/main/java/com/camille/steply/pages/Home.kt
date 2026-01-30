package com.camille.steply.pages

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.camille.steply.R
import com.camille.steply.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// -------------------- PALETTE --------------------
private val Bg = Color(0xFFF4F1EC)
private val Card = Color.White
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF8E8E93)
private val Divider = Color(0xFFE6E6EA)
private val Accent = Color(0xFFFF8A00)

private sealed class NavIcon {
    data class Vector(val imageVector: ImageVector) : NavIcon()
    data class Drawable(val resId: Int) : NavIcon()
}

private data class NavItem(
    val label: String,
    val icon: NavIcon
)

@Composable
fun Home(navController: NavController, homeViewModel: HomeViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inPreview = LocalInspectionMode.current

    val uiState by homeViewModel.uiState.collectAsState()

    // Responsive
    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420
    val sidePad = if (isSmall) 12.dp else 16.dp

    // tracking always on
    LaunchedEffect(Unit) {
        if (!inPreview) homeViewModel.ensureTrackingRunning()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) homeViewModel.refreshPlaceThrottled()
    }

    // Refresh location everytime resumed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    homeViewModel.refreshPlaceThrottled()
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dailyGoal = uiState.dailyGoal

    LaunchedEffect(Unit) {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            homeViewModel.refreshPlaceThrottled()
        }
    }

    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = sidePad,
                        end = sidePad,
                        bottom = if (isSmall) 12.dp else 18.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = 0,
                    onSelect = { index ->
                        when (index) {
                            0 -> homeViewModel.selectToday()
                            1 -> {
                                val popped = navController.popBackStack(Routes.ACTIVITY, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Routes.ACTIVITY) { launchSingleTop = true }
                                }
                            }
                            2 -> {
                                val popped = navController.popBackStack(Routes.PROFILE, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Bg)
                .padding(horizontal = sidePad, vertical = 10.dp)
        ) {
            TopBarLight(
                placeText = when {
                    uiState.locationLoading -> ""
                    uiState.locationError != null -> "Err: ${uiState.locationError}"
                    else -> uiState.currentPlacename
                },
                weatherText = when {
                    uiState.meteoLoading -> ""
                    uiState.meteoError != null -> "Weather unavailable"
                    else -> ", ${uiState.meteoTempC}°C ${uiState.meteoDesc}  "
                },
                onSettings = { },
                onCalendar = { showCalendar = true }
            )

            Spacer(Modifier.height(7.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Bg)
                    .verticalScroll(scrollState)
                    .padding(top = 12.dp)
            ) {
                StepsMainCard(
                    dateLabel = uiState.currentDayname,
                    dateValue = uiState.currentDate,
                    steps = uiState.selectedSteps,
                    dailyGoal = dailyGoal,
                    km = uiState.selectedKm,
                    kcal = uiState.selectedKcal,
                    isSmall = isSmall
                )

                Spacer(Modifier.height(10.dp))

                StreakCard(streakDays = uiState.streakDays)

                Spacer(Modifier.height(7.dp))

                WeeklyStepsLight(
                    currentDateIso = uiState.currentDateIso,
                    selectedDateIso = uiState.selectedDateIso,
                    values = uiState.weeklySteps,
                    dailyGoal = dailyGoal,
                    onSelectDay = { iso -> homeViewModel.selectDay(iso) }
                )

                Spacer(Modifier.height(4.dp))
            }
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
                ) { append(placeText) }
                withStyle(style = SpanStyle(color = TextSecondary)) { append(weatherText) }
            },
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                IconButton(onClick = onCalendar, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
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
    isSmall: Boolean
) {
    val ringSize = if (isSmall) 200.dp else 230.dp
    val ringPad = if (isSmall) 8.dp else 10.dp
    val strokeDp = if (isSmall) 10.dp else 11.dp

    val titleSize = if (isSmall) 24.sp else 28.sp
    val daySize = if (isSmall) 14.sp else 16.sp

    val topPadV = if (isSmall) 16.dp else 20.dp
    val spacerBeforeRing = if (isSmall) 18.dp else 24.dp

    val targetProgress = if (dailyGoal <= 0) 0f else (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(650), label = "ringProgress")
    val animatedSteps by animateIntAsState(targetValue = steps, animationSpec = tween(450), label = "steps")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Card,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = topPadV),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dateLabel, color = TextSecondary, fontSize = daySize)
            Text(
                dateValue,
                color = TextPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(spacerBeforeRing))

            val progressColor = progressColorForSteps(steps, dailyGoal)

            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(ringPad)) {
                    val strokeWidth = strokeDp.toPx()
                    drawArc(
                        color = Color(0xFFE6E6EA),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                val stepsStr = animatedSteps.toString()
                val stepsFontSize = when {
                    isSmall && stepsStr.length >= 6 -> 34.sp
                    isSmall && stepsStr.length == 5 -> 40.sp
                    isSmall -> 50.sp
                    stepsStr.length >= 6 -> 40.sp
                    stepsStr.length == 5 -> 48.sp
                    else -> 60.sp
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stepsStr, color = TextPrimary, fontSize = stepsFontSize, fontWeight = FontWeight.Bold, maxLines = 1)

                    Text(
                        text = "Steps",
                        color = TextSecondary,
                        fontSize = if (isSmall) 13.sp else 14.sp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )

                    Spacer(Modifier.height(if (isSmall) 10.dp else 12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoMini(value = km, label = "km")
                        Box(modifier = Modifier.size(4.dp).background(TextSecondary.copy(0.3f), CircleShape))
                        InfoMini(value = kcal, label = "kcal")
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Goal: $dailyGoal",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = if (isSmall) 12.sp else 13.sp,
                        fontWeight = FontWeight.Medium
                    )
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

// -------------------- STREAK --------------------
@Composable
fun StreakCard(streakDays: Int) {
    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420

    val outerPad = if (isSmall) 16.dp else 20.dp
    val bigNum = if (isSmall) 32.sp else 40.sp
    val labelSize = if (isSmall) 14.sp else 15.sp
    val msgSize = if (isSmall) 13.sp else 14.sp
    val innerPad = if (isSmall) 14.dp else 16.dp

    val animatedStreak by animateIntAsState(
        targetValue = streakDays,
        animationSpec = tween(durationMillis = 450),
        label = "streakCount"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(outerPad)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = animatedStreak.toString(),
                        fontSize = bigNum,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                    Text(
                        text = "day streak!",
                        fontSize = labelSize,
                        fontWeight = FontWeight.Medium,
                        color = Accent
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
                            .padding(innerPad),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasStreak = streakDays >= 1

                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = hasStreak,
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                label = "streakMessage"
                            ) { ok ->
                                Text(
                                    text = buildAnnotatedString {
                                        if (ok) {
                                            append("Keep your ")
                                            withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                                                append("Perfect Streak")
                                            }
                                            append(" \nby walking every day!")
                                        } else {
                                            withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                                                append("You Can Do It!")
                                            }
                                            append(" \nStart your streak now.")
                                        }
                                    },
                                    fontSize = msgSize,
                                    color = Color(0xFF444444)
                                )
                            }
                        }

                        Text(
                            text = "🔥",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------- DASHBOARD --------------------
@Composable
private fun WeeklyStepsLight(
    currentDateIso: String,
    selectedDateIso: String,
    values: List<Int>,
    dailyGoal: Int,
    onSelectDay: (String) -> Unit
) {
    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420

    val today = remember(currentDateIso) { LocalDate.parse(currentDateIso) }

    val last7Days = remember(today) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }
    }

    val days = remember(last7Days) {
        last7Days.map { d ->
            d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase()
        }
    }

    val dates = remember(last7Days) { last7Days.map { it.dayOfMonth.toString() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
    ) {
        val barWidth = if (isSmall) 9.dp else 10.dp
        val dayFont = if (isSmall) 13.sp else 15.sp
        val dateFont = if (isSmall) 18.sp else 20.sp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSmall) 160.dp else 180.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val stepsForDay = values.getOrNull(index) ?: 0

                val frac = if (dailyGoal <= 0) 0f
                else (stepsForDay.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

                val barHeight = (6f + 84f * frac).dp

                val dayIso = last7Days[index].toString()
                val isSelected = dayIso == selectedDateIso

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barHeight)
                            .background(
                                color = if (stepsForDay >= dailyGoal) Color(0xFF4CAF50) else Accent,
                                shape = RoundedCornerShape(50)
                            )
                    )

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0xFFE2E2E2) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) { onSelectDay(dayIso) }
                            .padding(horizontal = if (isSmall) 10.dp else 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day,
                                color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                                fontSize = dayFont,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = dates[index],
                                color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                                fontSize = dateFont,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
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
        NavItem("Steps", NavIcon.Drawable(R.drawable.steps_icon)),
        NavItem("Activity", NavIcon.Vector(Icons.Default.DirectionsRun)),
        NavItem("Profile", NavIcon.Vector(Icons.Default.Person))
    )

    val container = Color(0xFFF3F0EC)
    val selectedBg = Color(0xFFE2E2E2)
    val textNormal = Color(0xFF111111)
    val accent = Accent
    val shadow = 12.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = container,
        shadowElevation = shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
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
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (val ic = item.icon) {
                            is NavIcon.Vector -> {
                                Icon(
                                    imageVector = ic.imageVector,
                                    contentDescription = item.label,
                                    tint = if (selected) accent else textNormal,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            is NavIcon.Drawable -> {
                                Icon(
                                    painter = painterResource(id = ic.resId),
                                    contentDescription = item.label,
                                    tint = if (selected) accent else textNormal,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = if (selected) accent else textNormal,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// -------------------- CALENDAR --------------------

@Composable
fun BottomSlidePopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val hiddenOffset = 900f

    val sheetOffset = remember { Animatable(hiddenOffset) }
    val scrimAlpha = remember { Animatable(0f) }
    var keepInComposition by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            keepInComposition = true
            sheetOffset.snapTo(hiddenOffset)
            scrimAlpha.snapTo(0f)
            scrimAlpha.animateTo(1f, tween(110))
            sheetOffset.animateTo(0f, tween(180))
        } else {
            scrimAlpha.animateTo(0f, tween(140))
            sheetOffset.animateTo(hiddenOffset, tween(240))
            keepInComposition = false
        }
    }

    if (!keepInComposition) return

    Box(modifier = Modifier.fillMaxSize()) {
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
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 18.dp)
    ) {
        // Header
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
                            selectedDate = if (selectedDate == date) null else date
                        }
                    )
                }
            }

            val popupAlpha = remember { Animatable(0f) }
            var popupDate by remember { mutableStateOf<LocalDate?>(null) }

            LaunchedEffect(selectedDate) {
                if (selectedDate != null) {
                    popupDate = selectedDate
                    popupAlpha.animateTo(1f, tween(140))
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
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit
) {
    val locale = Locale.getDefault()

    val monthName = remember(month) {
        month.month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
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
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = remember { LocalDate.now() }

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
    date: LocalDate,
    steps: Int,
    dailyGoal: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val isToday = date == today
    val isFuture = date.isAfter(today)
    val isZeroPast = !isFuture && steps == 0
    val textColor = when {
        isFuture -> Color(0xFF8E8E93)
        else -> Color.Black
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
    date: LocalDate,
    steps: Int,
    dailyGoal: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()

    val monthName = remember(date) {
        date.month.getDisplayName(TextStyle.FULL, locale)
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

                            drawArc(
                                color = Color(0xFFE6E6EA),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
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

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 44.dp),
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
    date: LocalDate,
    steps: Int,
    dailyGoal: Int,
    onClose: () -> Unit
) {
    var shownDate by remember { mutableStateOf(date) }
    var shownSteps by remember { mutableStateOf(steps) }

    val alpha = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(date, steps, dailyGoal) {
        if (!initialized) {
            initialized = true
            shownDate = date
            shownSteps = steps
            alpha.snapTo(1f)
            return@LaunchedEffect
        }

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


// -------------------- COLOR --------------------

private fun progressColorForSteps(steps: Int, dailyGoal: Int): Color {
    if (dailyGoal <= 0) return Color(0xFFE6E6EA)

    val progress = (steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    return if (progress <= 0.5f) {
        lerp(
            start = Color(0xFFE53935),
            stop = Color(0xFFFFEB3B),
            fraction = progress / 0.5f
        )
    } else {
        lerp(
            start = Color(0xFFFFEB3B),
            stop = Color(0xFF4CAF50),
            fraction = (progress - 0.5f) / 0.5f
        )
    }
}

/*private fun progressColorForSteps(steps: Int, dailyGoal: Int): Color {
    return if (steps >= dailyGoal) Color(0xFF4CAF50) else Accent
}*/
