package com.camille.steply.pages

import android.app.Application
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import com.camille.steply.service.WorkoutLocationService
import com.camille.steply.viewmodel.ActivityViewModel
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.WorkoutType
import com.camille.steply.viewmodel.WorkoutViewModel
import com.camille.steply.viewmodel.WorkoutVmFactory
import com.camille.steply.viewmodel.WorkoutHistoryViewModel
import com.camille.steply.viewmodel.WorkoutReportSnapshot
import com.camille.steply.viewmodel.LatLngP
import com.camille.steply.viewmodel.TrackSegmentP
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun WorkoutScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    activityViewModel: ActivityViewModel = viewModel(),
    type: WorkoutType
) {
    val homeState by homeViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val workoutVm: WorkoutViewModel = viewModel(
        factory = WorkoutVmFactory(context.applicationContext as Application)
    )
    val mapState by workoutVm.state.collectAsState()

    var paused by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableStateOf(0) }
    var initialLatLng by remember { mutableStateOf<LatLng?>(null) }

    val kcal by workoutVm.kcal.collectAsState()

    val historyVm: WorkoutHistoryViewModel = viewModel()

    val serverId by workoutVm.currentWorkoutId.collectAsState()

    LaunchedEffect(serverId) {
        val id = serverId
        if (id != null) {
            activityViewModel.updateCurrentWorkoutId(id)
            Log.d("SYNC", "ID PythonAnywhere consegnato a ActivityViewModel: $id")
        }
    }

    LaunchedEffect(Unit) {
        runCatching { homeViewModel.fetchCurrentLatLngOnce() }
            .onSuccess { p -> initialLatLng = LatLng(p.lat, p.lon) }
    }

    LaunchedEffect(paused) {
        if (!paused) {
            while (true) {
                delay(1000)
                elapsedSec += 1
            }
        }
    }

    LaunchedEffect(Unit) {
        activityViewModel.startNewWorkout(type)
        Log.d("KCAL_UI", "WorkoutScreen started")
        val i = Intent(context, WorkoutLocationService::class.java).apply {
            action = WorkoutLocationService.ACTION_START
        }
        ContextCompat.startForegroundService(context, i)

        workoutVm.ensureLocationUpdates()
        workoutVm.start()
    }

    LaunchedEffect(type, homeState.weightKg) {
        val weightKg = if (homeState.weightKg == null || homeState.weightKg == 0.0) 70.0 else homeState.weightKg!!

        val activity = when (type) {
            WorkoutType.RUN -> "run"
            WorkoutType.WALK -> "walk"
            WorkoutType.CYCLING -> "cycle"
        }

        val ageYears = 27
        val sex = "male"

        Log.d("KCAL_UI", "Calling startRemoteSessionAndLoop with weight=$weightKg")
        workoutVm.startRemoteSessionAndLoop(
            activity = activity,
            weightKg = weightKg,
            ageYears = ageYears,
            sex = sex,
            elapsedSecProvider = { elapsedSec },
            intervalMs = 10_000L
        )
    }

    LaunchedEffect(paused) {
        if (paused) workoutVm.pause() else workoutVm.resume()
    }

    val title = when (type) {
        WorkoutType.RUN -> "RUN"
        WorkoutType.WALK -> "WALK"
        WorkoutType.CYCLING -> "CYCLING"
    }

    val durationText = remember(elapsedSec) { formatDuration(elapsedSec) }
    val kmText = String.format(java.util.Locale.US, "%.2f", mapState.distanceMeters / 1000.0)
    val kcalText = kcal.roundToInt().toString()

    val config = LocalConfiguration.current
    val screenH = config.screenHeightDp.dp
    val mapMinH = 260.dp
    val mapMaxH = minOf((screenH * 0.60f), 580.dp).coerceAtLeast(320.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EC))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // TOP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 3.dp, end = 3.dp, top = 20.dp, bottom = 22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = homeState.meteoDesc, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(text = "${homeState.meteoTempC}°C", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                }
            }

            if (paused) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                // 1. Stop Services
                                val stopIntent = Intent(context, WorkoutLocationService::class.java).apply {
                                    action = WorkoutLocationService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                                workoutVm.stopKcalLoop()

                                val st = mapState
                                val finalKcal = kcal

                                val idPerSalvataggio = activityViewModel.uiState.value.currentWorkoutId
                                    ?: serverId
                                    ?: "TEMP_${System.currentTimeMillis()}"

                                Log.d("SYNC_DEBUG", "ID utilizzato per salvataggio e snapshot: $idPerSalvataggio")

                                // 2. Map Points
                                val googleGeoPoints = st.segments.flatMap { it.points }.map {
                                    com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
                                }

                                val currentId = activityViewModel.uiState.value.currentWorkoutId
                                    ?: serverId
                                    ?: "TEMP_${System.currentTimeMillis()}"

                                Log.d("DEBUG_SAVE", "ID recuperato per lo snapshot: $currentId")

                                // 3. Save to Firestore
                                activityViewModel.finishAndSaveWorkout(
                                    tipo = type.name,
                                    durataSec = elapsedSec.toLong(),
                                    km = st.distanceMeters / 1000.0,
                                    calorie = finalKcal,
                                    percorso = googleGeoPoints,
                                    meteoEmoji = homeState.meteoDesc,
                                    meteoTempC = homeState.meteoTempC
                                )

                                // 4. Prepare Snapshot
                                val snapshot = WorkoutReportSnapshot(
                                    idAllenamento = idPerSalvataggio,
                                    type = type.name,
                                    startTimeMs = System.currentTimeMillis() - (elapsedSec * 1000L),
                                    durationSec = elapsedSec,
                                    distanceMeters = st.distanceMeters,
                                    kcal = finalKcal.roundToInt(),
                                    meteoEmoji = homeState.meteoDesc,
                                    meteoTempC = homeState.meteoTempC,
                                    startPoint = st.startPoint?.let { LatLngP(it.latitude, it.longitude) },
                                    segments = st.segments.map { seg ->
                                        TrackSegmentP(
                                            dashed = seg.dashed,
                                            points = seg.points.map { p -> LatLngP(p.latitude, p.longitude) }
                                        )
                                    }
                                )

                                // 5. Navigation
                                navController.getBackStackEntry(Routes.ACTIVITY)
                                    .savedStateHandle.set(Routes.WORKOUT_REPORT_SNAPSHOT, snapshot)
                                navController.getBackStackEntry(Routes.ACTIVITY)
                                    .savedStateHandle.set(Routes.FROM_HISTORY, false)

                                navController.navigate(Routes.WORKOUT_REPORT) {
                                    launchSingleTop = true
                                    popUpTo(Routes.ACTIVITY) { inclusive = false }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "End", tint = Color.Black, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(text = "End", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { paused = !paused }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = if (paused) Icons.Default.Refresh else Icons.Default.Pause, contentDescription = if (paused) "Resume" else "Pause", tint = Color.Black, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(text = if (paused) "Resume" else "Pause", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // MAP CARD
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = mapMinH, max = mapMaxH),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFE8E8E8),
            shadowElevation = 12.dp
        ) {
            val lastFromSegments = mapState.segments.lastOrNull()?.points?.lastOrNull()
            val last = lastFromSegments ?: initialLatLng

            if (last == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Getting your location…", color = Color(0xFF666666), fontWeight = FontWeight.Medium)
                }
            } else {
                val cameraState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(last, 17f) }
                LaunchedEffect(last) { cameraState.animate(update = CameraUpdateFactory.newLatLngZoom(last, 17f), durationMs = 600) }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                ) {
                    val trackColor = when (type) {
                        WorkoutType.WALK -> Color(0xFF2F80FF)
                        WorkoutType.RUN -> Color(0xFF9B51E0)
                        WorkoutType.CYCLING -> Color(0xFF27AE60)
                    }
                    val dashedPattern: List<PatternItem> = listOf(Dash(20f), Gap(14f))

                    mapState.segments.forEach { seg ->
                        if (seg.points.size >= 2) {
                            Polyline(points = seg.points, color = trackColor, width = 10f, pattern = if (seg.dashed) dashedPattern else null, geodesic = true)
                        }
                    }

                    val start = mapState.startPoint
                    if (start != null) {
                        val activityIcon = when (type) {
                            WorkoutType.WALK -> Icons.Default.DirectionsWalk
                            WorkoutType.RUN -> Icons.Default.DirectionsRun
                            WorkoutType.CYCLING -> Icons.Default.DirectionsBike
                        }
                        val startIcon = rememberStartMarkerIconWithLabel(bgColor = trackColor, icon = activityIcon, label = "Start")
                        Marker(state = MarkerState(position = start), icon = startIcon, anchor = Offset(0.5f, 0.35f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // STATS CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFF8F6F2),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatMini(icon = Icons.Default.Timer, iconColor = Color(0xFF7064AF), title = "Duration", value = durationText)
                StatMini(icon = Icons.Default.Route, iconColor = Color(0xFF32ADE6), title = "Distance", value = "$kmText km")
                StatMini(icon = Icons.Default.LocalFireDepartment, iconColor = Color(0xFFFF9500), title = "Active Energy", value = "$kcalText kcal")
            }
        }
    }
}

@Composable
private fun StatMini(icon: ImageVector, iconColor: Color, title: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = title, fontSize = 13.sp, color = Color(0xFF7A7A7A), fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        Text(text = value, fontSize = 22.sp, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun rememberStartMarkerIconWithLabel(
    bgColor: Color, icon: ImageVector, label: String = "Start",
    circleSize: Dp = 34.dp, iconSize: Dp = 22.dp, labelTextSize: TextUnit = 14.sp,
    labelHPadding: Dp = 8.dp, labelVPadding: Dp = 4.dp, gapBetween: Dp = 4.dp, cornerRadius: Dp = 14.dp
): BitmapDescriptor {
    val density = LocalDensity.current
    val painter = rememberVectorPainter(image = icon)
    return remember(bgColor, icon, label, density) {
        val circlePx = with(density) { circleSize.toPx() }
        val iconPx = with(density) { iconSize.toPx() }
        val textPx = with(density) { labelTextSize.toPx() }
        val padHPx = with(density) { labelHPadding.toPx() }
        val padVPx = with(density) { labelVPadding.toPx() }
        val gapPx = with(density) { gapBetween.toPx() }
        val cornerPx = with(density) { cornerRadius.toPx() }
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = textPx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fm = textPaint.fontMetrics
        val textWidth = textPaint.measureText(label)
        val textHeight = (fm.descent - fm.ascent)
        val labelW = textWidth + 2f * padHPx
        val labelH = textHeight + 2f * padVPx
        val bmpW = maxOf(circlePx, labelW).roundToInt()
        val bmpH = (circlePx + gapPx + labelH).roundToInt()
        val imageBitmap = ImageBitmap(bmpW, bmpH)
        val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)
        val centerX = bmpW / 2f
        val circleCenterY = circlePx / 2f
        val circlePaint = androidx.compose.ui.graphics.Paint().apply { color = bgColor }
        canvas.drawCircle(Offset(centerX, circleCenterY), circlePx / 2f, circlePaint)
        val iconLeft = centerX - iconPx / 2f
        val iconTop = circleCenterY - iconPx / 2f
        val drawScope = CanvasDrawScope()
        drawScope.draw(density = density, layoutDirection = LayoutDirection.Ltr, canvas = canvas, size = IntSize(bmpW, bmpH).toSize()) {
            translate(iconLeft, iconTop) { with(painter) { draw(size = androidx.compose.ui.geometry.Size(iconPx, iconPx), alpha = 1f, colorFilter = ColorFilter.tint(Color.White)) } }
            val labelLeft = centerX - labelW / 2f
            val labelTop = circlePx + gapPx
            val labelRight = labelLeft + labelW
            val labelBottom = labelTop + labelH
            drawIntoCanvas { c ->
                val native = c.nativeCanvas
                val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
                native.drawRoundRect(labelLeft, labelTop, labelRight, labelBottom, cornerPx, cornerPx, bgPaint)
                val textX = centerX - (textWidth / 2f)
                val baseline = labelTop + padVPx - fm.ascent
                native.drawText(label, textX, baseline, textPaint)
            }
        }
        BitmapDescriptorFactory.fromBitmap(imageBitmap.asAndroidBitmap())
    }
}