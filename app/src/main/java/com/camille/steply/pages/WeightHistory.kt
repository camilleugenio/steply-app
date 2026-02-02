package com.camille.steply.pages

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.camille.steply.viewmodel.ProfileViewModel
import com.camille.steply.viewmodel.WeightViewModel
import com.camille.steply.viewmodel.WeightEntry
import java.util.Locale

private val BgColor = Color(0xFFF4F1EC)
private val AccentColor = Color(0xFFFF8A00)
private val GridColor = Color.LightGray.copy(alpha = 0.3f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightHistory(navController: NavHostController) {
    val profileViewModel: ProfileViewModel = viewModel()
    val weightViewModel: WeightViewModel = viewModel()

    val profileUiState by profileViewModel.uiState.collectAsState()
    val weightUiState by weightViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly", "Monthly")

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val filteredHistory = remember(weightUiState.weightHistory, selectedTab) {
        val base = weightUiState.weightHistory.sortedBy { it.timestamp }
        when (selectedTab) {
            0 -> base.takeLast(7)
            1 -> base.takeLast(30)
            else -> base
        }
    }

    // Prendiamo il peso attuale dal ProfileViewModel
    val currentWeightValue = profileUiState.weight.replace(",", ".").toDoubleOrNull() ?: 0.0

    Scaffold(
        containerColor = BgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Weight Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { TabSelector(tabs, selectedTab) { selectedTab = it } }

            item {
                Column {
                    Text("Weight Overview", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    EditableStatCard(
                        label = "Current Weight",
                        value = String.format(Locale.US, "%.1f kg", currentWeightValue),
                        onClick = { showSheet = true }
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(top = 32.dp, bottom = 24.dp, start = 12.dp, end = 24.dp)
                ) {
                    key(filteredHistory, selectedTab) {
                        WeightGridChartMinimal(history = filteredHistory)
                    }
                }
            }

            item {
                Text("History", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            if (weightUiState.weightHistory.isEmpty()) {
                item { Text("No records found.", color = Color.Gray) }
            } else {
                items(weightUiState.weightHistory.sortedByDescending { it.timestamp }) { entry ->
                    HistoryItem(entry.date, entry.weight)
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                WeightUpdateSheetContent(
                    initialWeight = profileUiState.weight,
                    onSave = { newWeight ->
                        // Usiamo il WeightViewModel per aggiungere la voce e aggiornare il DB
                        weightViewModel.addWeightEntry(newWeight) {
                            showSheet = false
                        }
                    },
                    onCancel = { showSheet = false }
                )
            }
        }
    }
}

@Composable
fun WeightGridChartMinimal(history: List<WeightEntry>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (history.isEmpty()) return@Canvas

        val labelWidth = 60.dp.toPx()
        val chartWidth = size.width - labelWidth
        val chartHeight = size.height - 35.dp.toPx()

        val weights = history.map { it.weight.toFloat() }
        val maxW = (weights.maxOrNull() ?: 0f) + 1.5f
        val minW = (weights.minOrNull() ?: 0f) - 1.5f
        val range = (maxW - minW).coerceAtLeast(2f)

        repeat(5) { i ->
            val y = i * (chartHeight / 4)
            val value = maxW - i * (range / 4)
            drawLine(GridColor, Offset(labelWidth, y), Offset(size.width, y), 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.US, "%.1f", value),
                10f, y + 10f,
                Paint().apply { color = android.graphics.Color.GRAY; textSize = 28f }
            )
        }

        if (history.size >= 2) {
            val stepX = chartWidth / (history.size - 1)
            val path = Path()
            history.forEachIndexed { index, entry ->
                val x = labelWidth + stepX * index
                val y = chartHeight - ((entry.weight.toFloat() - minW) * chartHeight / range)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                if (index == 0 || index == history.lastIndex || (history.size > 2 && index == history.size / 2)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.date.takeLast(5), x, size.height,
                        Paint().apply { color = android.graphics.Color.GRAY; textAlign = Paint.Align.CENTER; textSize = 26f }
                    )
                }
            }
            drawPath(path, AccentColor, style = Stroke(width = 8f, cap = StrokeCap.Round))

            val lastX = labelWidth + stepX * history.lastIndex
            val lastY = chartHeight - ((history.last().weight.toFloat() - minW) * chartHeight / range)
            drawCircle(AccentColor, 10f, Offset(lastX, lastY))
            drawCircle(Color.White, 5f, Offset(lastX, lastY))
        }
    }
}

@Composable
fun EditableStatCard(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, color = Color.Gray, fontSize = 14.sp)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Icon(Icons.Default.Edit, contentDescription = null, tint = AccentColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun WeightUpdateSheetContent(initialWeight: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var textValue by remember { mutableStateOf(initialWeight) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Edit Weight", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = { Text("Current weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(textValue) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
        ) { Text("Save", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onCancel) { Text("Cancel", color = Color.Gray) }
    }
}

@Composable
fun TabSelector(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(48.dp), color = Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(4.dp)) {
            tabs.forEachIndexed { index, title ->
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onTabSelected(index) },
                    color = if (index == selectedIndex) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(title, fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium, color = if (index == selectedIndex) Color.Black else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(date: String, weight: Double) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(date, fontWeight = FontWeight.Medium)
            Text(String.format(Locale.US, "%.1f kg", weight), fontWeight = FontWeight.Bold, color = AccentColor)
        }
    }
}