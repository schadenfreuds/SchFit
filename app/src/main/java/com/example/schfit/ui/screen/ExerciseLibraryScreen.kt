package com.example.schfit.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schfit.R
import com.example.schfit.data.entity.ExerciseEntity
import com.example.schfit.ui.viewmodel.ExerciseDetailProgressData
import com.example.schfit.ui.viewmodel.ExerciseLibraryUiState
import com.example.schfit.ui.viewmodel.ExerciseLibraryViewModel
import com.example.schfit.ui.viewmodel.ExerciseWorkoutPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    viewModel: ExerciseLibraryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.exercise_lib_title),
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.exercise_lib_new_exercise_btn),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Arama Kutusu
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.exercise_lib_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp)
            )

            // Bölge Filtre Çipleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseLibraryViewModel.MUSCLE_GROUPS.forEach { group ->
                    val isSelected = uiState.selectedMuscleGroup == group
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onMuscleGroupSelect(group) },
                        label = {
                            Text(
                                text = group,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Hareket Listesi
            if (uiState.exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.exercise_lib_no_match),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.openAddDialog() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.exercise_lib_add_with_name_btn))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.selectedMuscleGroup} ${stringResource(R.string.exercise_lib_exercises_suffix)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${uiState.exercises.size} ${stringResource(R.string.exercise_lib_exercise_count_suffix)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    items(uiState.exercises, key = { it.id }) { exercise ->
                        ExerciseItemCard(
                            exercise = exercise,
                            onCardClick = { viewModel.openExerciseProgress(exercise) },
                            onEditClick = { viewModel.openEditDialog(exercise) },
                            onDeleteClick = { viewModel.requestDeleteExercise(exercise) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Hareket Gelişim & Grafik Detay Diyaloğu
        if (uiState.isProgressDialogOpen && uiState.selectedExerciseProgress != null) {
            ExerciseProgressDetailDialog(
                data = uiState.selectedExerciseProgress!!,
                onDismiss = viewModel::closeProgressDialog
            )
        }

        // Hareket Ekleme / Düzenleme Diyaloğu
        if (uiState.isAddEditDialogOpen) {
            AddEditExerciseDialog(
                uiState = uiState,
                onNameChange = viewModel::onDraftNameChange,
                onGroupChange = viewModel::onDraftMuscleGroupChange,
                onNotesChange = viewModel::onDraftNotesChange,
                onDismiss = viewModel::closeAddEditDialog,
                onSave = { viewModel.saveExercise() }
            )
        }

        // Silme Onay Diyaloğu
        uiState.exerciseToDelete?.let { exercise ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteExercise,
                title = { Text(stringResource(R.string.exercise_lib_delete_title)) },
                text = { Text(stringResource(R.string.exercise_lib_delete_msg, exercise.name)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteExercise() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteExercise) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun ExerciseItemCard(
    exercise: ExerciseEntity,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (exercise.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.exercise_lib_custom_badge),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getMuscleGroupColor(exercise.muscleGroup).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = exercise.muscleGroup,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = getMuscleGroupColor(exercise.muscleGroup),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.exercise_lib_progress_pr_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Not varsa göster
            if (exercise.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = stringResource(R.string.note),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = exercise.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseProgressDetailDialog(
    data: ExerciseDetailProgressData,
    onDismiss: () -> Unit
) {
    val points = data.weightHistoryPoints
    var selectedPointIndex by remember(points) { mutableStateOf<Int?>(if (points.isNotEmpty()) points.lastIndex else null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getMuscleGroupColor(data.exercise.muscleGroup).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = data.exercise.muscleGroup,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = getMuscleGroupColor(data.exercise.muscleGroup),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Not varsa göster
                if (data.exercise.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = data.exercise.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // PR ve 1RM İstatistik Kartları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // PR Kartı
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE65100).copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.exercise_lib_pr_highest),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = if (data.personalRecordWeight > 0) "%.1f kg".format(data.personalRecordWeight) else "-",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFE65100)
                            )
                            if (data.personalRecordReps > 0) {
                                Text(
                                    text = "${data.personalRecordReps} ${stringResource(R.string.reps)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 1RM Kartı
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.exercise_lib_estimated_1rm),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (data.estimatedOneRepMax > 0) "%.1f kg".format(data.estimatedOneRepMax) else "-",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (data.oneRepMaxSourceSet != null) {
                                    "${data.oneRepMaxSourceSet.weightKg}kg × ${data.oneRepMaxSourceSet.reps} %s".format(stringResource(R.string.reps))
                                } else stringResource(R.string.exercise_lib_epley_formula),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Ağırlık Gelişim Grafiği Bölümü
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.exercise_lib_max_weight_chart),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${points.size} ${stringResource(R.string.exercise_lib_workout_count_suffix)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (points.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.exercise_lib_no_chart_data),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Seçilen nokta detayı
                            selectedPointIndex?.let { idx ->
                                if (idx in points.indices) {
                                    val pt = points[idx]
                                    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sdf.format(Date(pt.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${pt.maxWeightKg} kg (${pt.bestRepsAtMaxWeight} %s)".format(stringResource(R.string.reps)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Canvas Weight Line Chart
                            ExerciseWeightLineChart(
                                points = points,
                                selectedIndex = selectedPointIndex,
                                onSelectPoint = { selectedPointIndex = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }

                // Geçmiş Antrenman ve Set Detayları
                if (points.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.exercise_lib_history_and_sets),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val sdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

                    points.reversed().forEach { point ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sdf.format(Date(point.timestamp)),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${stringResource(R.string.exercise_lib_highest_label)}: ${point.maxWeightKg} kg",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    point.allSets.forEach { set ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (set.weightKg == point.maxWeightKg) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ) {
                                            Text(
                                                text = "S${set.setNumber}: ${set.weightKg}kg × ${set.reps}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (set.weightKg == point.maxWeightKg) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (set.weightKg == point.maxWeightKg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
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

@Composable
fun ExerciseWeightLineChart(
    points: List<ExerciseWorkoutPoint>,
    selectedIndex: Int?,
    onSelectPoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = Color(0xFFE65100)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val valueLabelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
    )
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    val minW = remember(points) { (points.minOfOrNull { it.maxWeightKg } ?: 0.0) - 2.0 }.coerceAtLeast(0.0)
    val maxW = remember(points) { (points.maxOfOrNull { it.maxWeightKg } ?: 10.0) + 3.0 }
    val wRange = (maxW - minW).coerceAtLeast(1.0)

    val prWeight = remember(points) { points.maxOfOrNull { it.maxWeightKg } ?: 0.0 }
    val prIndex = remember(points) { points.indexOfLast { it.maxWeightKg == prWeight } }

    val milestoneDateIndices = remember(points) {
        when {
            points.size <= 4 -> points.indices.toSet()
            points.size in 5..8 -> setOf(0, points.size / 2, points.lastIndex)
            else -> setOf(0, points.size / 3, (2 * points.size) / 3, points.lastIndex)
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        val spacing = if (points.size > 1) size.width / (points.size - 1) else size.width
                        val clicked = ((offset.x + spacing / 2f) / spacing).toInt().coerceIn(0, points.lastIndex)
                        onSelectPoint(clicked)
                    },
                    onTap = { offset ->
                        val spacing = if (points.size > 1) size.width / (points.size - 1) else size.width
                        val clicked = ((offset.x + spacing / 2f) / spacing).toInt().coerceIn(0, points.lastIndex)
                        onSelectPoint(clicked)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val bottomPadding = 32.dp.toPx()
        val topPadding = 28.dp.toPx()
        val availableHeight = height - bottomPadding - topPadding

        // Yatay kılavuz çizgileri
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = topPadding + availableHeight * (i.toFloat() / gridLines)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        if (points.size == 1) {
            val point = points[0]
            val x = width / 2f
            val y = topPadding + availableHeight / 2f

            drawCircle(color = primaryColor, radius = 7.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))

            val valText = "${point.maxWeightKg} kg"
            val textLayout = textMeasurer.measure(AnnotatedString(valText), TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor))
            drawText(textLayoutResult = textLayout, topLeft = Offset(x - textLayout.size.width / 2f, y - 22.dp.toPx()))

            val dateText = dateFormat.format(Date(point.timestamp))
            val dateLayout = textMeasurer.measure(AnnotatedString(dateText), labelStyle)
            drawText(textLayoutResult = dateLayout, topLeft = Offset(x - dateLayout.size.width / 2f, height - bottomPadding + 8.dp.toPx()))
            return@Canvas
        }

        val stepX = width / (points.size - 1)
        val pixelPoints = points.mapIndexed { index, item ->
            val x = index * stepX
            val ratio = (item.maxWeightKg - minW) / wRange
            val y = topPadding + availableHeight * (1f - ratio.toFloat())
            Offset(x, y)
        }

        // Fill path with gradient
        val fillPath = Path().apply {
            moveTo(pixelPoints.first().x, height - bottomPadding)
            lineTo(pixelPoints.first().x, pixelPoints.first().y)
            for (i in 1 until pixelPoints.size) {
                val prev = pixelPoints[i - 1]
                val curr = pixelPoints[i]
                val cpx1 = (prev.x + curr.x) / 2f
                val cpy1 = prev.y
                val cpx2 = (prev.x + curr.x) / 2f
                val cpy2 = curr.y
                cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
            }
            lineTo(pixelPoints.last().x, height - bottomPadding)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.30f), primaryColor.copy(alpha = 0.01f)),
                startY = topPadding,
                endY = height - bottomPadding
            )
        )

        // Line path
        val linePath = Path().apply {
            moveTo(pixelPoints.first().x, pixelPoints.first().y)
            for (i in 1 until pixelPoints.size) {
                val prev = pixelPoints[i - 1]
                val curr = pixelPoints[i]
                val cpx1 = (prev.x + curr.x) / 2f
                val cpy1 = prev.y
                val cpx2 = (prev.x + curr.x) / 2f
                val cpy2 = curr.y
                cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
            }
        }

        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Seçili noktanın dikey kılavuz çizgisi
        selectedIndex?.let { selIdx ->
            if (selIdx in pixelPoints.indices) {
                val selPt = pixelPoints[selIdx]
                drawLine(
                    color = accentColor.copy(alpha = 0.6f),
                    start = Offset(selPt.x, topPadding),
                    end = Offset(selPt.x, height - bottomPadding),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }
        }

        // Noktalar
        pixelPoints.forEachIndexed { index, pt ->
            val isSelected = selectedIndex == index
            val isPr = index == prIndex

            val pointColor = when {
                isSelected -> accentColor
                isPr -> Color(0xFFFB8C00)
                else -> primaryColor
            }

            drawCircle(
                color = pointColor,
                radius = if (isSelected) 6.5.dp.toPx() else 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                center = pt
            )
        }

        // Akıllı Tarih Etiketleri
        milestoneDateIndices.forEach { idx ->
            if (idx in pixelPoints.indices) {
                val pt = pixelPoints[idx]
                val dateText = dateFormat.format(Date(points[idx].timestamp))
                val dateLayout = textMeasurer.measure(AnnotatedString(dateText), labelStyle)

                val dateX = when (idx) {
                    0 -> 0f
                    points.lastIndex -> width - dateLayout.size.width
                    else -> (pt.x - dateLayout.size.width / 2f).coerceIn(0f, width - dateLayout.size.width)
                }

                drawText(textLayoutResult = dateLayout, topLeft = Offset(dateX, height - bottomPadding + 8.dp.toPx()))
            }
        }

        // Seçili Nokta Ağırlık Rozeti (Pill Badge)
        val activeBadgeIndex = selectedIndex ?: prIndex
        if (activeBadgeIndex in pixelPoints.indices) {
            val activePt = pixelPoints[activeBadgeIndex]
            val weightText = "${points[activeBadgeIndex].maxWeightKg} kg"
            val textLayout = textMeasurer.measure(AnnotatedString(weightText), valueLabelStyle)

            val badgePaddingH = 8.dp.toPx()
            val badgePaddingV = 4.dp.toPx()
            val badgeWidth = textLayout.size.width + badgePaddingH * 2
            val badgeHeight = textLayout.size.height + badgePaddingV * 2

            val badgeX = (activePt.x - badgeWidth / 2f).coerceIn(4.dp.toPx(), width - badgeWidth - 4.dp.toPx())
            val badgeY = (activePt.y - badgeHeight - 8.dp.toPx()).coerceAtLeast(2.dp.toPx())

            // Rozet arka planı
            drawRoundRect(
                color = if (activeBadgeIndex == selectedIndex) accentColor else primaryColor,
                topLeft = Offset(badgeX, badgeY),
                size = androidx.compose.ui.geometry.Size(badgeWidth, badgeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Rozet metni
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(badgeX + badgePaddingH, badgeY + badgePaddingV)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExerciseDialog(
    uiState: ExerciseLibraryUiState,
    onNameChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (uiState.isEditing) stringResource(R.string.exercise_lib_edit_dialog_title) else stringResource(R.string.exercise_lib_add_dialog_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = uiState.draftName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.exercise_lib_exercise_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = stringResource(R.string.exercise_lib_select_muscle_group),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExerciseLibraryViewModel.SELECTABLE_MUSCLE_GROUPS.forEach { group ->
                        val isSelected = uiState.draftMuscleGroup == group
                        FilterChip(
                            selected = isSelected,
                            onClick = { onGroupChange(group) },
                            label = { Text(group) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.draftNotes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.exercise_lib_notes_label)) },
                    placeholder = { Text(stringResource(R.string.exercise_lib_notes_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                uiState.errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (uiState.isSaving) stringResource(R.string.saving)
                    else if (uiState.isEditing) stringResource(R.string.update)
                    else stringResource(R.string.save)
                )
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

fun getMuscleGroupColor(muscleGroup: String): Color {
    return when (muscleGroup) {
        "Göğüs" -> Color(0xFF1E88E5) // Blue
        "Sırt" -> Color(0xFF00897B) // Teal
        "Bacak" -> Color(0xFF43A047) // Green
        "Omuz" -> Color(0xFFFB8C00) // Orange
        "Biceps" -> Color(0xFF8E24AA) // Purple
        "Triceps" -> Color(0xFFD81B60) // Pink
        "Karın" -> Color(0xFFE53935) // Red
        else -> Color(0xFF5E35B1) // Deep Purple
    }
}
