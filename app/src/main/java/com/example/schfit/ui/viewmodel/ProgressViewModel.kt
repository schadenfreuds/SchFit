package com.example.schfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.entity.BodyMetricEntity
import com.example.schfit.data.repository.SchFitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutHistoryUiModel(
    val logId: Long,
    val routineName: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val totalSets: Int,
    val totalVolume: Double
)

data class ExerciseSetDetail(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val volume: Double
)

data class ExerciseDetailGroup(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val sets: List<ExerciseSetDetail>,
    val totalVolume: Double
)

data class WorkoutDetailState(
    val logId: Long,
    val routineName: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val totalVolume: Double,
    val totalSets: Int,
    val exercises: List<ExerciseDetailGroup>
)

data class ProgressUiState(
    val metricsList: List<BodyMetricEntity> = emptyList(),
    val workoutHistory: List<WorkoutHistoryUiModel> = emptyList(),
    val totalOverallVolume: Double = 0.0,
    val totalWorkoutsCount: Int = 0,
    val weightInput: String = "",
    val isSavingWeight: Boolean = false,
    val errorMessage: String? = null,
    val targetWeight: Double? = null,
    val targetWeightInput: String = "",
    val isTargetWeightDialogOpen: Boolean = false,
    val selectedWorkoutDetail: WorkoutDetailState? = null,
    val isLoadingDetail: Boolean = false,
    val workoutToDelete: WorkoutHistoryUiModel? = null,
    val isHistoryDialogOpen: Boolean = false,
    val isWeightHistoryDialogOpen: Boolean = false,
    val weightToDelete: BodyMetricEntity? = null
) {
    val initialWeight: Double?
        get() = metricsList.minByOrNull { it.timestamp }?.weightKg

    val currentWeight: Double?
        get() = metricsList.maxByOrNull { it.timestamp }?.weightKg

    val weightDifference: Double?
        get() = if (initialWeight != null && currentWeight != null) currentWeight!! - initialWeight!! else null

    val remainingToGoal: Double?
        get() = if (currentWeight != null && targetWeight != null) currentWeight!! - targetWeight!! else null
}

private data class LocalProgressState(
    val weightInput: String = "",
    val isSavingWeight: Boolean = false,
    val errorMessage: String? = null,
    val targetWeightInput: String = "",
    val isTargetWeightDialogOpen: Boolean = false,
    val selectedWorkoutDetail: WorkoutDetailState? = null,
    val isLoadingDetail: Boolean = false,
    val workoutToDelete: WorkoutHistoryUiModel? = null,
    val isHistoryDialogOpen: Boolean = false,
    val isWeightHistoryDialogOpen: Boolean = false,
    val weightToDelete: BodyMetricEntity? = null
)

class ProgressViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalProgressState())
    private val _workoutHistory = MutableStateFlow<List<WorkoutHistoryUiModel>>(emptyList())

    init {
        loadWorkoutHistory()
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        combine(
            repository.getAllMetrics(),
            _workoutHistory,
            repository.getTotalVolumeFlow(),
            repository.getTargetWeightFlow()
        ) { metrics, history, totalVol, targetWeight ->
            ProgressMetricsIntermediate(metrics, history, totalVol, targetWeight)
        },
        repository.getTotalWorkoutsCount(),
        _localState
    ) { intermediate, workoutCount, local ->
        ProgressUiState(
            metricsList = intermediate.metrics,
            workoutHistory = intermediate.history,
            totalOverallVolume = intermediate.totalVol,
            totalWorkoutsCount = workoutCount,
            weightInput = local.weightInput,
            isSavingWeight = local.isSavingWeight,
            errorMessage = local.errorMessage,
            targetWeight = intermediate.targetWeight,
            targetWeightInput = local.targetWeightInput,
            isTargetWeightDialogOpen = local.isTargetWeightDialogOpen,
            selectedWorkoutDetail = local.selectedWorkoutDetail,
            isLoadingDetail = local.isLoadingDetail,
            workoutToDelete = local.workoutToDelete,
            isHistoryDialogOpen = local.isHistoryDialogOpen,
            isWeightHistoryDialogOpen = local.isWeightHistoryDialogOpen,
            weightToDelete = local.weightToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState()
    )

    fun onWeightInputChange(input: String) {
        _localState.update { it.copy(weightInput = input) }
    }

    fun addWeightMetric(onSuccess: () -> Unit = {}) {
        val weight = _localState.value.weightInput.trim().replace(',', '.').toDoubleOrNull()
        if (weight == null || weight <= 0.0 || weight > 500.0) {
            _localState.update { it.copy(errorMessage = "Lütfen geçerli bir kilo değeri girin (örn: 75.5 veya 75,5).") }
            return
        }

        viewModelScope.launch {
            _localState.update { it.copy(isSavingWeight = true, errorMessage = null) }
            try {
                repository.addBodyMetric(weight)
                _localState.update { it.copy(weightInput = "") }
                onSuccess()
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = "Kilo kaydedilirken hata oluştu: ${e.localizedMessage}") }
            } finally {
                _localState.update { it.copy(isSavingWeight = false) }
            }
        }
    }

    // Hedef Kilo Yönetimi
    fun openTargetWeightDialog() {
        val currentTarget = repository.getTargetWeight()
        _localState.update {
            it.copy(
                isTargetWeightDialogOpen = true,
                targetWeightInput = currentTarget?.let { t -> "%.1f".format(t).replace(',', '.') } ?: "",
                errorMessage = null
            )
        }
    }

    fun closeTargetWeightDialog() {
        _localState.update { it.copy(isTargetWeightDialogOpen = false) }
    }

    fun onTargetWeightInputChange(input: String) {
        _localState.update { it.copy(targetWeightInput = input) }
    }

    fun saveTargetWeight() {
        val input = _localState.value.targetWeightInput.trim().replace(',', '.')
        val target = input.toDoubleOrNull()
        if (target == null || target <= 0.0 || target > 500.0) {
            _localState.update { it.copy(errorMessage = "Lütfen geçerli bir hedef kilo girin.") }
            return
        }
        repository.setTargetWeight(target)
        closeTargetWeightDialog()
    }

    fun clearTargetWeight() {
        repository.setTargetWeight(null)
        closeTargetWeightDialog()
    }

    fun openWeightHistoryDialog() {
        _localState.update { it.copy(isWeightHistoryDialogOpen = true) }
    }

    fun closeWeightHistoryDialog() {
        _localState.update { it.copy(isWeightHistoryDialogOpen = false) }
    }

    fun requestDeleteWeight(metric: BodyMetricEntity) {
        _localState.update { it.copy(weightToDelete = metric) }
    }

    fun cancelDeleteWeight() {
        _localState.update { it.copy(weightToDelete = null) }
    }

    fun confirmDeleteWeight() {
        val toDelete = _localState.value.weightToDelete ?: return
        viewModelScope.launch {
            repository.deleteBodyMetric(toDelete.id)
            _localState.update { it.copy(weightToDelete = null) }
        }
    }

    fun deleteWeightMetric(id: Long) {
        viewModelScope.launch {
            repository.deleteBodyMetric(id)
        }
    }

    fun openHistoryDialog() {
        _localState.update { it.copy(isHistoryDialogOpen = true) }
    }

    fun closeHistoryDialog() {
        _localState.update { it.copy(isHistoryDialogOpen = false) }
    }

    fun selectWorkoutForDetail(item: WorkoutHistoryUiModel) {
        viewModelScope.launch {
            _localState.update { it.copy(isLoadingDetail = true) }
            try {
                val detailedSets = repository.getDetailedSetLogsForWorkout(item.logId)
                val groups = detailedSets.groupBy { it.exerciseId }.map { (exId, sets) ->
                    val first = sets.first()
                    val setDetails = sets.map { s ->
                        ExerciseSetDetail(
                            setNumber = s.setNumber,
                            weightKg = s.weightKg,
                            reps = s.reps,
                            volume = s.weightKg * s.reps
                        )
                    }
                    ExerciseDetailGroup(
                        exerciseId = exId,
                        exerciseName = first.exerciseName,
                        muscleGroup = first.muscleGroup,
                        sets = setDetails,
                        totalVolume = setDetails.sumOf { it.volume }
                    )
                }

                val detail = WorkoutDetailState(
                    logId = item.logId,
                    routineName = item.routineName,
                    timestamp = item.timestamp,
                    durationMinutes = item.durationMinutes,
                    totalVolume = item.totalVolume,
                    totalSets = item.totalSets,
                    exercises = groups
                )
                _localState.update { it.copy(selectedWorkoutDetail = detail, isLoadingDetail = false) }
            } catch (e: Exception) {
                _localState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    fun closeWorkoutDetail() {
        _localState.update { it.copy(selectedWorkoutDetail = null) }
    }

    fun requestDeleteWorkout(item: WorkoutHistoryUiModel) {
        _localState.update { it.copy(workoutToDelete = item) }
    }

    fun cancelDeleteWorkout() {
        _localState.update { it.copy(workoutToDelete = null) }
    }

    fun confirmDeleteWorkout() {
        val item = _localState.value.workoutToDelete ?: return
        viewModelScope.launch {
            try {
                repository.deleteWorkout(item.logId)
                _localState.update { it.copy(workoutToDelete = null) }
                if (_localState.value.selectedWorkoutDetail?.logId == item.logId) {
                    _localState.update { it.copy(selectedWorkoutDetail = null) }
                }
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = "Antrenman silinirken hata oluştu.") }
            }
        }
    }

    fun loadWorkoutHistory() {
        viewModelScope.launch {
            repository.getAllWorkoutLogs().collect { logs ->
                val historyItems = logs.map { log ->
                    val routine = log.routineId?.let { repository.getRoutineById(it).firstOrNull() }
                    val sets = repository.getSetLogsListForWorkout(log.id)
                    val volume = repository.getVolumeForWorkout(log.id)

                    WorkoutHistoryUiModel(
                        logId = log.id,
                        routineName = routine?.name ?: "Serbest Antrenman",
                        timestamp = log.timestamp,
                        durationMinutes = log.durationMinutes,
                        totalSets = sets.size,
                        totalVolume = volume
                    )
                }
                _workoutHistory.value = historyItems
            }
        }
    }
}

private data class ProgressMetricsIntermediate(
    val metrics: List<com.example.schfit.data.entity.BodyMetricEntity>,
    val history: List<WorkoutHistoryUiModel>,
    val totalVol: Double,
    val targetWeight: Double?
)
