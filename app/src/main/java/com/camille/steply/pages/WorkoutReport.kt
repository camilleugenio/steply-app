package com.camille.steply.pages

import android.graphics.Typeface
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.camille.steply.viewmodel.WorkoutType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PatternItem
import com.google.maps.android.compose.*
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable


// -------------------- SNAPSHOT (Parcelable) --------------------

@Parcelize
@Serializable
data class LatLngP(val lat: Double, val lon: Double) : Parcelable

@Parcelize
@Serializable
data class TrackSegmentP(
    val dashed: Boolean,
    val points: List<LatLngP>
) : Parcelable

@Parcelize
@Serializable
data class WorkoutReportSnapshot(
    val type: String,
    val startTimeMs: Long,
    val durationSec: Int,
    val distanceMeters: Double,
    val kcal: Int,
    val meteoEmoji: String,
    val meteoTempC: String,
    val startPoint: LatLngP?,
    val segments: List<TrackSegmentP>
) : Parcelable

private const val SNAPSHOT_KEY = "workout_report_snapshot"

// -------------------- SCREEN --------------------

@Composable
fun WorkoutReportScreen(
    navController: NavController
) {
    // Legge lo snapshot dalla savedStateHandle
    val snapshot = remember {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<WorkoutReportSnapshot>(SNAPSHOT_KEY)
    }

    if (snapshot == null) {
        // fallback minimale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F1EC)),
            contentAlignment = Alignment.Center
        ) {
            Text("Report not available", color = Color.Black)
        }
        return
    }

    val type = runCatching { WorkoutType.valueOf(snapshot.type) }.getOrElse { WorkoutType.WALK }

    val trackColor = when (type) {
        WorkoutType.WALK -> Color(0xFF2F80FF)
        WorkoutType.RUN -> Color(0xFF9B51E0)
        WorkoutType.CYCLING -> Color(0xFF27AE60)
    }

    val activityIcon = when (type) {
        WorkoutType.WALK -> Icons.Default.DirectionsWalk
        WorkoutType.RUN -> Icons.Default.DirectionsRun
        WorkoutType.CYCLING -> Icons.Default.DirectionsBike
    }

    val title = when (type) {
        WorkoutType.WALK -> "Walk"
        WorkoutType.RUN -> "Run"
        WorkoutType.CYCLING -> "Cycling"
    }

    val durationText = remember(snapshot.durationSec) { formatDuration(snapshot.durationSec) }
    val km = snapshot.distanceMeters / 1000.0
    val kmText = String.format(Locale.US, "%.2f", km)

    // ✅ velocità media: km/h
    val avgSpeedKmh = if (snapshot.durationSec > 0) {
        (km / (snapshot.durationSec / 3600.0))
    } else 0.0
    val avgSpeedText = String.format(Locale.US, "%.1f", avgSpeedKmh)

    val dateText = remember(snapshot.startTimeMs) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.ENGLISH)
        sdf.format(Date(snapshot.startTimeMs))
            .replaceFirstChar { it.uppercase() }
    }

    // punti mappa
    val start = snapshot.startPoint?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }
    val end = snapshot.segments.lastOrNull()?.points?.lastOrNull()
        ?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }

    val allPoints = snapshot.segments.flatMap { it.points }
    val initialCenter = end
        ?: start
        ?: allPoints.firstOrNull()?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }

    Box(modifier = Modifier.fillMaxSize()) {

        // -------------------- MAP (full screen) --------------------
        if (initialCenter != null) {
            val cameraState = rememberCameraPositionState {
                position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(initialCenter, 16f)
            }

            LaunchedEffect(initialCenter) {
                cameraState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(initialCenter, 16.5f),
                    durationMs = 550
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                val dashedPattern: List<PatternItem> = listOf(Dash(20f), Gap(14f))

                snapshot.segments.forEach { seg ->
                    val pts = seg.points.map { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }
                    if (pts.size >= 2) {
                        Polyline(
                            points = pts,
                            color = trackColor,
                            width = 10f,
                            pattern = if (seg.dashed) dashedPattern else null,
                            geodesic = true
                        )
                    }
                }

                // START marker
                if (start != null) {
                    val startIcon = rememberStartMarkerIconWithLabel(
                        bgColor = trackColor,
                        icon = activityIcon,
                        label = "Start"
                    )
                    Marker(
                        state = MarkerState(position = start),
                        icon = startIcon,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.35f)
                    )
                }

                // FINISH marker (bandiera)
                if (end != null) {
                    val finishIcon = rememberStartMarkerIconWithLabel(
                        bgColor = trackColor,
                        icon = Icons.Default.Flag,
                        label = "Finish"
                    )

                    Marker(
                        state = MarkerState(position = end),
                        icon = finishIcon,
                        anchor = Offset(0.5f, 0.35f) // stesso anchor di Start
                    )
                }

            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8E8E8)),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading map…", color = Color(0xFF666666), fontWeight = FontWeight.Medium)
            }
        }

        // -------------------- TOP LEFT X --------------------
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 14.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    navController.navigate(Routes.ACTIVITY) {
                        launchSingleTop = true
                        popUpTo(Routes.ACTIVITY) { inclusive = false }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }

        // -------------------- REPORT CARD (overlay) --------------------
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp,
                    end = 16.dp,
                    bottom = 36.dp ),
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Header row: activity left, weather right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: icon + title + date
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(trackColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activityIcon,
                                contentDescription = null,
                                tint = trackColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = dateText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B6B6B)
                            )
                        }
                    }

                    // Right: meteo
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = snapshot.meteoEmoji, fontSize = 20.sp)
                        Text(
                            text = "${snapshot.meteoTempC}°C",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Stats grid (2x2)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBig(
                            icon = Icons.Default.Timer,
                            iconTint = Color(0xFF7064AF),
                            title = "Duration",
                            value = durationText
                        )
                        StatBig(
                            icon = Icons.Default.Route,
                            iconTint = Color(0xFF32ADE6),
                            title = "Distance",
                            value = "$kmText km"
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBig(
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFFF9500),
                            title = "Total Energy",
                            value = "${snapshot.kcal} kcal"
                        )
                        StatBig(
                            icon = Icons.Default.Speed,
                            iconTint = Color(0xFF27AE60),
                            title = "Avg Speed",
                            value = "$avgSpeedText km/h"
                        )
                    }
                }
            }
        }
    }
}

// -------------------- UI pieces --------------------

@Composable
private fun StatBig(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Column(modifier = Modifier.widthIn(min = 140.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF7A7A7A),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(5.dp))
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

// -------------------- FINISH MARKER (flag) --------------------

@Composable
private fun rememberFinishMarkerIcon(
    bgColor: Color,
    icon: ImageVector,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp
): BitmapDescriptor {
    val density = LocalDensity.current
    val painter = rememberVectorPainter(image = icon)

    return remember(bgColor, icon, density) {
        val sPx = with(density) { size.toPx() }
        val iconPx = with(density) { iconSize.toPx() }

        val bmpW = sPx.toInt()
        val bmpH = (sPx * 1.2f).toInt()

        val imageBitmap = ImageBitmap(bmpW, bmpH)
        val canvas = Canvas(imageBitmap)

        val centerX = bmpW / 2f
        val circleCenterY = sPx / 2f

        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = IntSize(bmpW, bmpH).toSize()
        ) {
            drawCircle(
                color = bgColor,
                radius = sPx / 2f,
                center = Offset(centerX, circleCenterY)
            )

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(centerX - sPx * 0.16f, sPx)
                lineTo(centerX + sPx * 0.16f, sPx)
                lineTo(centerX, sPx * 1.16f)
                close()
            }
            drawPath(path, bgColor)
        }

        // icona bandiera su bitmap separata
        val iconBitmap = ImageBitmap(iconPx.toInt(), iconPx.toInt())
        val iconCanvas = Canvas(iconBitmap)
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = iconCanvas,
            size = IntSize(iconPx.toInt(), iconPx.toInt()).toSize()
        ) {
            with(painter) {
                draw(
                    size = androidx.compose.ui.geometry.Size(iconPx, iconPx),
                    alpha = 1f,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        val native = canvas.nativeCanvas
        val left = centerX - iconPx / 2f
        val top = circleCenterY - iconPx / 2f
        native.drawBitmap(iconBitmap.asAndroidBitmap(), left, top, null)

        BitmapDescriptorFactory.fromBitmap(imageBitmap.asAndroidBitmap())
    }
}



// -------------------- START MARKER --------------------

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

