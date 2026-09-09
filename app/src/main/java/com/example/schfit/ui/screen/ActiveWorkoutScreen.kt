package com.example.schfit.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.schfit.data.entity.SetLogEntity
import com.example.schfit.ui.viewmodel.NewPrEvent
import com.example.schfit.ui.viewmodel.WorkoutSessionUiState
import com.example.schfit.ui.viewmodel.WorkoutSessionViewModel
import androidx.compose.ui.res.stringResource
import com.example.schfit.R
import java.util.Locale

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    routineId: Long,
    viewModel: WorkoutSessionViewModel,
    onNavigateBack: () -> Unit,
    onFinishWorkoutNavigation: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Bildirim İzni İsteme (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(routineId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.initSession(routineId, context)
    }

    // Telefonun kendi geri tuşuna (fiziksel buton veya ekran kaydırma jesti) basıldığında
    BackHandler(enabled = !uiState.isWorkoutFinished) {
        showExitDialog = true
    }

    // Ekranı Açık Tut (Keep Screen On) Tercihi
    val activity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) {
        if (viewModel.isKeepScreenOnEnabled()) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Antrenman Bittiğinde Tam Ekran Özet Sayfası (MACFit Tasarımı)
    if (uiState.isWorkoutFinished) {
        WorkoutSummaryScreen(
            uiState = uiState,
            onShareStory = { userPhoto -> viewModel.shareWorkoutStory(context, userPhoto) },
            onSaveToGallery = { userPhoto -> viewModel.saveWorkoutStoryToGallery(context, userPhoto) },
            onFinishWorkoutNavigation = onFinishWorkoutNavigation
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.routineName.ifBlank { stringResource(R.string.active_workout_title) },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (uiState.exercises.isNotEmpty()) {
                            Text(
                                text = "${uiState.currentExerciseIndex + 1} / ${uiState.exercises.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showFinishConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.active_workout_finish_button), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.workouts_no_routines),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AnimatedContent(
                    targetState = uiState.isInRestMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WorkoutScreenContentTransition"
                ) { isInRestMode ->
                    if (isInRestMode) {
                        // DİNLENME EKRANI (Rest Mode View)
                        RestModeScreen(
                            uiState = uiState,
                            onAddSeconds = { viewModel.addRestSeconds(it, context) },
                            onSkipTimer = { viewModel.skipRestTimer() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        )
                    } else {
                        // AKTİF SET GİRİŞ EKRANI (Set Input View)
                        ActiveSetInputScreen(
                            uiState = uiState,
                            onWeightChange = viewModel::onWeightInputChange,
                            onRepsChange = viewModel::onRepsInputChange,
                            onAdjustWeight = viewModel::adjustWeight,
                            onAdjustReps = viewModel::adjustReps,
                            onCompleteSet = { viewModel.completeSet(context) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // 🏆 Tam Ekran Büyüyen & Patlayan PR Kutlama Animasyonu (TikTok Streak Tarzı)
            uiState.newPrEvent?.let { prEvent ->
                PrCelebrationOverlay(
                    event = prEvent,
                    onDismiss = viewModel::dismissPrCelebration
                )
            }
        }

        // Çıkış Diyaloğu (Arka Planda Tut veya İptal Et)
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(stringResource(R.string.active_workout_cancel_dialog_title)) },
                text = {
                    Text(stringResource(R.string.active_workout_cancel_dialog_msg))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showExitDialog = false
                                viewModel.cancelWorkout()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.active_workout_cancel_button))
                        }

                        TextButton(onClick = { showExitDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            )
        }

        // Onay & Tamamlama Diyalogları
        if (showFinishConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showFinishConfirmDialog = false },
                title = { Text(stringResource(R.string.active_workout_finish_dialog_title)) },
                text = { Text(stringResource(R.string.active_workout_finish_dialog_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showFinishConfirmDialog = false
                            viewModel.finishWorkout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

/**
 * 1. AKTİF SET GİRİŞ EKRANI
 * Kullanıcı ağırlık ve tekrar girer. "Seti Tamamla" butonuna bastığı anda dinlenme ekranına geçer.
 */
@Composable
fun ActiveSetInputScreen(
    uiState: WorkoutSessionUiState,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onCompleteSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentExercise = uiState.currentExercise

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Exercise Info Header & Set Indicator
        currentExercise?.let { ex ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = ex.muscleGroup,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Set ${uiState.currentSetNumber} / ${ex.targetSets}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Text(
                        text = ex.exerciseName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Referans Kartı (Önceki Antrenman Performansı)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = uiState.previousPerformance ?: stringResource(R.string.active_workout_first_time),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Hareket Notları Kartı (Varsa)
        if (!currentExercise?.notes.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = stringResource(R.string.note),
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${stringResource(R.string.note)}: ${currentExercise?.notes}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // Veri Giriş Alanları (Ağırlık ve Tekrar)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.active_workout_enter_set_data, uiState.currentSetNumber),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Ağırlık Girişi
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.active_workout_weight_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickAdjustButton(text = "-5", onClick = { onAdjustWeight(-5.0) })
                            QuickAdjustButton(text = "-2.5", onClick = { onAdjustWeight(-2.5) })
                            QuickAdjustButton(text = "+2.5", onClick = { onAdjustWeight(2.5) })
                            QuickAdjustButton(text = "+5", onClick = { onAdjustWeight(5.0) })
                        }
                    }

                    OutlinedTextField(
                        value = uiState.weightInput,
                        onValueChange = onWeightChange,
                        placeholder = { Text("80") },
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
                }

                // Tekrar Girişi
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.active_workout_reps_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickAdjustButton(text = "-1", onClick = { onAdjustReps(-1) })
                            QuickAdjustButton(text = "+1", onClick = { onAdjustReps(1) })
                        }
                    }

                    OutlinedTextField(
                        value = uiState.repsInput,
                        onValueChange = onRepsChange,
                        placeholder = { Text("10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Text(
                                text = stringResource(R.string.active_workout_reps_unit),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )
                }

                // Error text if any
                uiState.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Seti Tamamla Butonu
                Button(
                    onClick = onCompleteSet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.active_workout_complete_set),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Tamamlanan Setler Listesi
        if (uiState.completedSetsForCurrentExercise.isNotEmpty()) {
            CompletedSetsSummaryCard(sets = uiState.completedSetsForCurrentExercise)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * 2. DİNLENME VE MOLA EKRANI (Rest Mode View)
 */
@Composable
fun RestModeScreen(
    uiState: WorkoutSessionUiState,
    onAddSeconds: (Int) -> Unit,
    onSkipTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentExercise = uiState.currentExercise
    val lastSet = uiState.lastCompletedSet

    val progress = if (uiState.totalRestDuration > 0) {
        (uiState.restSecondsRemaining.toFloat() / uiState.totalRestDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "RestProgressAnimation")

    val minutes = uiState.restSecondsRemaining / 60
    val seconds = uiState.restSecondsRemaining % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Tamamlanan Set Bilgilendirme Rozeti
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1B5E20).copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (lastSet != null) stringResource(R.string.active_workout_set_completed_title, lastSet.setNumber) else stringResource(R.string.active_workout_set_completed_title, 1),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                        if (lastSet != null) {
                            Text(
                                text = "${lastSet.weightKg} kg  •  ${lastSet.reps} ${stringResource(R.string.active_workout_reps_unit)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                currentExercise?.let { ex ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = ex.exerciseName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Büyük Dinlenme Sayacı Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.active_workout_rest_timer),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Devasa Dijital Sayaç
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                )

                // Süre Ekleme ve Atla Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onAddSeconds(5) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("+5s")
                        }
                        OutlinedButton(
                            onClick = { onAddSeconds(15) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("+15s")
                        }
                        OutlinedButton(
                            onClick = { onAddSeconds(30) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("+30s")
                        }
                    }

                    Button(
                        onClick = onSkipTimer,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(R.string.active_workout_rest_timer_skip))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Sıradaki Set / Hareket Önizleme Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.active_workout_whats_next),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (uiState.isAllDone) {
                    Text(
                        text = stringResource(R.string.active_workout_all_sets_done),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else if (uiState.currentSetNumber == 1 && uiState.completedSetsForCurrentExercise.isEmpty()) {
                    val nextEx = uiState.currentExercise
                    Text(
                        text = "${nextEx?.exerciseName ?: ""} (${nextEx?.targetSets ?: 3} ${stringResource(R.string.sets)})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else {
                    currentExercise?.let { ex ->
                        Text(
                            text = "${ex.exerciseName} - Set ${uiState.currentSetNumber} / ${ex.targetSets}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Tamamlanan Setler Listesi
        if (uiState.completedSetsForCurrentExercise.isNotEmpty()) {
            CompletedSetsSummaryCard(sets = uiState.completedSetsForCurrentExercise)
        } else if (uiState.lastCompletedSet != null) {
            CompletedSetsSummaryCard(sets = listOf(uiState.lastCompletedSet))
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun QuickAdjustButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CompletedSetsSummaryCard(sets: List<SetLogEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.active_workout_completed_sets_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set ${set.setNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${set.weightKg} kg  x  ${set.reps} ${stringResource(R.string.active_workout_reps_unit)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val distance: Float,
    val color: Color,
    val size: Float,
    val rotation: Float
)

@Composable
fun PrCelebrationOverlay(
    event: NewPrEvent,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-160).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "prSlideDown"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "prAlpha"
    )

    // Minecraft XP Sparkle Animation
    val infiniteTransition = rememberInfiniteTransition(label = "xpSparkle")
    val sparkleOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleMovement"
    )

    // Auto-dismiss after 4.5 seconds
    LaunchedEffect(Unit) {
        isVisible = true
        kotlinx.coroutines.delay(4500)
        isVisible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * alpha))
            .clickable {
                isVisible = false
                onDismiss()
            }
            .zIndex(100f)
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 🎮 MINECRAFT / GAME ACHIEVEMENT TOAST BANNER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    isVisible = false
                    onDismiss()
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E232A) // Deep Obsidian / Dark Stone
            ),
            border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFFAA00)), // Gold pixel-accent border
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Taraf: Altın Kupa & XP Işıltı Çerçevesi
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2A1F05),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // XP Pırıltıları
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(34.dp)
                                .offset(y = sparkleOffset.dp * 0.15f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Sağ Taraf: Başarım Yazıları
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.active_workout_achievement_unlocked),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.1.sp
                            ),
                            color = Color(0xFFFFAA00) // Minecraft Gold
                        )
                    }

                    Text(
                        text = "${event.exerciseName}: ${event.weightKg} kg × ${event.reps} ${stringResource(R.string.active_workout_reps_unit)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    if (event.previousMax > 0.0) {
                        val increase = event.weightKg - event.previousMax
                        Text(
                            text = stringResource(R.string.active_workout_new_peak, increase, event.previousMax),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF55FF55) // Minecraft XP Green
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
