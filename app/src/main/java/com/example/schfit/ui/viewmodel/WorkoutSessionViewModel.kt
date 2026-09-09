package com.example.schfit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.entity.RoutineExerciseWithDetails
import com.example.schfit.data.entity.SetLogEntity
import com.example.schfit.data.entity.WorkoutLogEntity
import com.example.schfit.data.repository.ActiveWorkoutSessionData
import com.example.schfit.data.repository.SchFitRepository
import com.example.schfit.util.NotificationHelper
import com.example.schfit.util.ShareCardGenerator
import com.example.schfit.util.StoryPrItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class NewPrEvent(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val previousMax: Double
)

data class WorkoutSessionUiState(
    val isLoading: Boolean = true,
    val routineId: Long = 0,
    val routineName: String = "",
    val workoutLogId: Long = 0,
    val exercises: List<RoutineExerciseWithDetails> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val previousPerformance: String? = null,
    val weightInput: String = "",
    val repsInput: String = "",
    val isRestTimerActive: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val totalRestDuration: Int = 90,
    val isNextSetReady: Boolean = false,
    val lastCompletedSet: SetLogEntity? = null,
    val completedSetsForCurrentExercise: List<SetLogEntity> = emptyList(),
    val totalCompletedSetsCount: Int = 0,
    val isWorkoutFinished: Boolean = false,
    val isAllWorkoutSetsCompleted: Boolean = false,
    val workoutDurationMinutes: Int = 0,
    val totalVolumeLifted: Double = 0.0,
    val sessionPrs: List<StoryPrItem> = emptyList(),
    val newPrEvent: NewPrEvent? = null,
    val workoutDateTimestamp: Long? = null,
    val error: String? = null
) {
    val currentExercise: RoutineExerciseWithDetails?
        get() = exercises.getOrNull(currentExerciseIndex)

    val nextExercise: RoutineExerciseWithDetails?
        get() = exercises.getOrNull(currentExerciseIndex + 1)

    val targetSets: Int
        get() = currentExercise?.targetSets ?: 3

    val isLastSetOfExercise: Boolean
        get() = currentSetNumber >= targetSets

    val isLastExercise: Boolean
        get() = currentExerciseIndex >= exercises.size - 1

    val isAllDone: Boolean
        get() = isAllWorkoutSetsCompleted || (isLastExercise && isLastSetOfExercise)

    val isInRestMode: Boolean
        get() = isRestTimerActive
}

class WorkoutSessionViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = System.currentTimeMillis()
    private var currentRestEndTimeMillis: Long? = null

    fun isKeepScreenOnEnabled(): Boolean = repository.isKeepScreenOnEnabled()

    private fun parseWeight(input: String): Double? {
        val sanitized = input.trim().replace(',', '.')
        return sanitized.toDoubleOrNull()
    }

    private fun formatWeight(weight: Double): String {
        return if (weight % 1.0 == 0.0) {
            weight.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", weight)
        }
    }

    private fun formatPreviousSets(sets: List<SetLogEntity>): String {
        if (sets.isEmpty()) return "İlk kez yapılıyor"
        return sets.joinToString("  ") { s ->
            "${formatWeight(s.weightKg)}x${s.reps}"
        }
    }

    fun initSession(routineId: Long, context: Context? = null) {
        val shouldShowLoading = _uiState.value.routineId != routineId || _uiState.value.exercises.isEmpty()
        if (shouldShowLoading) {
            _uiState.update { it.copy(isLoading = true, routineId = routineId) }
        }

        viewModelScope.launch {

            val routine = repository.getRoutineById(routineId).firstOrNull()
            val exercises = repository.getRoutineExercisesList(routineId)
            val activeSession = repository.getActiveWorkoutSession()

            val logId: Long
            var curExIndex: Int
            var curSetNum: Int
            val rName = routine?.name ?: "Antrenman"

            if (activeSession != null && activeSession.routineId == routineId) {
                // Aktif oturumu geri yükle
                logId = activeSession.workoutLogId
                sessionStartTime = activeSession.startTimeMillis
                curExIndex = activeSession.currentExerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0))
                curSetNum = activeSession.currentSetNumber
                currentRestEndTimeMillis = activeSession.restEndTimeMillis
            } else {
                // Yeni oturum oluştur
                logId = repository.createWorkoutLog(routineId)
                sessionStartTime = System.currentTimeMillis()
                curExIndex = 0
                curSetNum = 1
                currentRestEndTimeMillis = null
            }

            // DB'deki gerçek set kayıtlarını sorgula ve pozisyonu doğrula (Self-healing)
            val allCompletedSets = repository.getSetLogsListForWorkout(logId)
            var isAllDone = false

            if (exercises.isNotEmpty()) {
                // Tamamlanmamış ilk egzersizi ve seti bul
                var foundUncompleted = false
                for ((idx, ex) in exercises.withIndex()) {
                    val setsForEx = allCompletedSets.filter { it.exerciseId == ex.exerciseId }
                    if (setsForEx.size < ex.targetSets) {
                        curExIndex = idx
                        curSetNum = setsForEx.size + 1
                        foundUncompleted = true
                        break
                    }
                }
                if (!foundUncompleted) {
                    // Tüm egzersizler ve setler tamamlanmış
                    curExIndex = (exercises.size - 1).coerceAtLeast(0)
                    curSetNum = exercises.last().targetSets
                    isAllDone = true
                }
            }

            val currentEx = exercises.getOrNull(curExIndex)
            var prevPerf: String? = null
            var prefilledWeight = ""
            var prefilledReps = ""

            if (currentEx != null) {
                val prevSets = repository.getPreviousWorkoutSetsForExercise(currentEx.exerciseId, logId)
                prevPerf = formatPreviousSets(prevSets)
                val firstPrev = prevSets.firstOrNull()
                if (firstPrev != null) {
                    prefilledWeight = formatWeight(firstPrev.weightKg)
                    prefilledReps = firstPrev.reps.toString()
                }
            }

            val exerciseCompletedSets = if (currentEx != null) {
                allCompletedSets.filter { it.exerciseId == currentEx.exerciseId }
            } else emptyList()
            val volume = repository.getVolumeForWorkout(logId)

            val lastSet = exerciseCompletedSets.lastOrNull() ?: allCompletedSets.lastOrNull()
            if (lastSet != null) {
                prefilledWeight = formatWeight(lastSet.weightKg)
                prefilledReps = lastSet.reps.toString()
            }

            // Dinlenme durumu kontrolü
            val now = System.currentTimeMillis()
            val isResting = currentRestEndTimeMillis != null && currentRestEndTimeMillis!! > now
            val remainingSeconds = if (isResting) ((currentRestEndTimeMillis!! - now) / 1000).toInt() else 0

            _uiState.update {
                it.copy(
                    isLoading = false,
                    routineName = rName,
                    workoutLogId = logId,
                    exercises = exercises,
                    currentExerciseIndex = curExIndex,
                    currentSetNumber = curSetNum,
                    previousPerformance = prevPerf,
                    totalRestDuration = currentEx?.restSeconds ?: 90,
                    weightInput = prefilledWeight,
                    repsInput = prefilledReps,
                    lastCompletedSet = lastSet,
                    completedSetsForCurrentExercise = exerciseCompletedSets,
                    totalCompletedSetsCount = allCompletedSets.size,
                    totalVolumeLifted = volume,
                    isRestTimerActive = isResting,
                    restSecondsRemaining = remainingSeconds,
                    isNextSetReady = false,
                    isAllWorkoutSetsCompleted = isAllDone
                )
            }

            persistCurrentSession(if (isResting) currentRestEndTimeMillis else null)

            if (isResting) {
                startRestTimerWithEndTime(
                    remainingSeconds,
                    currentRestEndTimeMillis!!,
                    currentEx?.restSeconds ?: 90,
                    context
                )
            }
        }
    }

    private fun persistCurrentSession(restEndTime: Long? = currentRestEndTimeMillis) {
        val state = _uiState.value
        if (state.workoutLogId <= 0) return

        repository.saveActiveWorkoutSession(
            ActiveWorkoutSessionData(
                routineId = state.routineId,
                routineName = state.routineName,
                workoutLogId = state.workoutLogId,
                currentExerciseIndex = state.currentExerciseIndex,
                currentSetNumber = state.currentSetNumber,
                startTimeMillis = sessionStartTime,
                restEndTimeMillis = restEndTime,
                totalRestDuration = state.totalRestDuration
            )
        )
    }

    fun onWeightInputChange(weight: String) {
        _uiState.update { it.copy(weightInput = weight, error = null) }
    }

    fun onRepsInputChange(reps: String) {
        _uiState.update { it.copy(repsInput = reps, error = null) }
    }

    fun adjustWeight(delta: Double) {
        val current = parseWeight(_uiState.value.weightInput) ?: 0.0
        val newWeight = (current + delta).coerceAtLeast(0.0)
        _uiState.update {
            it.copy(
                weightInput = formatWeight(newWeight),
                error = null
            )
        }
    }

    fun adjustReps(delta: Int) {
        val current = _uiState.value.repsInput.toIntOrNull() ?: 0
        val newReps = (current + delta).coerceAtLeast(1)
        _uiState.update {
            it.copy(
                repsInput = newReps.toString(),
                error = null
            )
        }
    }

    fun completeSet(context: Context? = null) {
        val state = _uiState.value
        val currentEx = state.currentExercise ?: return
        val weight = parseWeight(state.weightInput)
        val reps = state.repsInput.toIntOrNull()

        if (weight == null || weight <= 0.0) {
            _uiState.update { it.copy(error = "Lütfen geçerli bir ağırlık (kg) girin (örn: 50 veya 52.5).") }
            return
        }
        if (reps == null || reps <= 0) {
            _uiState.update { it.copy(error = "Lütfen geçerli bir tekrar sayısı girin.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            // PR Kontrolü (Bu hareket için geçmiş antrenmanlardaki en yüksek ağırlık)
            val pastSets = repository.getAllSetLogsForExercise(currentEx.exerciseId)
            val previousSessionsSets = pastSets.filter { it.workoutLogId != state.workoutLogId }
            val previousMax = previousSessionsSets.maxOfOrNull { it.weightKg } ?: 0.0

            val isNewPr = weight > previousMax && weight > 0.0 && previousSessionsSets.isNotEmpty()

            val savedSetId = repository.insertSetLog(
                workoutLogId = state.workoutLogId,
                exerciseId = currentEx.exerciseId,
                setNumber = state.currentSetNumber,
                weightKg = weight,
                reps = reps
            )

            val completedSet = SetLogEntity(
                id = savedSetId,
                workoutLogId = state.workoutLogId,
                exerciseId = currentEx.exerciseId,
                setNumber = state.currentSetNumber,
                weightKg = weight,
                reps = reps,
                timestamp = System.currentTimeMillis()
            )

            val updatedSets = repository.getSetLogsListForWorkout(state.workoutLogId)
            val volume = repository.getVolumeForWorkout(state.workoutLogId)

            val currentPrList = _uiState.value.sessionPrs.toMutableList()
            if (isNewPr) {
                val existingPrIdx = currentPrList.indexOfFirst { it.exerciseName == currentEx.exerciseName }
                val newPrItem = StoryPrItem(currentEx.exerciseName, weight, reps)
                if (existingPrIdx >= 0) {
                    if (weight > currentPrList[existingPrIdx].weightKg) {
                        currentPrList[existingPrIdx] = newPrItem
                    }
                } else {
                    currentPrList.add(newPrItem)
                }

                NotificationHelper.playPrCelebrationSound(context)
            }

            // 🌟 OTOMATİK SONRAKİ SET / HAREKET HESAPLAMASI
            val nextExIndex: Int
            val nextSetNum: Int
            val nextWeight: String
            val nextReps: String
            val nextPrevPerf: String?
            val nextRestDuration: Int
            val nextExerciseSets: List<SetLogEntity>
            val isAllDone: Boolean

            if (state.currentSetNumber < currentEx.targetSets) {
                // Aynı egzersizin bir sonraki seti
                nextExIndex = state.currentExerciseIndex
                nextSetNum = state.currentSetNumber + 1
                nextWeight = formatWeight(weight)
                nextReps = reps.toString()
                nextPrevPerf = state.previousPerformance
                nextRestDuration = currentEx.restSeconds
                nextExerciseSets = updatedSets.filter { it.exerciseId == currentEx.exerciseId }
                isAllDone = false
            } else if (state.currentExerciseIndex < state.exercises.size - 1) {
                // Sonraki egzersize geçiş
                nextExIndex = state.currentExerciseIndex + 1
                val nextEx = state.exercises[nextExIndex]
                nextSetNum = 1

                val prevSets = repository.getPreviousWorkoutSetsForExercise(nextEx.exerciseId, state.workoutLogId)
                nextPrevPerf = formatPreviousSets(prevSets)
                val firstPrev = prevSets.firstOrNull()
                nextWeight = firstPrev?.let { formatWeight(it.weightKg) } ?: ""
                nextReps = firstPrev?.reps?.toString() ?: ""
                nextRestDuration = nextEx.restSeconds
                nextExerciseSets = emptyList()
                isAllDone = false
            } else {
                // Tüm egzersizler ve setler tamamlandı!
                nextExIndex = state.currentExerciseIndex
                nextSetNum = state.currentSetNumber
                nextWeight = formatWeight(weight)
                nextReps = reps.toString()
                nextPrevPerf = state.previousPerformance
                nextRestDuration = currentEx.restSeconds
                nextExerciseSets = updatedSets.filter { it.exerciseId == currentEx.exerciseId }
                isAllDone = true
            }

            // Dinlenme Sayacını Başlat
            val durationSeconds = currentEx.restSeconds
            val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)
            currentRestEndTimeMillis = endTime

            _uiState.update {
                it.copy(
                    currentExerciseIndex = nextExIndex,
                    currentSetNumber = nextSetNum,
                    weightInput = nextWeight,
                    repsInput = nextReps,
                    previousPerformance = nextPrevPerf,
                    totalRestDuration = nextRestDuration,
                    lastCompletedSet = completedSet,
                    completedSetsForCurrentExercise = nextExerciseSets,
                    totalCompletedSetsCount = updatedSets.size,
                    totalVolumeLifted = volume,
                    sessionPrs = currentPrList,
                    newPrEvent = if (isNewPr) NewPrEvent(currentEx.exerciseName, weight, reps, previousMax) else it.newPrEvent,
                    isRestTimerActive = true,
                    restSecondsRemaining = durationSeconds,
                    isNextSetReady = false,
                    isAllWorkoutSetsCompleted = isAllDone
                )
            }

            // Oturumu hemen sonraki set pozisyonuyla kaydet (Yeniden açıldığında asla aynı sete dönmez!)
            persistCurrentSession(endTime)

            startRestTimerWithEndTime(durationSeconds, endTime, durationSeconds, context)
        }
    }

    fun dismissPrCelebration() {
        _uiState.update { it.copy(newPrEvent = null) }
    }

    fun shareWorkoutStory(context: Context, userPhoto: android.graphics.Bitmap? = null) {
        val state = _uiState.value
        val exNames = state.exercises.map { it.exerciseName }
        val bitmap = ShareCardGenerator.generateWorkoutStoryBitmap(
            context = context,
            routineName = state.routineName,
            durationMinutes = state.workoutDurationMinutes,
            totalVolume = state.totalVolumeLifted,
            totalSets = state.totalCompletedSetsCount,
            prList = state.sessionPrs,
            exerciseNames = exNames,
            userPhotoBitmap = userPhoto
        )
        ShareCardGenerator.shareToStory(context, bitmap)
    }

    fun saveWorkoutStoryToGallery(context: Context, userPhoto: android.graphics.Bitmap? = null) {
        val state = _uiState.value
        val exNames = state.exercises.map { it.exerciseName }
        val bitmap = ShareCardGenerator.generateWorkoutStoryBitmap(
            context = context,
            routineName = state.routineName,
            durationMinutes = state.workoutDurationMinutes,
            totalVolume = state.totalVolumeLifted,
            totalSets = state.totalCompletedSetsCount,
            prList = state.sessionPrs,
            exerciseNames = exNames,
            userPhotoBitmap = userPhoto
        )
        ShareCardGenerator.saveImageToGallery(context, bitmap)
    }

    private fun startRestTimerWithEndTime(
        seconds: Int,
        endTimeMillis: Long,
        totalDuration: Int,
        context: Context?
    ) {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isRestTimerActive = true,
                restSecondsRemaining = seconds,
                totalRestDuration = totalDuration,
                isNextSetReady = false
            )
        }

        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((endTimeMillis - now) / 1000).toInt()

                if (remaining <= 0) {
                    break
                }

                _uiState.update { it.copy(restSecondsRemaining = remaining) }
                delay(500)
            }

            // Timer Finished
            currentRestEndTimeMillis = null
            persistCurrentSession(null)

            val state = _uiState.value
            if (state.isAllWorkoutSetsCompleted) {
                finishWorkout()
            } else {
                _uiState.update {
                    it.copy(
                        isRestTimerActive = false,
                        isNextSetReady = false,
                        restSecondsRemaining = 0
                    )
                }
            }

            if (context != null) {
                val exName = _uiState.value.currentExercise?.exerciseName ?: "Egzersiz"
                val nextSet = _uiState.value.currentSetNumber
                val soundType = repository.getSoundType()
                val isVibration = repository.isVibrationEnabled()

                NotificationHelper.sendRestTimerCompletedNotification(
                    context = context,
                    exerciseName = exName,
                    setNumber = nextSet,
                    isVibrationEnabled = isVibration
                )
                NotificationHelper.playSoundByType(soundType)
                if (isVibration) {
                    NotificationHelper.triggerVibration(context)
                }
            }
        }
    }

    fun addRestSeconds(extraSeconds: Int, context: Context? = null) {
        val currentRemaining = _uiState.value.restSecondsRemaining
        val newRemaining = currentRemaining + extraSeconds
        val newEndTime = System.currentTimeMillis() + (newRemaining * 1000L)
        val newTotal = maxOf(_uiState.value.totalRestDuration, newRemaining)

        currentRestEndTimeMillis = newEndTime
        persistCurrentSession(newEndTime)

        startRestTimerWithEndTime(newRemaining, newEndTime, newTotal, context)
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        currentRestEndTimeMillis = null
        persistCurrentSession(null)

        val state = _uiState.value
        if (state.isAllWorkoutSetsCompleted) {
            finishWorkout()
        } else {
            _uiState.update {
                it.copy(
                    isRestTimerActive = false,
                    restSecondsRemaining = 0,
                    isNextSetReady = false
                )
            }
        }
    }

    fun moveToNextSetOrExercise() {
        timerJob?.cancel()
        currentRestEndTimeMillis = null
        persistCurrentSession(null)

        val state = _uiState.value
        if (state.isAllWorkoutSetsCompleted) {
            finishWorkout()
        } else {
            _uiState.update {
                it.copy(
                    isRestTimerActive = false,
                    isNextSetReady = false,
                    restSecondsRemaining = 0
                )
            }
        }
    }

    fun finishWorkout() {
        timerJob?.cancel()
        currentRestEndTimeMillis = null
        repository.clearActiveWorkoutSession()

        val state = _uiState.value
        val durationMillis = System.currentTimeMillis() - sessionStartTime
        val durationMinutes = maxOf(1, (durationMillis / (1000 * 60)).toInt())

        viewModelScope.launch {
            val log = WorkoutLogEntity(
                id = state.workoutLogId,
                routineId = state.routineId,
                timestamp = sessionStartTime,
                durationMinutes = durationMinutes
            )
            repository.updateWorkoutLog(log)

            val volume = repository.getVolumeForWorkout(state.workoutLogId)

            _uiState.update {
                it.copy(
                    isWorkoutFinished = true,
                    workoutDurationMinutes = durationMinutes,
                    totalVolumeLifted = volume
                )
            }
        }
    }

    fun cancelWorkout() {
        timerJob?.cancel()
        currentRestEndTimeMillis = null
        val state = _uiState.value

        viewModelScope.launch {
            repository.clearActiveWorkoutSession()
            if (state.workoutLogId > 0) {
                repository.deleteWorkout(state.workoutLogId)
            }
            _uiState.update {
                it.copy(isWorkoutFinished = true)
            }
        }
    }

    fun loadPastWorkoutSummary(workoutLogId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val workoutLog = repository.getWorkoutLogById(workoutLogId)
            if (workoutLog != null) {
                sessionStartTime = workoutLog.timestamp
                val routineIdVal = workoutLog.routineId ?: 0L
                val routine = repository.getRoutineById(routineIdVal).firstOrNull()
                val exercises = repository.getRoutineExercisesList(routineIdVal)
                val allSets = repository.getSetLogsListForWorkout(workoutLogId)
                val volume = repository.getVolumeForWorkout(workoutLogId)

                // PR'ları bul
                val prList = mutableListOf<StoryPrItem>()
                for (ex in exercises) {
                    val setsForEx = allSets.filter { it.exerciseId == ex.exerciseId }
                    val maxSet = setsForEx.maxByOrNull { it.weightKg }
                    if (maxSet != null && maxSet.weightKg > 0) {
                        prList.add(StoryPrItem(ex.exerciseName, maxSet.weightKg, maxSet.reps))
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        workoutLogId = workoutLogId,
                        routineId = routineIdVal,
                        routineName = routine?.name ?: "Geçmiş Antrenman",
                        workoutDurationMinutes = workoutLog.durationMinutes,
                        totalVolumeLifted = volume,
                        totalCompletedSetsCount = allSets.size,
                        exercises = exercises,
                        sessionPrs = prList,
                        isWorkoutFinished = true,
                        workoutDateTimestamp = workoutLog.timestamp
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
