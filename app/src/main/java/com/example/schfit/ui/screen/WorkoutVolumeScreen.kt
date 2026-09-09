package com.example.schfit.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schfit.R
import com.example.schfit.ui.viewmodel.ProgressUiState
import com.example.schfit.ui.viewmodel.ProgressViewModel
import com.example.schfit.ui.viewmodel.WorkoutDetailState
import com.example.schfit.ui.viewmodel.WorkoutHistoryUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutVolumeScreen(
    viewModel: ProgressViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workout_volume_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Hacim & Antrenman Özet Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.total_volume),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.totalOverallVolume >= 1000) "%.1f %s".format(uiState.totalOverallVolume / 1000, stringResource(R.string.ton)) else "%.0f kg".format(uiState.totalOverallVolume),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.total_workouts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.totalWorkoutsCount}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Antrenman Hacim Grafiği Kartı
            WorkoutVolumeChartCard(history = uiState.workoutHistory)

            // "Antrenman Geçmişi" Buton Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { viewModel.openHistoryDialog() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF7B1FA2),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.workout_volume_history_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.workout_volume_history_desc, uiState.workoutHistory.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = stringResource(R.string.workout_volume_history_title),
                        tint = Color(0xFF7B1FA2),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Antrenman Geçmişi Tam Liste Diyaloğu
        if (uiState.isHistoryDialogOpen) {
            WorkoutHistoryFullDialog(
                historyList = uiState.workoutHistory,
                onDismiss = viewModel::closeHistoryDialog,
                onSelectWorkout = viewModel::selectWorkoutForDetail,
                onDeleteWorkout = viewModel::requestDeleteWorkout
            )
        }

        // Antrenman Detayları Diyaloğu
        uiState.selectedWorkoutDetail?.let { detail ->
            WorkoutDetailDialog(
                detail = detail,
                onDismiss = viewModel::closeWorkoutDetail
            )
        }

        // Antrenman Silme Onay Diyaloğu
        uiState.workoutToDelete?.let { workout ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteWorkout,
                title = { Text(stringResource(R.string.workouts_delete_confirm_title)) },
                text = { Text(stringResource(R.string.workouts_delete_confirm_msg, workout.routineName)) },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDeleteWorkout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteWorkout) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun WorkoutVolumeChartCard(history: List<WorkoutHistoryUiModel>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Chronological order for chart (take up to the last 7 workouts)
    val chartData = remember(history) {
        history.sortedBy { it.timestamp }.takeLast(7)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.workout_volume_chart_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (chartData.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.workout_volume_last_n_workouts, chartData.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariantColor
                    )
                }
            }

            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.workout_volume_no_chart_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariantColor,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val maxVolume = remember(chartData) {
                    (chartData.maxOfOrNull { it.totalVolume } ?: 1.0).coerceAtLeast(100.0)
                }

                val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

                selectedIndex?.let { index ->
                    if (index in chartData.indices) {
                        val item = chartData[index]
                        val fullDateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.routineName} (${fullDateFormat.format(Date(item.timestamp))})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%.0f kg".format(item.totalVolume),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .pointerInput(chartData) {
                            detectTapGestures { offset ->
                                val barSpacing = size.width / chartData.size
                                val clickedIndex = (offset.x / barSpacing).toInt()
                                if (clickedIndex in chartData.indices) {
                                    selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                                }
                            }
                        }
                ) {
                    val count = chartData.size
                    val spacing = size.width / count
                    val barWidth = (spacing * 0.55f).coerceAtMost(42.dp.toPx())
                    val chartHeight = size.height - 30.dp.toPx()

                    val gridSteps = 3
                    for (i in 1..gridSteps) {
                        val y = chartHeight * (1f - (i.toFloat() / gridSteps))
                        drawLine(
                            color = onSurfaceVariantColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    chartData.forEachIndexed { i, item ->
                        val barHeight = ((item.totalVolume / maxVolume) * chartHeight).toFloat().coerceAtLeast(8.dp.toPx())
                        val centerX = spacing * i + spacing / 2f
                        val left = centerX - barWidth / 2f
                        val top = chartHeight - barHeight

                        val isBarSelected = selectedIndex == i

                        val brush = Brush.verticalGradient(
                            colors = if (isBarSelected) {
                                listOf(secondaryColor, primaryColor)
                            } else {
                                listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                            },
                            startY = top,
                            endY = chartHeight
                        )

                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        val valueLabel = if (item.totalVolume >= 1000) {
                            "%.1fT".format(item.totalVolume / 1000)
                        } else {
                            "%.0f".format(item.totalVolume)
                        }

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = primaryColor.toArgb()
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText(valueLabel, centerX, top - 6.dp.toPx(), paint)
                        }

                        val dateLabel = dateFormat.format(Date(item.timestamp))
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = onSurfaceVariantColor.toArgb()
                                textSize = 11.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(dateLabel, centerX, size.height - 6.dp.toPx(), paint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryFullDialog(
    historyList: List<WorkoutHistoryUiModel>,
    onDismiss: () -> Unit,
    onSelectWorkout: (WorkoutHistoryUiModel) -> Unit,
    onDeleteWorkout: (WorkoutHistoryUiModel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.workout_volume_history_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.workouts_no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    historyList.forEach { item ->
                        WorkoutHistoryItemCard(
                            item = item,
                            onClick = { onSelectWorkout(item) },
                            onDelete = { onDeleteWorkout(item) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

