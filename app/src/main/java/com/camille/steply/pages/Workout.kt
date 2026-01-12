package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.WorkoutViewModel
import com.camille.steply.viewmodel.WorkoutType
import com.google.android.gms.maps.model.Dash
import androidx.compose.ui.geometry.Offset
import com.camille.steply.R
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PatternItem
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlin.math.roundToInt
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.TextUnit



@Composable
fun WorkoutScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    type: WorkoutType
) {
    val homeState by homeViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val workoutVm: com.camille.steply.viewmodel.WorkoutViewModel = viewModel(
        factory = com.camille.steply.viewmodel.WorkoutVmFactory(context.applicationContext as Application)
    )
    val mapState by workoutVm.state.collectAsState()

    var paused by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableStateOf(0) }

    var initialLatLng by remember { mutableStateOf<LatLng?>(null) }


    LaunchedEffect(Unit) {
        runCatching { homeViewModel.fetchCurrentLatLngOnce() }
            .onSuccess { p -> initialLatLng = LatLng(p.lat, p.lon) }
    }

    // ✅ timer semplice (poi lo sposteremo in un WorkoutViewModel)
    LaunchedEffect(paused) {
        if (!paused) {
            while (true) {
                delay(1000)
                elapsedSec += 1
            }
        }
    }

    LaunchedEffect(Unit) {
        workoutVm.ensureLocationUpdates()
        workoutVm.start()
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

    // Per ora placeholders: poi li colleghiamo a GPS/step/calorie reali
    val kmText = String.format(java.util.Locale.US, "%.2f", mapState.distanceMeters / 1000.0)
    val kcalText = "0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EC))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {

        // -------------------- WORKOUT TOP BAR --------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 3.dp, end = 3.dp, top = 60.dp, bottom = 30.dp)
        ) {

            // ---- TITOLO ----
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = homeState.meteoDesc,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${homeState.meteoTempC}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }

            // ✅ PILL SINISTRA: END (solo quando in pausa)
            if (paused) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                // per ora: torna indietro
                                navController.popBackStack()
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop, // ⛔ icona stop
                            contentDescription = "End",
                            tint = Color.Black,
                            modifier = Modifier.size(25.dp)
                        )

                        Spacer(Modifier.width(5.dp))

                        Text(
                            text = "End",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            // ✅ PILL DESTRA: PAUSE / RESUME (icona cambia)
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp), // ✅ era start, deve essere end
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
                    Icon(
                        imageVector = if (paused) Icons.Default.Refresh else Icons.Default.Pause,
                        contentDescription = if (paused) "Resume" else "Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(25.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = if (paused) "Resume" else "Pause",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        // -------- MAP CARD (Google Map) --------
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFE8E8E8),
            shadowElevation = 12.dp
        ) {
            // 1) se ho punti tracciati uso l’ultimo
            // 2) altrimenti uso la posizione iniziale one-shot
            val lastFromSegments = mapState.segments.lastOrNull()?.points?.lastOrNull()
            val last = lastFromSegments ?: initialLatLng


            if (last == null) {
                // ✅ niente Roma: mostro solo loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Getting your location…",
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val cameraState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(last, 17f)
                }

                LaunchedEffect(last) { cameraState.animate( update = CameraUpdateFactory.newLatLngZoom(last, 17f), durationMs = 600 ) }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false
                    )
                ) {
                    val trackColor = when (type) {
                        WorkoutType.WALK -> Color(0xFF2F80FF)    // blu
                        WorkoutType.RUN -> Color(0xFF9B51E0)     // viola
                        WorkoutType.CYCLING -> Color(0xFF27AE60) // verde
                    }

                    val dashedPattern: List<PatternItem> = listOf(Dash(20f), Gap(14f))

                    mapState.segments.forEach { seg ->
                        if (seg.points.size >= 2) {
                            Polyline(
                                points = seg.points,
                                color = trackColor,
                                width = 10f,
                                pattern = if (seg.dashed) dashedPattern else null,
                                geodesic = true
                            )
                        }
                    }

                    val activityIcon = when (type) {
                        WorkoutType.WALK -> Icons.Default.DirectionsWalk
                        WorkoutType.RUN -> Icons.Default.DirectionsRun
                        WorkoutType.CYCLING -> Icons.Default.DirectionsBike
                    }

                    // ✅ START marker fisso con label sempre visibile
                    val start = mapState.startPoint
                    if (start != null) {
                        val startIcon = rememberStartMarkerIconWithLabel(
                            bgColor = trackColor,
                            icon = activityIcon,
                            label = "Start"
                        )

                        Marker(
                            state = MarkerState(position = start),
                            icon = startIcon,
                            // ancora “spostata” un po’ verso l’alto perché sotto c’è la label
                            anchor = Offset(0.5f, 0.35f)
                        )
                    }
                }
            }
        }



        Spacer(Modifier.height(14.dp))

        // -------- STATS CARD --------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFF8F6F2),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatMini(
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFF7064AF),
                    title = "Duration",
                    value = durationText
                )
                StatMini(
                    icon = Icons.Default.Route,
                    iconColor = Color(0xFF32ADE6),
                    title = "Distance",
                    value = "$kmText km"
                )
                StatMini(
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF9500),
                    title = "Active Energy",
                    value = "$kcalText kcal"
                )
            }
        }
    }
}

// -------------------- STATISTICHE SOTTO--------------------

@Composable
private fun StatMini(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.Start) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF7A7A7A),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = value,
            fontSize = 22.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}


private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

// -------------------- MARKER --------------------

@Composable
private fun rememberStartMarkerIconWithLabel(
    bgColor: Color,
    icon: ImageVector,
    label: String = "Start",
    circleSize: Dp = 34.dp,
    iconSize: Dp = 22.dp,
    labelTextSize: TextUnit = 14.sp,
    labelHPadding: Dp = 8.dp,
    labelVPadding: Dp = 4.dp,
    gapBetween: Dp = 4.dp,
    cornerRadius: Dp = 14.dp
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

        // Android Paint per testo
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

        // cerchio colorato
        val circlePaint = androidx.compose.ui.graphics.Paint().apply { color = bgColor }
        canvas.drawCircle(Offset(centerX, circleCenterY), circlePx / 2f, circlePaint)

        // icona bianca al centro
        val iconLeft = centerX - iconPx / 2f
        val iconTop = circleCenterY - iconPx / 2f

        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = IntSize(bmpW, bmpH).toSize()
        ) {
            translate(iconLeft, iconTop) {
                with(painter) {
                    draw(
                        size = androidx.compose.ui.geometry.Size(iconPx, iconPx),
                        alpha = 1f,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }

            // label sotto (sempre visibile)
            val labelLeft = centerX - labelW / 2f
            val labelTop = circlePx + gapPx
            val labelRight = labelLeft + labelW
            val labelBottom = labelTop + labelH

            drawIntoCanvas { c ->
                val native = c.nativeCanvas

                val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                }

                native.drawRoundRect(
                    labelLeft, labelTop, labelRight, labelBottom,
                    cornerPx, cornerPx,
                    bgPaint
                )

                val textX = centerX - (textWidth / 2f)
                val baseline = labelTop + padVPx - fm.ascent
                native.drawText(label, textX, baseline, textPaint)
            }
        }

        BitmapDescriptorFactory.fromBitmap(imageBitmap.asAndroidBitmap())
    }
}

