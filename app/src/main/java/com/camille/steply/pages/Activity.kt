package com.camille.steply.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.camille.steply.viewmodel.ActivityViewModel
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.WorkoutHistoryViewModel
import com.camille.steply.viewmodel.WorkoutReportSnapshot
import com.camille.steply.viewmodel.WorkoutType
import com.camille.steply.viewmodel.workoutColor
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import com.camille.steply.viewmodel.ActivityEvent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush

@Composable
fun ActivityScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val activityViewModel: ActivityViewModel = viewModel()
    val activityState by activityViewModel.uiState.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()

    var navigating by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val historyVm: WorkoutHistoryViewModel = viewModel()
    val historyItems by historyVm.items.collectAsState()
    val isLoading by historyVm.isLoading.collectAsState()

    var showSkeleton by remember { mutableStateOf(false) }

    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420
    val sidePad = if (isSmall) 12.dp else 16.dp
    val bottomPad = if (isSmall) 12.dp else 18.dp

    LaunchedEffect(Unit) {
        historyVm.monitorUserHistory()
    }

    LaunchedEffect(Unit) {
        activityViewModel.events.collectLatest { event ->
            when (event) {
                is ActivityEvent.NavigateToWorkout -> {
                    navigating = true
                    navController.navigate("${Routes.WORKOUT}/${event.type.name}") {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            showSkeleton = false
            kotlinx.coroutines.delay(250)
            if (isLoading) showSkeleton = true
        } else {
            showSkeleton = false
        }
    }

    if (activityState.isCountingDown && activityState.countdownType != null) {
        CountdownFullScreen(
            number = activityState.secondsLeft,
            label = activityState.phaseText,
            color = workoutColor(activityState.countdownType!!)
        )
        return
    }

    if (navigating) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F1EC)))
        return
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                navigating = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        containerColor = Color(0xFFF4F1EC),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = sidePad)
                    .padding(bottom = bottomPad),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = 1,
                    onSelect = { index ->
                        when (index) {
                            0 -> {
                                homeViewModel.selectToday()
                                navController.navigate(Routes.STEPS) { launchSingleTop = true }
                            }
                            1 -> Unit
                            2 -> {
                                navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                ActivitiesTopBar(
                    onWorkoutSelected = { type ->
                        activityViewModel.startCountdown(type)
                    }
                )

                Spacer(Modifier.height(10.dp))

                ActivityWeatherHeader(
                    temp = uiState.meteoTempC,
                    emoji = uiState.meteoDesc,
                    place = uiState.currentPlacename,
                    loading = uiState.meteoLoading
                )

                Spacer(Modifier.height(8.dp))

                when {
                    isLoading && showSkeleton -> { WorkoutHistorySkeletonList() }

                    isLoading && !showSkeleton -> { Spacer(Modifier.height(1.dp)) }

                    !isLoading && historyItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Click + to start your first workout",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }

                    else -> {
                        ActivityHistorySection(
                            history = historyItems,
                            onClick = { snap ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(Routes.WORKOUT_REPORT_SNAPSHOT, snap)
                                navController.currentBackStackEntry?.savedStateHandle?.set(Routes.FROM_HISTORY, true)
                                navController.navigate(Routes.WORKOUT_REPORT)
                            },
                            onDelete = { snap ->
                                activityViewModel.deleteWorkout(snap)
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitiesTopBar(onWorkoutSelected: (WorkoutType) -> Unit) {
    var showWorkoutPicker by remember { mutableStateOf(false) }
    val alpha = remember { Animatable(0f) }
    val popupAnimOffsetY = remember { Animatable(-8f) }
    var keepInComposition by remember { mutableStateOf(false) }

    LaunchedEffect(showWorkoutPicker) {
        if (showWorkoutPicker) {
            keepInComposition = true
            alpha.snapTo(0f)
            popupAnimOffsetY.snapTo(-8f)
            alpha.animateTo(1f, tween(120))
            popupAnimOffsetY.animateTo(0f, tween(140))
        } else {
            alpha.animateTo(0f, tween(90))
            popupAnimOffsetY.animateTo(-8f, tween(90))
            keepInComposition = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(text = "All Activities", fontSize = 28.sp, color = Color.Black, modifier = Modifier.align(Alignment.Center))
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(8.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showWorkoutPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }

            val popupOffset = with(LocalDensity.current) { IntOffset(x = (-10).dp.roundToPx(), y = 60.dp.roundToPx()) }

            if (keepInComposition) {
                Popup(alignment = Alignment.TopEnd, offset = popupOffset, onDismissRequest = { showWorkoutPicker = false }, properties = PopupProperties(focusable = true)) {
                    Box(modifier = Modifier.graphicsLayer(alpha = alpha.value, translationY = popupAnimOffsetY.value).width(180.dp)) {
                        Surface(shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 8.dp) {
                            Column(modifier = Modifier.widthIn(min = 210.dp).padding(horizontal = 18.dp, vertical = 14.dp)) {
                                Text(text = "Start a workout", fontWeight = FontWeight.SemiBold, color = Color(0xFF7A7A7A), fontSize = 16.sp)
                                Spacer(Modifier.height(14.dp))
                                WorkoutRow(label = "Walk", icon = Icons.Default.DirectionsWalk, iconTint = Color(0xFF2F80FF), onClick = { showWorkoutPicker = false; onWorkoutSelected(WorkoutType.WALK) })
                                WorkoutRow(label = "Run", icon = Icons.Default.DirectionsRun, iconTint = Color(0xFF9B51E0), onClick = { showWorkoutPicker = false; onWorkoutSelected(WorkoutType.RUN) })
                                WorkoutRow(label = "Cycling", icon = Icons.Default.DirectionsBike, iconTint = Color(0xFF27AE60), onClick = { showWorkoutPicker = false; onWorkoutSelected(WorkoutType.CYCLING) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Dp.roundToPx(): Int {
    val density = LocalDensity.current
    return with(density) { this@roundToPx.toPx().roundToInt() }
}

@Composable
fun WorkoutRow(label: String, icon: ImageVector, iconTint: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.width(180.dp).clip(RoundedCornerShape(14.dp)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}

@Composable
private fun ActivityWeatherHeader(temp: String, emoji: String, place: String, loading: Boolean) {
    val reservedHeight = 115.dp
    Box(modifier = Modifier.fillMaxWidth().height(reservedHeight).padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE6E6E6)))
                Spacer(Modifier.width(16.dp))
                Column {
                    Box(modifier = Modifier.height(18.dp).width(150.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE6E6E6)))
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.height(30.dp).width(90.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE6E6E6)))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 70.sp)
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = place, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                    Text(text = "$temp°C", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun CountdownFullScreen(number: Int, label: String, color: Color) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F1EC)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = number.toString(), fontSize = 120.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(10.dp))
            Text(text = label, fontSize = 44.sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ActivityHistorySection(
    history: List<WorkoutReportSnapshot>,
    onClick: (WorkoutReportSnapshot) -> Unit,
    onDelete: (WorkoutReportSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val currentMonthKey by remember(history, listState) {
        derivedStateOf {
            val idx = listState.firstVisibleItemIndex
            val snap = history.getOrNull(idx)
            snap?.let { yearMonthKey(it.startTimeMs) } ?: ""
        }
    }

    Column(modifier = modifier) {
        if (currentMonthKey.isNotBlank()) {
            Text(text = currentMonthKey, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(2.dp))
        FadedEdgesLazyColumn(state = listState, topFadeHeight = 26.dp, bottomFadeHeight = 26.dp) {
            items(items = history, key = { it.startTimeMs }) { snap ->
                SwipeableWorkoutRow(
                    snap = snap,
                    onClick = { onClick(snap) },
                    onDelete = { onDelete(snap) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableWorkoutRow(
    snap: WorkoutReportSnapshot,
    onClick: () -> Unit,
    onDelete: (WorkoutReportSnapshot) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showConfirmDialog = true
                false
            } else false
        }
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete Activity", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this workout? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(snap)
                    showConfirmDialog = false
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Color(0xFFE53935)
            } else Color.Transparent

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp).size(28.dp)
                )
            }
        },
        content = {
            WorkoutHistoryRow(snap = snap, onClick = onClick)
        }
    )
}

private fun yearMonthKey(ms: Long): String {
    val cal = java.util.Calendar.getInstance().apply { time = Date(ms) }
    val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, Locale.ENGLISH) ?: ""
    val year = cal.get(java.util.Calendar.YEAR)
    return "${month} $year".replaceFirstChar { it.uppercase() }
}

@Composable
private fun WorkoutHistoryRow(snap: WorkoutReportSnapshot, onClick: () -> Unit) {
    val type = runCatching { WorkoutType.valueOf(snap.type) }.getOrElse { WorkoutType.WALK }
    val (icon, tint) = when (type) {
        WorkoutType.WALK -> Icons.Default.DirectionsWalk to Color(0xFF2F80FF)
        WorkoutType.RUN -> Icons.Default.DirectionsRun to Color(0xFF9B51E0)
        WorkoutType.CYCLING -> Icons.Default.DirectionsBike to Color(0xFF27AE60)
    }
    val title = when (type) {
        WorkoutType.WALK -> "Walk"
        WorkoutType.RUN -> "Run"
        WorkoutType.CYCLING -> "Cycling"
    }
    val km = snap.distanceMeters / 1000.0
    val kmText = String.format(Locale.US, "%.2f km", km)
    val timeText = remember(snap.startTimeMs) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(snap.startTimeMs)) }
    val dayText = remember(snap.startTimeMs) { SimpleDateFormat("EEEE d MMMM", Locale.ENGLISH).format(Date(snap.startTimeMs)) }

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5C5C5C))
                    Text(timeText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B6B))
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(kmText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(dayText, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFB0B0B0))
        }
    }
}

@Composable
private fun WorkoutHistorySkeletonList() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) { WorkoutHistorySkeletonRow() }
    }
}

@Composable
private fun WorkoutHistorySkeletonRow() {
    Surface(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(88.dp), shape = RoundedCornerShape(26.dp), color = Color(0xFFF2F2F2), shadowElevation = 4.dp) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE8E8E8)))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.height(16.dp).width(140.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE2E2E2)))
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.height(22.dp).width(90.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFC8C8C8)))
            }
        }
    }
}

@Composable
private fun FadedEdgesLazyColumn(state: androidx.compose.foundation.lazy.LazyListState, topFadeHeight: Dp, bottomFadeHeight: Dp, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val bg = Color(0xFFF4F1EC)
    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.lazy.LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 15.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
        Box(modifier = Modifier.fillMaxWidth().height(topFadeHeight).align(Alignment.TopCenter).background(Brush.verticalGradient(colors = listOf(bg, bg.copy(alpha = 0f)))))
        Box(modifier = Modifier.fillMaxWidth().height(bottomFadeHeight).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(bg.copy(alpha = 0f), bg))))
    }
}