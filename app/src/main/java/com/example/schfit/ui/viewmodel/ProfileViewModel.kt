package com.example.schfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.repository.SchFitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilePrModel(
    val exerciseName: String,
    val muscleGroup: String,
    val maxWeightKg: Double,
    val maxReps: Int
)

data class ProfileAchievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val progressText: String
)

data class ProfileUiState(
    val userName: String = "Can",
    val userTitle: String = "SchFit Sporcusu",
    val memberSince: String = "Ağustos 2026",
    val totalWorkoutsCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalDurationMinutes: Int = 0,
    val currentWeightKg: Double? = null,
    val heightCm: Int = 185,
    val fitnessGoal: String = "Hipertrofi & Kas Kazanımı",
    val weeklyGoal: Int = 4,
    val topPrs: List<ProfilePrModel> = emptyList(),
    val achievements: List<ProfileAchievement> = emptyList(),
    val isEditProfileDialogOpen: Boolean = false
) {
    val totalDurationFormatted: String
        get() {
            val hours = totalDurationMinutes / 60
            val minutes = totalDurationMinutes % 60
            return if (hours > 0) "$hours sa $minutes dk" else "$minutes dk"
        }

    val totalVolumeFormatted: String
        get() {
            return if (totalVolumeKg >= 1000.0) {
                "%.1f Ton".format(totalVolumeKg / 1000.0)
            } else {
                "%.0f kg".format(totalVolumeKg)
            }
        }
}

private data class ProfileLocalState(
    val userName: String = "Can",
    val heightCm: Int = 185,
    val fitnessGoal: String = "Hipertrofi & Kas Kazanımı",
    val topPrs: List<ProfilePrModel> = emptyList(),
    val totalDurationMinutes: Int = 0,
    val isEditProfileDialogOpen: Boolean = false
)

class ProfileViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(
        ProfileLocalState(
            userName = repository.getUserName(),
            heightCm = repository.getUserHeight(),
            fitnessGoal = repository.getUserGoal()
        )
    )

    init {
        loadPrsAndWorkoutStats()
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.getTotalWorkoutsCount(),
        repository.getTotalVolumeFlow(),
        repository.getLatestMetricFlow(),
        _localState
    ) { workoutCount, totalVol, latestMetric, local ->
        val achievements = calculateAchievements(workoutCount, totalVol, local.topPrs.size)

        ProfileUiState(
            userName = local.userName,
            userTitle = when {
                workoutCount >= 50 -> "Efsanevi Sporcu 👑"
                workoutCount >= 20 -> "İleri Seviye Atlet 🏆"
                workoutCount >= 5 -> "Gelişen Sporcu 🔥"
                else -> "SchFit Sporcusu ⚡"
            },
            memberSince = "Ağustos 2026",
            totalWorkoutsCount = workoutCount,
            totalVolumeKg = totalVol,
            totalDurationMinutes = local.totalDurationMinutes,
            currentWeightKg = latestMetric?.weightKg,
            heightCm = local.heightCm,
            fitnessGoal = local.fitnessGoal,
            weeklyGoal = repository.getWeeklyWorkoutGoal(),
            topPrs = local.topPrs,
            achievements = achievements,
            isEditProfileDialogOpen = local.isEditProfileDialogOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun loadPrsAndWorkoutStats() {
        viewModelScope.launch {
            repository.getAllWorkoutLogs().collect { logs ->
                val totalMinutes = logs.sumOf { it.durationMinutes }

                // Collect PRs across all exercises
                repository.getAllExercises().collect { exercises ->
                    val prList = mutableListOf<ProfilePrModel>()
                    for (exercise in exercises) {
                        val sets = repository.getAllSetLogsForExercise(exercise.id)
                        if (sets.isNotEmpty()) {
                            val maxSet = sets.maxByOrNull { it.weightKg }
                            if (maxSet != null && maxSet.weightKg > 0) {
                                prList.add(
                                    ProfilePrModel(
                                        exerciseName = exercise.name,
                                        muscleGroup = exercise.muscleGroup,
                                        maxWeightKg = maxSet.weightKg,
                                        maxReps = maxSet.reps
                                    )
                                )
                            }
                        }
                    }
                    val sortedPrs = prList.sortedByDescending { it.maxWeightKg }
                    _localState.update {
                        it.copy(
                            totalDurationMinutes = totalMinutes,
                            topPrs = sortedPrs
                        )
                    }
                }
            }
        }
    }

    private fun calculateAchievements(
        workoutCount: Int,
        totalVolumeKg: Double,
        prsCount: Int
    ): List<ProfileAchievement> {
        return listOf(
            ProfileAchievement(
                id = "first_workout",
                title = "İlk Adım",
                description = "İlk antrenmanını başarıyla tamamla",
                iconEmoji = "🥇",
                isUnlocked = workoutCount >= 1,
                progressText = if (workoutCount >= 1) "Tamamlandı ✓" else "0 / 1 Antrenman"
            ),
            ProfileAchievement(
                id = "one_ton_club",
                title = "1 Ton Kulübü",
                description = "Toplam 1.000 kg ağırlık kaldır",
                iconEmoji = "⚡",
                isUnlocked = totalVolumeKg >= 1000.0,
                progressText = if (totalVolumeKg >= 1000.0) "Tamamlandı ✓" else "%.0f / 1.000 kg".format(totalVolumeKg)
            ),
            ProfileAchievement(
                id = "ten_ton_club",
                title = "10 Ton Kulübü",
                description = "Toplam 10.000 kg ağırlık kaldır",
                iconEmoji = "💎",
                isUnlocked = totalVolumeKg >= 10000.0,
                progressText = if (totalVolumeKg >= 10000.0) "Tamamlandı ✓" else "%.0f / 10.000 kg".format(totalVolumeKg)
            ),
            ProfileAchievement(
                id = "iron_discipline",
                title = "Demir Disiplin",
                description = "En az 5 antrenman seansını tamamla",
                iconEmoji = "🛡️",
                isUnlocked = workoutCount >= 5,
                progressText = if (workoutCount >= 5) "Tamamlandı ✓" else "$workoutCount / 5 Antrenman"
            ),
            ProfileAchievement(
                id = "record_breaker",
                title = "Rekor Avcısı",
                description = "Farklı hareketlerde kişisel rekorlar kır",
                iconEmoji = "👑",
                isUnlocked = prsCount >= 3,
                progressText = if (prsCount >= 3) "Tamamlandı ✓ ($prsCount PR)" else "$prsCount / 3 Rekor"
            ),
            ProfileAchievement(
                id = "centurion",
                title = "Usta Sporcu",
                description = "25 tamamlanmış antrenman seansına ulaş",
                iconEmoji = "🏆",
                isUnlocked = workoutCount >= 25,
                progressText = if (workoutCount >= 25) "Tamamlandı ✓" else "$workoutCount / 25 Antrenman"
            )
        )
    }

    fun openEditProfileDialog() {
        _localState.update { it.copy(isEditProfileDialogOpen = true) }
    }

    fun closeEditProfileDialog() {
        _localState.update { it.copy(isEditProfileDialogOpen = false) }
    }

    fun saveProfile(name: String, height: Int, goal: String) {
        val trimmedName = name.trim().ifBlank { "Can" }
        repository.setUserName(trimmedName)
        repository.setUserHeight(height)
        repository.setUserGoal(goal)
        _localState.update {
            it.copy(
                userName = trimmedName,
                heightCm = height,
                fitnessGoal = goal,
                isEditProfileDialogOpen = false
            )
        }
    }
}
