package com.example.schfit.ui.screen

import android.graphics.Paint
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schfit.R
import com.example.schfit.data.entity.BodyMetricEntity
import com.example.schfit.ui.viewmodel.ProgressUiState
import com.example.schfit.ui.viewmodel.ProgressViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackingScreen(
    viewModel: ProgressViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.weight_tracking_title),
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
                actions = {
                    IconButton(onClick = viewModel::openTargetWeightDialog) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = stringResource(R.string.weight_tracking_set_target_btn),
                            tint = if (uiState.targetWeight != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

            // 1. Kilo Gelişim Grafiği & Hedefe Kalan (En başta)
            WeightProgressChartCard(
                metricsList = uiState.metricsList,
                targetWeight = uiState.targetWeight,
                onOpenTargetWeightDialog = viewModel::openTargetWeightDialog
            )

            // 2. Kilo Kaydı Ekleme Kutusu (Grafiğin altında)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.weight_tracking_new_entry_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.weightInput,
                            onValueChange = viewModel::onWeightInputChange,
                            placeholder = { Text(stringResource(R.string.weight_tracking_entry_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                Text(
                                    text = stringResource(R.string.kg),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        )

                        Button(
                            onClick = { viewModel.addWeightMetric() },
                            enabled = !uiState.isSavingWeight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.save))
                        }
                    }

                    uiState.errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 3. "Kilo Ölçüm Geçmişi" Butonu (Ekleme kutusunun altında)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { viewModel.openWeightHistoryDialog() },
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
                            color = Color(0xFFE65100),
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
                                text = stringResource(R.string.weight_tracking_history_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.weight_tracking_history_desc, uiState.metricsList.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = stringResource(R.string.weight_tracking_history_title),
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Hedef Kilo Belirleme Diyaloğu
        if (uiState.isTargetWeightDialogOpen) {
            SetTargetWeightDialog(
                input = uiState.targetWeightInput,
                onInputChange = viewModel::onTargetWeightInputChange,
                onDismiss = viewModel::closeTargetWeightDialog,
                onSave = viewModel::saveTargetWeight,
                onClear = viewModel::clearTargetWeight,
                hasExistingTarget = (uiState.targetWeight != null),
                errorMessage = uiState.errorMessage
            )
        }

        // Kilo Ölçüm Geçmişi Tam Liste Diyaloğu
        if (uiState.isWeightHistoryDialogOpen) {
            WeightHistoryFullDialog(
                metricsList = uiState.metricsList,
                onDismiss = viewModel::closeWeightHistoryDialog,
                onDelete = viewModel::requestDeleteWeight
            )
        }

        // Kilo Silme Onay Diyaloğu
        uiState.weightToDelete?.let { metric ->
            val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteWeight,
                title = { Text(stringResource(R.string.weight_tracking_delete_title)) },
                text = { Text(stringResource(R.string.weight_tracking_delete_msg, "%.1f".format(metric.weightKg), dateFormat.format(Date(metric.timestamp)))) },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDeleteWeight,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteWeight) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun WeightProgressChartCard(
    metricsList: List<BodyMetricEntity>,
    targetWeight: Double?,
    onOpenTargetWeightDialog: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Chronological order for chart (take up to the last 8 recordings)
    val chartData = remember(metricsList) {
        metricsList.sortedBy { it.timestamp }.takeLast(8)
    }

    val initialWeight = remember(metricsList) { metricsList.minByOrNull { it.timestamp }?.weightKg }
    val currentWeight = remember(metricsList) { metricsList.maxByOrNull { it.timestamp }?.weightKg }
    val weightDiff = if (initialWeight != null && currentWeight != null) currentWeight - initialWeight else null
    val remainingToGoal = if (currentWeight != null && targetWeight != null) targetWeight - currentWeight else null

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.weight_tracking_chart_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (targetWeight != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenTargetWeightDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (targetWeight != null) MaterialTheme.colorScheme.primary else onSurfaceVariantColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (targetWeight != null) stringResource(R.string.weight_tracking_target_label, "%.1f".format(targetWeight)) else stringResource(R.string.weight_tracking_set_target_btn),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (targetWeight != null) MaterialTheme.colorScheme.primary else onSurfaceVariantColor
                        )
                    }
                }
            }

            // Summary metrics row (Başlangıç, Güncel, Değişim, Hedefe Kalan)
            if (currentWeight != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.weight_tracking_initial_label), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(initialWeight?.let { "%.1f kg".format(it) } ?: "- kg", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.weight_tracking_current_label), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("%.1f kg".format(currentWeight), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = primaryColor)
                    }

                    val (diffText, diffIcon, diffColor) = when {
                        weightDiff == null -> Triple("- kg", Icons.Default.TrendingFlat, onSurfaceVariantColor)
                        weightDiff > 0 -> Triple("+%.1f kg".format(weightDiff), Icons.Default.TrendingUp, Color(0xFFE65100))
                        weightDiff < 0 -> Triple("%.1f kg".format(weightDiff), Icons.Default.TrendingDown, Color(0xFF2E7D32))
                        else -> Triple("0.0 kg", Icons.Default.TrendingFlat, primaryColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.weight_tracking_change_label), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(diffIcon, contentDescription = null, tint = diffColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(diffText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = diffColor)
                        }
                    }

                    // Hedefe Kalan Sütunu
                    val (remText, remColor) = when {
                        targetWeight == null -> Pair("- kg", onSurfaceVariantColor)
                        remainingToGoal == null -> Pair("- kg", onSurfaceVariantColor)
                        remainingToGoal > 0 -> Pair("+%.1f kg".format(remainingToGoal), Color(0xFF3B82F6))
                        remainingToGoal < 0 -> Pair("%.1f kg".format(remainingToGoal), Color(0xFF10B981))
                        else -> Pair("0.0 kg", Color(0xFF10B981))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.weight_tracking_remaining_goal_label), style = MaterialTheme.typography.bodySmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = remText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = remColor
                        )
                    }
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
                        text = stringResource(R.string.weight_tracking_no_chart_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariantColor,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val allWeights = remember(chartData, targetWeight) {
                    val list = chartData.map { it.weightKg }
                    if (targetWeight != null) list + targetWeight else list
                }
                val minWeight = remember(allWeights) { (allWeights.minOrNull() ?: 50.0) - 1.5 }
                val maxWeight = remember(allWeights) { (allWeights.maxOrNull() ?: 80.0) + 1.5 }
                val weightSpan = (maxWeight - minWeight).coerceAtLeast(1.0)

                val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

                // Selected point details banner
                selectedIndex?.let { index ->
                    if (index in chartData.indices) {
                        val item = chartData[index]
                        val fullDateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
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
                                    text = fullDateFormat.format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%.1f kg".format(item.weightKg),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }

                val targetStr = stringResource(R.string.weight_tracking_target_label, if (targetWeight != null) "%.1f".format(targetWeight) else "")

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .pointerInput(chartData) {
                            detectTapGestures { offset ->
                                val spacing = if (chartData.size > 1) size.width / (chartData.size - 1) else size.width
                                val clickedIndex = ((offset.x + spacing / 2f) / spacing).toInt()
                                if (clickedIndex in chartData.indices) {
                                    selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                                }
                            }
                        }
                ) {
                    val count = chartData.size
                    val topPadding = 24.dp.toPx()
                    val bottomPadding = 30.dp.toPx()
                    val chartHeight = size.height - topPadding - bottomPadding
                    val sidePadding = 24.dp.toPx()
                    val availableWidth = size.width - (sidePadding * 2)
                    val spacing = if (count > 1) availableWidth / (count - 1) else 0f

                    // Grid lines
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = topPadding + chartHeight * (i.toFloat() / gridSteps)
                        drawLine(
                            color = onSurfaceVariantColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    // Draw Target Weight Reference Line if set
                    if (targetWeight != null) {
                        val targetNormalized = ((targetWeight - minWeight) / weightSpan).toFloat()
                        val targetY = topPadding + chartHeight * (1f - targetNormalized)
                        drawLine(
                            color = Color(0xFF10B981).copy(alpha = 0.75f),
                            start = Offset(0f, targetY),
                            end = Offset(size.width, targetY),
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )

                        val targetPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#10B981")
                            textSize = 10.sp.toPx()
                            isFakeBoldText = true
                            textAlign = Paint.Align.RIGHT
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "🎯 $targetStr",
                            size.width - 10.dp.toPx(),
                            targetY - 6.dp.toPx(),
                            targetPaint
                        )
                    }

                    // Compute points
                    val points = chartData.mapIndexed { i, item ->
                        val x = if (count > 1) sidePadding + i * spacing else size.width / 2f
                        val normalized = ((item.weightKg - minWeight) / weightSpan).toFloat()
                        val y = topPadding + chartHeight * (1f - normalized)
                        Offset(x, y)
                    }

                    // Draw line & gradient fill
                    if (points.size > 1) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        val fillPath = Path().apply {
                            moveTo(points.first().x, topPadding + chartHeight)
                            lineTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, topPadding + chartHeight)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.0f)),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw Data Points & Labels
                    points.forEachIndexed { i, pt ->
                        val item = chartData[i]
                        val isSelected = (selectedIndex == i)

                        // Outer ring
                        drawCircle(
                            color = if (isSelected) primaryColor else Color.White,
                            radius = if (isSelected) 7.dp.toPx() else 4.5.dp.toPx(),
                            center = pt
                        )

                        // Inner dot
                        drawCircle(
                            color = if (isSelected) Color.White else primaryColor,
                            radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )

                        // Value text above point
                        val valuePaint = Paint().apply {
                            color = primaryColor.toArgb()
                            textSize = 11.sp.toPx()
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.1f".format(item.weightKg),
                            pt.x,
                            pt.y - 10.dp.toPx(),
                            valuePaint
                        )

                        // Date label below chart
                        val datePaint = Paint().apply {
                            color = onSurfaceVariantColor.toArgb()
                            textSize = 10.sp.toPx()
                            textAlign = Paint.Align.CENTER
                        }
                        val formattedDate = dateFormat.format(Date(item.timestamp))
                        drawContext.canvas.nativeCanvas.drawText(
                            formattedDate,
                            pt.x,
                            size.height - 6.dp.toPx(),
                            datePaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetTargetWeightDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    hasExistingTarget: Boolean,
    errorMessage: String?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = stringResource(R.string.weight_tracking_set_target_dialog_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.weight_tracking_set_target_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.weight_tracking_target_weight_input_label)) },
                    placeholder = { Text(stringResource(R.string.weight_tracking_entry_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = stringResource(R.string.kg),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (hasExistingTarget) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.weight_tracking_remove_target_btn))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun WeightHistoryFullDialog(
    metricsList: List<BodyMetricEntity>,
    onDismiss: () -> Unit,
    onDelete: (BodyMetricEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
    val sortedList = remember(metricsList) { metricsList.sortedByDescending { it.timestamp } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(R.string.weight_tracking_history_title)} (${metricsList.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            if (sortedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.weight_tracking_no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedList.forEach { metric ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "%.1f kg".format(metric.weightKg),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(metric.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onDelete(metric) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
