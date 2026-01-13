package com.camille.steply.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import com.camille.steply.viewmodel.WorkoutType
import com.camille.steply.viewmodel.workoutColor
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import com.camille.steply.viewmodel.ActivityEvent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect


@Composable
fun ActivityScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val activityViewModel: ActivityViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val activityState by activityViewModel.uiState.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()

    var navigating by remember { mutableStateOf(false) }
    var navType by remember { mutableStateOf<WorkoutType?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // SE COUNTDOWN ATTIVO: SPARISCE TUTTO E MOSTRA SOLO QUESTO
    if (activityState.isCountingDown && activityState.countdownType != null) {
        CountdownFullScreen(
            number = activityState.secondsLeft,
            label = activityState.phaseText,
            color = workoutColor(activityState.countdownType!!)
        )
        return
    }

// ✅ Se sto navigando: schermo neutro (così NON compare più “3”)
    if (navigating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F1EC))
        )
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


    // ---------------- UI NORMALE ----------------

    Scaffold(
        containerColor = Color(0xFFF4F1EC),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = 1,
                    onSelect = { index ->
                        when (index) {
                            0 -> {
                                val popped = navController.popBackStack(Routes.STEPS, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Routes.STEPS) { launchSingleTop = true }
                                }
                            }

                            1 -> Unit

                            2 -> {
                                val popped = navController.popBackStack(Routes.PROFILE, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                                }
                            }
                        }
                    }
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

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ActivityWeatherHeader(
                        temp = uiState.meteoTempC,
                        emoji = uiState.meteoDesc,
                        place = uiState.currentPlacename,
                        loading = uiState.meteoLoading
                    )
                }
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Click + to start your first workout",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E8E93)
            )
        }

    }
}



// -------------------- TOP BAR --------------------

@Composable
private fun ActivitiesTopBar(
    onWorkoutSelected: (WorkoutType) -> Unit
) {
    var showWorkoutPicker by remember { mutableStateOf(false) }

    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
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


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "All Activities",
            fontSize = 28.sp,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        // ---- + con popup ancorato ----
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(8.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showWorkoutPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            // offset del popup (in PX) calcolato dai DP
            val popupOffset = with(LocalDensity.current) {
                IntOffset(x = (-10).dp.roundToPx(), y = 60.dp.roundToPx())
            }

            if (keepInComposition) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = popupOffset,
                    onDismissRequest = { showWorkoutPicker = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer(
                            alpha = alpha.value,
                            translationY = popupAnimOffsetY.value // questo è px float, ok
                        )
                            .width(180.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 210.dp)
                                    .padding(horizontal = 18.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Start a workout",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7A7A7A),
                                    fontSize = 16.sp
                                )

                                Spacer(Modifier.height(14.dp))

                                WorkoutRow(
                                    label = "Walk",
                                    icon = Icons.Default.DirectionsWalk,
                                    iconTint = Color(0xFF2F80FF),
                                    onClick = {
                                        showWorkoutPicker = false
                                        onWorkoutSelected(WorkoutType.WALK)
                                    }
                                )
                                WorkoutRow(
                                    label = "Run",
                                    icon = Icons.Default.DirectionsRun,
                                    iconTint = Color(0xFF9B51E0),
                                    onClick = {
                                        showWorkoutPicker = false
                                        onWorkoutSelected(WorkoutType.RUN)
                                    }
                                )
                                WorkoutRow(
                                    label = "Cycling",
                                    icon = Icons.Default.DirectionsBike,
                                    iconTint = Color(0xFF27AE60),
                                    onClick = {
                                        showWorkoutPicker = false
                                        onWorkoutSelected(WorkoutType.CYCLING)
                                    }
                                )
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
fun WorkoutRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

// -------------------- WEATHER --------------------

@Composable
private fun ActivityWeatherHeader(
    temp: String,
    emoji: String,
    place: String,
    loading: Boolean
) {
    if (loading) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 70.sp
        )

        Spacer(Modifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.Start
        ){
            Text(
                text = place,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
            Text(
                text = "$temp°C",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

// -------------------- COUNTDOWN --------------------

@Composable
private fun CountdownFullScreen(
    number: Int,
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EC)), // ✅ sfondo fisso
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number.toString(),
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = color // ✅ colore attività SOLO sul numero
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                color = color.copy(alpha = 0.7f) // ✅ stesso colore, più soft
            )
        }
    }
}


