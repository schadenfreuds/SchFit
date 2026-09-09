package com.example.schfit.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.schfit.R
import com.example.schfit.ui.viewmodel.WorkoutSessionUiState
import com.example.schfit.util.EnhanceParameters
import com.example.schfit.util.GeminiLightingService
import com.example.schfit.util.ImageEnhancer
import com.example.schfit.util.LightingPreset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    uiState: WorkoutSessionUiState,
    onShareStory: (Bitmap?) -> Unit,
    onSaveToGallery: (Bitmap?) -> Unit,
    onFinishWorkoutNavigation: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalCroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawPhotoToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    var isAiEnhancing by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(LightingPreset.ORIGINAL) }
    var customGeminiParams by remember { mutableStateOf<EnhanceParameters?>(null) }
    var aiCoachComment by remember { mutableStateOf<String?>(null) }
    var isPressingOriginal by remember { mutableStateOf(false) }


    // Fotoğraf Seçici (Galeri) - EXIF Oryantasyonunu düzelterek kırpma penceresini açar
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val loadedBitmap = loadBitmapWithExif(context, uri)
                if (loadedBitmap != null) {
                    rawPhotoToCrop = loadedBitmap
                    showCropDialog = true
                } else {
                    Toast.makeText(context, "Fotoğraf açılamadı", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fotoğraf yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val workoutDate = if (uiState.workoutDateTimestamp != null && uiState.workoutDateTimestamp > 0) {
        Date(uiState.workoutDateTimestamp)
    } else {
        Date()
    }
    val dateFormat = SimpleDateFormat("d MMMM EEEE, HH:mm", Locale("tr"))
    val currentDateFormatted = dateFormat.format(workoutDate).uppercase(Locale("tr"))

    val currentDisplayBitmap = if (isPressingOriginal && originalCroppedBitmap != null) {
        originalCroppedBitmap
    } else {
        selectedPhotoBitmap
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.workout_summary_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onFinishWorkoutNavigation) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. MACFit Tarzı Büyütülmüş Fotoğraflı Ana Kart (Birebir Hikaye Kartı Oranı: 940:805)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // A) Spor Salonu / Antrenman Fotoğrafı (Oran: 940 / 805)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(940f / 805f)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .pointerInput(selectedPhotoBitmap, originalCroppedBitmap, selectedPreset) {
                                    if (selectedPhotoBitmap != null) {
                                        detectTapGestures(
                                            onPress = {
                                                if (originalCroppedBitmap != null && selectedPreset != LightingPreset.ORIGINAL) {
                                                    isPressingOriginal = true
                                                    tryAwaitRelease()
                                                    isPressingOriginal = false
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                            if (currentDisplayBitmap != null) {
                                Image(
                                    bitmap = currentDisplayBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.workout_summary_photo_desc),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Basılı Tut Karşılaştır Rozeti
                                if (selectedPhotoBitmap != null && selectedPreset != LightingPreset.ORIGINAL && !isPressingOriginal) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TouchApp,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.workout_summary_hold_original),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Fotoğrafı Kırp & Değiştir Rozeti
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.70f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .clickable {
                                            if (rawPhotoToCrop != null) {
                                                showCropDialog = true
                                            } else {
                                                photoPickerLauncher.launch("image/*")
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Crop,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.workout_summary_crop),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                // Varsayılan Şık Fitness Hero Arka Planı
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { photoPickerLauncher.launch("image/*") }
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF1E293B),
                                                    Color(0xFF0F172A),
                                                    Color(0xFF020617)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(68.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.FitnessCenter,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(34.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.workout_summary_tap_to_add_photo),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Fotoğrafın Sol Üstündeki Şeffaf SCHFIT Rozeti
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "⚡ SCHFIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }


                            // Üst Sağ Aktif Filtre / Orijinal Rozeti
                            if (currentDisplayBitmap != null) {

                                if (isPressingOriginal) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFDC2626).copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.workout_summary_original_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                } else if (selectedPreset != LightingPreset.ORIGINAL) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedPreset == LightingPreset.GEMINI_AI) Color(0xFF8B5CF6).copy(alpha = 0.9f) else Color(0xFF10B981).copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(selectedPreset.emoji, fontSize = 12.sp)
                                            Text(
                                                text = selectedPreset.displayName.uppercase(Locale.getDefault()),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }


                        // B) Kart İçi Bilgiler ve Kompakt İstatistikler
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Antrenman Başlığı & Tarih
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (uiState.routineName.isNotBlank()) uiState.routineName else stringResource(R.string.workout_summary_default_name),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = currentDateFormatted,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            // İnce Ayırıcı Çizgi
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            )

                            // 3'lü İstatistik Metrikleri (Hacim, Süre, Set)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetricColumn(
                                    label = stringResource(R.string.workout_summary_total_volume),
                                    value = if (uiState.totalVolumeLifted >= 1000) {
                                        "%.1f %s".format(uiState.totalVolumeLifted / 1000.0, stringResource(R.string.ton))
                                    } else {
                                        "%.0f kg".format(uiState.totalVolumeLifted)
                                    },
                                    valueColor = MaterialTheme.colorScheme.primary
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )

                                MetricColumn(
                                    label = stringResource(R.string.workout_summary_duration),
                                    value = "${uiState.workoutDurationMinutes} ${stringResource(R.string.min)}",
                                    valueColor = Color(0xFFF59E0B)
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )

                                MetricColumn(
                                    label = stringResource(R.string.workout_summary_sets),
                                    value = "${uiState.totalCompletedSetsCount}",
                                    valueColor = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }

                // 1.5. ✨ Gemini AI Işık ve Filtre Paneli (Fotoğraf seçildiyse gösterilir)
                if (selectedPhotoBitmap != null && originalCroppedBitmap != null) {
                    AiLightingControlCard(
                        isAiEnhancing = isAiEnhancing,
                        selectedPreset = selectedPreset,
                        aiCoachComment = aiCoachComment,
                        onTriggerGeminiAi = {
                            coroutineScope.launch {
                                isAiEnhancing = true
                                val result = GeminiLightingService.analyzeAndGetLightingParams(originalCroppedBitmap!!)
                                result.onSuccess { params ->
                                    customGeminiParams = params
                                    aiCoachComment = params.coachComment
                                    selectedPreset = LightingPreset.GEMINI_AI
                                    selectedPhotoBitmap = ImageEnhancer.applyParameters(originalCroppedBitmap!!, params)
                                    Toast.makeText(context, context.getString(R.string.ai_lighting_success_toast), Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    val fallback = ImageEnhancer.getLocalQuickParameters()
                                    customGeminiParams = fallback
                                    aiCoachComment = fallback.coachComment
                                    selectedPreset = LightingPreset.GEMINI_AI
                                    selectedPhotoBitmap = ImageEnhancer.applyParameters(originalCroppedBitmap!!, fallback)
                                    Toast.makeText(context, context.getString(R.string.ai_lighting_local_toast), Toast.LENGTH_SHORT).show()
                                }
                                isAiEnhancing = false
                            }
                        },
                        onSelectPreset = { preset ->
                            selectedPreset = preset
                            selectedPhotoBitmap = when (preset) {
                                LightingPreset.ORIGINAL -> originalCroppedBitmap
                                LightingPreset.GEMINI_AI -> {
                                    val p = customGeminiParams ?: preset.params
                                    ImageEnhancer.applyParameters(originalCroppedBitmap!!, p)
                                }
                                else -> ImageEnhancer.applyPreset(originalCroppedBitmap!!, preset)
                            }
                        }
                    )
                }



                // 2. Başarımlar / Kişisel Rekorlar (PR'lar) Kartı
                if (uiState.sessionPrs.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF422006).copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = stringResource(R.string.workout_summary_prs_title),
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "${stringResource(R.string.workout_summary_prs_title)} (${uiState.sessionPrs.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFBBF24)
                                )
                            }

                            uiState.sessionPrs.forEach { pr ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pr.exerciseName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "🔥 ${pr.weightKg} kg (${pr.reps} ${stringResource(R.string.reps)})",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFF59E0B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💪",
                                fontSize = 18.sp
                            )
                            Text(
                                text = stringResource(R.string.workout_summary_good_job),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Egzersiz Listesi Kartı
                if (uiState.exercises.isNotEmpty()) {
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
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.workout_summary_completed_exercises),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            uiState.exercises.forEachIndexed { index, ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = ex.exerciseName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${ex.targetSets} ${stringResource(R.string.sets)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 🌟 4. Alt Eylem Butonları (Fotoğraf Ekle, Paylaş, Galeriye Kaydet)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Fotoğraf Ekle / Değiştir Butonu
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.workout_summary_photo_btn),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.workout_summary_photo_btn),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }

                    // 2. Paylaş Butonu (Primary Accent)
                    Button(
                        onClick = { onShareStory(selectedPhotoBitmap) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981) // Canlı zümrüt yeşili
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.share),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    // 3. Cihaza Kaydet Butonu
                    OutlinedButton(
                        onClick = { onSaveToGallery(selectedPhotoBitmap) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = stringResource(R.string.workout_summary_save_btn),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.workout_summary_save_btn),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4. Ana Ekrana Dön Butonu
                Button(
                    onClick = onFinishWorkoutNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.workout_summary_return_home),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 🌟 İnteraktif Fotoğraf Kırpma & En-Boy Ayarlama Katmanı (Tam Ekran Overlay)
        if (showCropDialog && rawPhotoToCrop != null) {
            BackHandler { showCropDialog = false }

            ImageCropOverlay(
                sourceBitmap = rawPhotoToCrop!!,
                onCropComplete = { cropped ->
                    originalCroppedBitmap = cropped
                    selectedPhotoBitmap = cropped
                    selectedPreset = LightingPreset.ORIGINAL
                    customGeminiParams = null
                    aiCoachComment = null
                    showCropDialog = false

                    // Fotoğraf kırpıldıktan hemen sonra otomatik Gemini AI ışık analizini başlat
                    coroutineScope.launch {
                        isAiEnhancing = true
                        val result = GeminiLightingService.analyzeAndGetLightingParams(cropped)
                        result.onSuccess { params ->
                            customGeminiParams = params
                            aiCoachComment = params.coachComment
                            selectedPreset = LightingPreset.GEMINI_AI
                            selectedPhotoBitmap = ImageEnhancer.applyParameters(cropped, params)
                            Toast.makeText(context, context.getString(R.string.ai_lighting_success_toast), Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            val fallback = ImageEnhancer.getLocalQuickParameters()
                            customGeminiParams = fallback
                            aiCoachComment = fallback.coachComment
                            selectedPreset = LightingPreset.GEMINI_AI
                            selectedPhotoBitmap = ImageEnhancer.applyParameters(cropped, fallback)
                        }
                        isAiEnhancing = false
                    }
                },
                onDismiss = { showCropDialog = false }
            )
        }
    }
}




/**
 * Kullanıcının dikey/yatay fotoğrafını tam boyutta görerek (ContentScale.Fit) serbestçe kaydırıp, büyütüp, döndürerek kırpmasını sağlayan tam ekran katman
 */
@Composable
fun ImageCropOverlay(
    sourceBitmap: Bitmap,
    onCropComplete: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var currentBitmap by remember(sourceBitmap) { mutableStateOf(sourceBitmap) }

    var cropBoxWidthPx by remember { mutableFloatStateOf(0f) }
    var cropBoxHeightPx by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0B0F19)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Üst Bar: İptal, Başlık, Döndür & Sıfırla (Kompakt)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = stringResource(R.string.crop_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Sıfırla Butonu
                    IconButton(onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.reset),
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 90 Derece Döndür Butonu
                    IconButton(onClick = {
                        val rotated = rotateBitmap(currentBitmap, 90f)
                        currentBitmap = rotated
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = stringResource(R.string.crop_rotate),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Kırpma Önizleme Kutusu (Oran: 940:805, ideal ekran boyutu)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(940f / 805f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF020617))
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(18.dp))
                    .onGloballyPositioned { coordinates ->
                        cropBoxWidthPx = coordinates.size.width.toFloat()
                        cropBoxHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(currentBitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.6f, 4.5f)
                            val srcW = currentBitmap.width.toFloat()
                            val srcH = currentBitmap.height.toFloat()
                            val baseScale = if (cropBoxWidthPx > 0 && cropBoxHeightPx > 0) {
                                minOf(cropBoxWidthPx / srcW, cropBoxHeightPx / srcH)
                            } else 1f
                            val totalW = srcW * baseScale * scale
                            val totalH = srcH * baseScale * scale
                            val maxOffsetX = ((totalW - cropBoxWidthPx).coerceAtLeast(0f) / 2f) + (cropBoxWidthPx * 0.4f)
                            val maxOffsetY = ((totalH - cropBoxHeightPx).coerceAtLeast(0f) / 2f) + (cropBoxHeightPx * 0.4f)
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.crop_title),
                    contentScale = ContentScale.Fit, // Full boyutta görünür
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )

                // 3x3 Kılavuz Çizgileri
                GridOverlay()
            }

            // 3. Alt Kontroller & Onay Butonu (Samsung sistem tuşlarının çok üstünde, güvenli mesafede)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.2f).coerceAtLeast(0.6f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "-",
                            tint = Color.White
                        )
                    }

                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.6f..4f,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { scale = (scale + 0.2f).coerceAtMost(4f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "+",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "%.1fx".format(scale),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = stringResource(R.string.crop_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 4. Kırp ve Fotoğrafı Ekle Butonu
                Button(
                    onClick = {
                        val finalCropped = createCroppedBitmap(
                            source = currentBitmap,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            viewW = if (cropBoxWidthPx > 0) cropBoxWidthPx else 940f,
                            viewH = if (cropBoxHeightPx > 0) cropBoxHeightPx else 805f
                        )
                        onCropComplete(finalCropped)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.crop_confirm),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * 3x3 Kuralı Izgara Çizgileri
 */
@Composable
private fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = 1.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.20f)

        // Dikey Çizgiler
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = stroke)
        drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = stroke)

        // Yatay Çizgiler
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = stroke)
        drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = stroke)
    }
}

/**
 * EXIF Oryantasyonunu okuyup doğru dik açıya getiren ve bellek dostu yükleyen fonksiyon
 */
fun loadBitmapWithExif(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
    return try {
        var orientation = ExifInterface.ORIENTATION_NORMAL
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        var sampleSize = 1
        val maxDim = maxOf(options.outWidth, options.outHeight)
        while (maxDim / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap: Bitmap? = null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
        }

        if (bitmap == null) return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
    } catch (e: Exception) {
        null
    }
}

/**
 * 90 derece döndürme yardımcısı
 */
fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

/**
 * Kullanıcının ekranda belirlediği kaydırma ve büyütme değerlerine göre yüksek çözünürlüklü kırpılmış bitmap üretir
 */
fun createCroppedBitmap(
    source: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    viewW: Float,
    viewH: Float,
    outputW: Int = 1175,
    outputH: Int = 1006
): Bitmap {
    val actualOutputW = if (outputW > 0) outputW else 1175
    val actualOutputH = if (viewW > 0 && viewH > 0) {
        (actualOutputW * (viewH / viewW)).roundToInt()
    } else {
        1006
    }

    val result = Bitmap.createBitmap(actualOutputW, actualOutputH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // Fotoğraf küçültüldüğünde veya boşluk kaldığında şık koyu arka plan
    canvas.drawColor(android.graphics.Color.parseColor("#0B0F19"))

    val srcW = source.width.toFloat()
    val srcH = source.height.toFloat()
    val vW = if (viewW > 0) viewW else actualOutputW.toFloat()
    val vH = if (viewH > 0) viewH else actualOutputH.toFloat()

    // ContentScale.Fit temel ölçeği
    val baseScale = minOf(vW / srcW, vH / srcH)
    val factor = actualOutputW.toFloat() / vW

    val drawScale = baseScale * scale * factor
    val totalScaledW = srcW * drawScale
    val totalScaledH = srcH * drawScale

    val drawLeft = (actualOutputW / 2f) + (offsetX * factor) - (totalScaledW / 2f)
    val drawTop = (actualOutputH / 2f) + (offsetY * factor) - (totalScaledH / 2f)

    val matrix = Matrix().apply {
        postScale(drawScale, drawScale)
        postTranslate(drawLeft, drawTop)
    }

    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    canvas.drawBitmap(source, matrix, paint)

    return result
}

@Composable
fun MetricColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            ),
            color = valueColor
        )
    }
}

/**
 * ✨ Gemini AI ve Akıllı Işık İyileştirme Kontrol Paneli
 */
@Composable
fun AiLightingControlCard(
    isAiEnhancing: Boolean,
    selectedPreset: LightingPreset,
    aiCoachComment: String?,
    onTriggerGeminiAi: () -> Unit,
    onSelectPreset: (LightingPreset) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.5.dp,
            if (selectedPreset == LightingPreset.GEMINI_AI) Color(0xFF8B5CF6).copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.ai_lighting_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isAiEnhancing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.2.dp,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }

            // Gemini AI İyileştirme Butonu
            Button(
                onClick = onTriggerGeminiAi,
                enabled = !isAiEnhancing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPreset == LightingPreset.GEMINI_AI) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAiEnhancing) {
                    Text(
                        text = stringResource(R.string.ai_lighting_analyzing),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_lighting_auto_enhance),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            // Filtre Çipleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LightingPreset.entries.forEach { preset ->
                    val isSelected = (selectedPreset == preset)
                    val presetName = when (preset) {
                        LightingPreset.ORIGINAL -> stringResource(R.string.preset_original)
                        LightingPreset.GEMINI_AI -> stringResource(R.string.preset_gemini_ai)
                        LightingPreset.GYM_PUMP -> stringResource(R.string.preset_gym_pump)
                        LightingPreset.MOODY_DARK -> stringResource(R.string.preset_moody_dark)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            if (preset == LightingPreset.GEMINI_AI) Color(0xFF8B5CF6).copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                if (preset == LightingPreset.GEMINI_AI) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectPreset(preset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(preset.emoji, fontSize = 13.sp)
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    if (preset == LightingPreset.GEMINI_AI) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // AI Coach Comment
            if (!aiCoachComment.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🤖", fontSize = 18.sp)
                        Text(
                            text = aiCoachComment,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}



