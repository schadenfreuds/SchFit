package com.example.schfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.entity.ExerciseEntity
import com.example.schfit.data.entity.WorkoutRoutineEntity
import com.example.schfit.data.repository.SchFitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedExerciseConfig(
    val exercise: ExerciseEntity,
    val targetSets: Int = 3,
    val restSeconds: Int = 90
)

data class WorkoutUiState(
    val selectedTab: Int = 0, // 0 = Programlarım, 1 = Antrenman Geçmişi
    val routines: List<WorkoutRoutineEntity> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val workoutHistory: List<WorkoutHistoryUiModel> = emptyList(),
    val selectedWorkoutDetail: WorkoutDetailState? = null,
    val isLoadingDetail: Boolean = false,
    val workoutToDelete: WorkoutHistoryUiModel? = null,
    val isCreateDialogOpen: Boolean = false,
    val editingRoutineId: Long? = null,
    val draftRoutineName: String = "",
    val draftExercises: List<SelectedExerciseConfig> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean
        get() = editingRoutineId != null
}

private data class WorkoutLocalDraftState(
    val selectedTab: Int = 0,
    val isCreateDialogOpen: Boolean = false,
    val editingRoutineId: Long? = null,
    val draftRoutineName: String = "",
    val draftExercises: List<SelectedExerciseConfig> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val selectedWorkoutDetail: WorkoutDetailState? = null,
    val isLoadingDetail: Boolean = false,
    val workoutToDelete: WorkoutHistoryUiModel? = null
)

class WorkoutViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _draftState = MutableStateFlow(WorkoutLocalDraftState())
    private val _workoutHistory = MutableStateFlow<List<WorkoutHistoryUiModel>>(emptyList())

    init {
        loadWorkoutHistory()
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        repository.getAllRoutines(),
        repository.getAllExercises(),
        _workoutHistory,
        _draftState
    ) { routines, exercises, history, draft ->
        WorkoutUiState(
            selectedTab = draft.selectedTab,
            routines = routines,
            availableExercises = exercises,
            workoutHistory = history,
            selectedWorkoutDetail = draft.selectedWorkoutDetail,
            isLoadingDetail = draft.isLoadingDetail,
            workoutToDelete = draft.workoutToDelete,
            isCreateDialogOpen = draft.isCreateDialogOpen,
            editingRoutineId = draft.editingRoutineId,
            draftRoutineName = draft.draftRoutineName,
            draftExercises = draft.draftExercises,
            isSaving = draft.isSaving,
            errorMessage = draft.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkoutUiState()
    )

    fun selectTab(tabIndex: Int) {
        _draftState.update { it.copy(selectedTab = tabIndex) }
    }

    fun openCreateDialog() {
        _draftState.update {
            it.copy(
                editingRoutineId = null,
                draftRoutineName = "",
                draftExercises = emptyList(),
                errorMessage = null,
                isCreateDialogOpen = true
            )
        }
    }

    fun openEditDialog(routine: WorkoutRoutineEntity) {
        viewModelScope.launch {
            val routineExercises = repository.getRoutineExercisesList(routine.id)
            val mapped = routineExercises.map {
                SelectedExerciseConfig(
                    exercise = ExerciseEntity(
                        id = it.exerciseId,
                        name = it.exerciseName,
                        muscleGroup = it.muscleGroup,
                        notes = it.notes
                    ),
                    targetSets = it.targetSets,
                    restSeconds = it.restSeconds
                )
            }
            _draftState.update {
                it.copy(
                    editingRoutineId = routine.id,
                    draftRoutineName = routine.name,
                    draftExercises = mapped,
                    errorMessage = null,
                    isCreateDialogOpen = true
                )
            }
        }
    }

    fun closeCreateDialog() {
        _draftState.update {
            it.copy(
                isCreateDialogOpen = false,
                editingRoutineId = null,
                draftRoutineName = "",
                draftExercises = emptyList(),
                errorMessage = null
            )
        }
    }

    fun onRoutineNameChange(name: String) {
        if (name.length <= 40) {
            _draftState.update { it.copy(draftRoutineName = name) }
        }
    }

    fun addExerciseToDraft(exercise: ExerciseEntity) {
        val currentList = _draftState.value.draftExercises
        if (currentList.none { it.exercise.id == exercise.id }) {
            _draftState.update {
                it.copy(draftExercises = currentList + SelectedExerciseConfig(exercise))
            }
        }
    }

    fun removeExerciseFromDraft(exerciseId: Long) {
        _draftState.update {
            it.copy(draftExercises = it.draftExercises.filter { ex -> ex.exercise.id != exerciseId })
        }
    }

    fun updateExerciseSets(exerciseId: Long, sets: Int) {
        val coercedSets = sets.coerceIn(1, 20)
        _draftState.update {
            it.copy(
                draftExercises = it.draftExercises.map { ex ->
                    if (ex.exercise.id == exerciseId) ex.copy(targetSets = coercedSets) else ex
                }
            )
        }
    }

    fun updateExerciseRestSeconds(exerciseId: Long, seconds: Int) {
        val coercedSeconds = seconds.coerceIn(15, 600)
        _draftState.update {
            it.copy(
                draftExercises = it.draftExercises.map { ex ->
                    if (ex.exercise.id == exerciseId) ex.copy(restSeconds = coercedSeconds) else ex
                }
            )
        }
    }

    fun moveExerciseUp(exerciseId: Long) {
        val list = _draftState.value.draftExercises.toMutableList()
        val index = list.indexOfFirst { it.exercise.id == exerciseId }
        if (index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            _draftState.update { it.copy(draftExercises = list) }
        }
    }

    fun moveExerciseDown(exerciseId: Long) {
        val list = _draftState.value.draftExercises.toMutableList()
        val index = list.indexOfFirst { it.exercise.id == exerciseId }
        if (index >= 0 && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            _draftState.update { it.copy(draftExercises = list) }
        }
    }

    fun saveRoutine() {
        val draft = _draftState.value
        val name = draft.draftRoutineName.trim()
        if (name.isBlank()) {
            _draftState.update { it.copy(errorMessage = "Lütfen program adı girin.") }
            return
        }
        if (draft.draftExercises.isEmpty()) {
            _draftState.update { it.copy(errorMessage = "Lütfen en az bir egzersiz ekleyin.") }
            return
        }

        viewModelScope.launch {
            _draftState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val exercises = draft.draftExercises.map {
                    it.exercise.id to (it.targetSets to it.restSeconds)
                }
                val editingId = draft.editingRoutineId
                if (editingId != null) {
                    repository.updateRoutineWithExercises(editingId, name, exercises)
                } else {
                    repository.createRoutineWithExercises(name, exercises)
                }
                closeCreateDialog()
            } catch (e: Exception) {
                _draftState.update { it.copy(errorMessage = "Kaydedilirken hata oluştu: ${e.localizedMessage}") }
            } finally {
                _draftState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
        }
    }

    // --- Antrenman Geçmişi Yönetimi ---
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

    fun selectWorkoutForDetail(item: WorkoutHistoryUiModel) {
        viewModelScope.launch {
            _draftState.update { it.copy(isLoadingDetail = true) }
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
                _draftState.update { it.copy(selectedWorkoutDetail = detail, isLoadingDetail = false) }
            } catch (e: Exception) {
                _draftState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    fun closeWorkoutDetail() {
        _draftState.update { it.copy(selectedWorkoutDetail = null) }
    }

    fun requestDeleteWorkout(item: WorkoutHistoryUiModel) {
        _draftState.update { it.copy(workoutToDelete = item) }
    }

    fun cancelDeleteWorkout() {
        _draftState.update { it.copy(workoutToDelete = null) }
    }

    fun confirmDeleteWorkout() {
        val item = _draftState.value.workoutToDelete ?: return
        viewModelScope.launch {
            try {
                repository.deleteWorkout(item.logId)
                _draftState.update { it.copy(workoutToDelete = null) }
                if (_draftState.value.selectedWorkoutDetail?.logId == item.logId) {
                    _draftState.update { it.copy(selectedWorkoutDetail = null) }
                }
            } catch (e: Exception) {
                _draftState.update { it.copy(errorMessage = "Antrenman silinirken hata oluştu.") }
            }
        }
    }
}
