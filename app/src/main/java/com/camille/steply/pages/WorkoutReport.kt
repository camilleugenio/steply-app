package com.camille.steply.pages

import android.graphics.Typeface
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    val snapshot = remember {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<WorkoutReportSnapshot>(SNAPSHOT_KEY)
    }

    if (snapshot == null) {
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

    // ✅ Responsive knobs
    val config = LocalConfiguration.current
    val screenW = config.screenWidthDp
    val screenHdp = config.screenHeightDp
    val isSmall = screenW < 380

    val hPad = if (isSmall) 14.dp else 16.dp
    val cardBottomPad = if (isSmall) 14.dp else 22.dp
    val cardInnerPad = if (isSmall) 14.dp else 18.dp
    val cardCorner = if (isSmall) 26.dp else 30.dp

    val titleSize = if (isSmall) 18.sp else 20.sp
    val dateSize = if (isSmall) 12.sp else 13.sp
    val valueSize = if (isSmall) 20.sp else 22.sp
    val statGap = if (isSmall) 12.dp else 18.dp

    // card max height ~40-45% schermo (con cap)
    val cardMaxH = minOf((screenHdp.dp * 0.45f), if (isSmall) 340.dp else 380.dp)

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

    val avgSpeedKmh = if (snapshot.durationSec > 0) (km / (snapshot.durationSec / 3600.0)) else 0.0
    val avgSpeedText = String.format(Locale.US, "%.1f", avgSpeedKmh)

    val dateText = remember(snapshot.startTimeMs) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.ENGLISH)
        sdf.format(Date(snapshot.startTimeMs)).replaceFirstChar { it.uppercase() }
    }

    // punti mappa
    val start = snapshot.startPoint?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }
    val end = snapshot.segments.lastOrNull()?.points?.lastOrNull()
        ?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }

    // tutti i punti convertiti in LatLng Google
    val allPtsG = remember(snapshot) {
        snapshot.segments
            .flatMap { it.points }
            .map { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }
    }

    // bounds che include tutto il percorso
    val bounds = remember(allPtsG) {
        if (allPtsG.isNotEmpty()) {
            val b = com.google.android.gms.maps.model.LatLngBounds.Builder()
            allPtsG.forEach { b.include(it) }
            b.build()
        } else null
    }


    Box(modifier = Modifier.fillMaxSize()) {

        // -------------------- MAP (full screen) --------------------
        if (bounds != null || start != null || end != null) {

            val cameraState = rememberCameraPositionState()

            val density = LocalDensity.current

            LaunchedEffect(bounds, isSmall, start, end) {
                val paddingPx = with(density) {
                    (if (isSmall) 140.dp else 160.dp).roundToPx()
                }

                when {
                    bounds != null && allPtsG.size >= 2 -> {
                        cameraState.animate(
                            update = CameraUpdateFactory.newLatLngBounds(bounds, paddingPx),
                            durationMs = 650
                        )
                    }
                    else -> {
                        val p = end ?: start
                        if (p != null) {
                            cameraState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(p, 16.5f),
                                durationMs = 550
                            )
                        }
                    }
                }
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

                if (start != null) {
                    val startIcon = rememberStartMarkerIconWithLabel(
                        bgColor = trackColor,
                        icon = activityIcon,
                        label = "Start"
                    )
                    Marker(
                        state = MarkerState(position = start),
                        icon = startIcon,
                        anchor = Offset(0.5f, 0.35f)
                    )
                }

                if (end != null) {
                    val finishIcon = rememberStartMarkerIconWithLabel(
                        bgColor = trackColor,
                        icon = Icons.Default.Flag,
                        label = "Finish"
                    )
                    Marker(
                        state = MarkerState(position = end),
                        icon = finishIcon,
                        anchor = Offset(0.5f, 0.35f)
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
                .padding(start = hPad, top = 12.dp)
                .size(if (isSmall) 42.dp else 44.dp)
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
                modifier = Modifier.size(if (isSmall) 20.dp else 22.dp)
            )
        }

        // -------------------- REPORT CARD (overlay) --------------------
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = hPad, end = hPad, bottom = cardBottomPad)
                .heightIn(max = cardMaxH),
            shape = RoundedCornerShape(cardCorner),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(cardInnerPad)) {

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSmall) 40.dp else 42.dp)
                                .clip(CircleShape)
                                .background(trackColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activityIcon,
                                contentDescription = null,
                                tint = trackColor,
                                modifier = Modifier.size(if (isSmall) 20.dp else 22.dp)
                            )
                        }

                        Spacer(Modifier.width(if (isSmall) 10.dp else 12.dp))

                        Column {
                            Text(
                                text = title,
                                fontSize = titleSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = dateText,
                                fontSize = dateSize,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B6B6B)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = snapshot.meteoEmoji, fontSize = if (isSmall) 18.sp else 20.sp)
                        Text(
                            text = "${snapshot.meteoTempC}°C",
                            fontSize = if (isSmall) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(if (isSmall) 14.dp else 24.dp))

// ✅ 2x2 SEMPRE, anche su schermi piccoli
                val cellGap = if (isSmall) 10.dp else 16.dp
                val rowGap = if (isSmall) 12.dp else 18.dp

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        StatBigGridCell(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            iconTint = Color(0xFF7064AF),
                            title = "Duration",
                            value = durationText,
                            isSmall = isSmall
                        )
                        StatBigGridCell(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Route,
                            iconTint = Color(0xFF32ADE6),
                            title = "Distance",
                            value = "$kmText km",
                            isSmall = isSmall
                        )
                    }

                    Spacer(Modifier.height(rowGap))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        StatBigGridCell(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFFF9500),
                            title = "Total Energy",
                            value = "${snapshot.kcal} kcal",
                            isSmall = isSmall
                        )
                        StatBigGridCell(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Speed,
                            iconTint = Color(0xFF27AE60),
                            title = "Avg Speed",
                            value = "$avgSpeedText km/h",
                            isSmall = isSmall
                        )
                    }
                }
            }
        }
    }
}

// -------------------- UI pieces --------------------

@Composable
private fun StatBigGridCell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    isSmall: Boolean
) {
    val titleSize = if (isSmall) 12.sp else 13.sp
    val valueSize = if (isSmall) 18.sp else 22.sp
    val iconSize = if (isSmall) 15.dp else 16.dp
    val topGap = if (isSmall) 4.dp else 5.dp

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = titleSize,
                color = Color(0xFF7A7A7A),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(topGap))

        Text(
            text = value,
            fontSize = valueSize,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
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
