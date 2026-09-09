package com.example.schfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.entity.ExerciseEntity
import com.example.schfit.data.entity.SetLogEntity
import com.example.schfit.data.repository.SchFitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseWorkoutPoint(
    val workoutLogId: Long,
    val timestamp: Long,
    val maxWeightKg: Double,
    val bestRepsAtMaxWeight: Int,
    val allSets: List<SetLogEntity>
)

data class ExerciseDetailProgressData(
    val exercise: ExerciseEntity,
    val weightHistoryPoints: List<ExerciseWorkoutPoint> = emptyList(),
    val personalRecordWeight: Double = 0.0,
    val personalRecordReps: Int = 0,
    val estimatedOneRepMax: Double = 0.0,
    val oneRepMaxSourceSet: SetLogEntity? = null,
    val totalSetsCompleted: Int = 0,
    val totalWorkoutsCount: Int = 0
)

data class ExerciseLibraryUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedMuscleGroup: String = "Tümü",
    val isAddEditDialogOpen: Boolean = false,
    val editingExerciseId: Long? = null,
    val draftName: String = "",
    val draftMuscleGroup: String = "Göğüs",
    val draftNotes: String = "",
    val isSaving: Boolean = false,
    val exerciseToDelete: ExerciseEntity? = null,
    val selectedExerciseProgress: ExerciseDetailProgressData? = null,
    val isProgressDialogOpen: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean
        get() = editingExerciseId != null
}

private data class ExerciseFilterAndFormState(
    val searchQuery: String = "",
    val selectedMuscleGroup: String = "Tümü",
    val isAddEditDialogOpen: Boolean = false,
    val editingExerciseId: Long? = null,
    val draftName: String = "",
    val draftMuscleGroup: String = "Omuz",
    val draftNotes: String = "",
    val isSaving: Boolean = false,
    val exerciseToDelete: ExerciseEntity? = null,
    val selectedExerciseProgress: ExerciseDetailProgressData? = null,
    val isProgressDialogOpen: Boolean = false,
    val errorMessage: String? = null
)

class ExerciseLibraryViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    companion object {
        val MUSCLE_GROUPS = listOf("Tümü", "Omuz", "Göğüs", "Sırt", "Biceps", "Triceps", "Bacak", "Karın")
        val SELECTABLE_MUSCLE_GROUPS = listOf("Omuz", "Göğüs", "Sırt", "Biceps", "Triceps", "Bacak", "Karın")
    }

    private val _localState = MutableStateFlow(ExerciseFilterAndFormState())

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        repository.getAllExercises(),
        _localState
    ) { allExercises, local ->
        val filtered = allExercises.filter { ex ->
            val matchesGroup = if (local.selectedMuscleGroup == "Tümü") true else ex.muscleGroup.equals(local.selectedMuscleGroup, ignoreCase = true)
            val matchesSearch = local.searchQuery.isBlank() ||
                    ex.name.contains(local.searchQuery, ignoreCase = true) ||
                    ex.muscleGroup.contains(local.searchQuery, ignoreCase = true) ||
                    ex.notes.contains(local.searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }

        ExerciseLibraryUiState(
            exercises = filtered,
            searchQuery = local.searchQuery,
            selectedMuscleGroup = local.selectedMuscleGroup,
            isAddEditDialogOpen = local.isAddEditDialogOpen,
            editingExerciseId = local.editingExerciseId,
            draftName = local.draftName,
            draftMuscleGroup = local.draftMuscleGroup,
            draftNotes = local.draftNotes,
            isSaving = local.isSaving,
            exerciseToDelete = local.exerciseToDelete,
            selectedExerciseProgress = local.selectedExerciseProgress,
            isProgressDialogOpen = local.isProgressDialogOpen,
            errorMessage = local.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExerciseLibraryUiState()
    )

    fun onSearchQueryChange(query: String) {
        _localState.update { it.copy(searchQuery = query) }
    }

    fun onMuscleGroupSelect(group: String) {
        _localState.update { it.copy(selectedMuscleGroup = group) }
    }

    fun openExerciseProgress(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val allSets = repository.getAllSetLogsForExercise(exercise.id)

            val groupedByWorkout = allSets.groupBy { it.workoutLogId }
            val points = groupedByWorkout.map { (workoutLogId, setsInWorkout) ->
                val sortedSets = setsInWorkout.sortedBy { it.setNumber }
                val maxWeight = setsInWorkout.maxOfOrNull { it.weightKg } ?: 0.0
                val bestReps = setsInWorkout.filter { it.weightKg == maxWeight }.maxOfOrNull { it.reps } ?: 0
                val minTimestamp = setsInWorkout.minOfOrNull { it.timestamp } ?: 0L
                ExerciseWorkoutPoint(
                    workoutLogId = workoutLogId,
                    timestamp = minTimestamp,
                    maxWeightKg = maxWeight,
                    bestRepsAtMaxWeight = bestReps,
                    allSets = sortedSets
                )
            }.sortedBy { it.timestamp }

            val prWeight = allSets.maxOfOrNull { it.weightKg } ?: 0.0
            val prReps = allSets.filter { it.weightKg == prWeight }.maxOfOrNull { it.reps } ?: 0

            // Epley Formula: 1RM = Weight * (1 + Reps / 30)
            fun calculate1RM(weight: Double, reps: Int): Double {
                if (weight <= 0.0) return 0.0
                if (reps <= 1) return weight
                return weight * (1.0 + (reps / 30.0))
            }

            val best1RMSet = allSets.maxByOrNull { calculate1RM(it.weightKg, it.reps) }
            val estimated1RM = best1RMSet?.let { calculate1RM(it.weightKg, it.reps) } ?: 0.0

            val progressData = ExerciseDetailProgressData(
                exercise = exercise,
                weightHistoryPoints = points,
                personalRecordWeight = prWeight,
                personalRecordReps = prReps,
                estimatedOneRepMax = estimated1RM,
                oneRepMaxSourceSet = best1RMSet,
                totalSetsCompleted = allSets.size,
                totalWorkoutsCount = points.size
            )

            _localState.update {
                it.copy(
                    selectedExerciseProgress = progressData,
                    isProgressDialogOpen = true
                )
            }
        }
    }

    fun closeProgressDialog() {
        _localState.update {
            it.copy(
                isProgressDialogOpen = false,
                selectedExerciseProgress = null
            )
        }
    }

    fun openAddDialog() {
        val defaultGroup = if (_localState.value.selectedMuscleGroup in SELECTABLE_MUSCLE_GROUPS) {
            _localState.value.selectedMuscleGroup
        } else "Göğüs"

        _localState.update {
            it.copy(
                isAddEditDialogOpen = true,
                editingExerciseId = null,
                draftName = "",
                draftMuscleGroup = defaultGroup,
                draftNotes = "",
                errorMessage = null
            )
        }
    }

    fun openEditDialog(exercise: ExerciseEntity) {
        _localState.update {
            it.copy(
                isAddEditDialogOpen = true,
                editingExerciseId = exercise.id,
                draftName = exercise.name,
                draftMuscleGroup = exercise.muscleGroup,
                draftNotes = exercise.notes,
                errorMessage = null
            )
        }
    }

    fun closeAddEditDialog() {
        _localState.update {
            it.copy(
                isAddEditDialogOpen = false,
                editingExerciseId = null,
                draftName = "",
                draftMuscleGroup = "Göğüs",
                draftNotes = "",
                errorMessage = null
            )
        }
    }

    fun onDraftNameChange(name: String) {
        _localState.update { it.copy(draftName = name) }
    }

    fun onDraftMuscleGroupChange(group: String) {
        _localState.update { it.copy(draftMuscleGroup = group) }
    }

    fun onDraftNotesChange(notes: String) {
        _localState.update { it.copy(draftNotes = notes) }
    }

    fun saveExercise(onSuccess: () -> Unit = {}) {
        val current = _localState.value
        val name = current.draftName.trim()
        val group = current.draftMuscleGroup
        val notes = current.draftNotes.trim()

        if (name.isBlank()) {
            _localState.update { it.copy(errorMessage = "Lütfen hareket adını girin.") }
            return
        }

        viewModelScope.launch {
            _localState.update { it.copy(isSaving = true) }
            try {
                if (current.editingExerciseId != null) {
                    repository.updateExercise(
                        ExerciseEntity(
                            id = current.editingExerciseId,
                            name = name,
                            muscleGroup = group,
                            isCustom = true,
                            notes = notes
                        )
                    )
                } else {
                    repository.addCustomExercise(name, group, notes)
                }
                closeAddEditDialog()
                onSuccess()
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = "Hareket kaydedilirken hata oluştu: ${e.localizedMessage}") }
            } finally {
                _localState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun requestDeleteExercise(exercise: ExerciseEntity) {
        _localState.update { it.copy(exerciseToDelete = exercise) }
    }

    fun cancelDeleteExercise() {
        _localState.update { it.copy(exerciseToDelete = null) }
    }

    fun deleteExercise(onSuccess: () -> Unit = {}) {
        val toDelete = _localState.value.exerciseToDelete ?: return
        viewModelScope.launch {
            try {
                repository.deleteExercise(toDelete.id)
                cancelDeleteExercise()
                onSuccess()
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = "Silinirken hata oluştu: ${e.localizedMessage}") }
            }
        }
    }
}
