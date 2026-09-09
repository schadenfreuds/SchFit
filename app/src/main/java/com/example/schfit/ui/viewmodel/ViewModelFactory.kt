package com.example.schfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.schfit.data.repository.SchFitRepository

class ViewModelFactory(
    private val repository: SchFitRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(WorkoutViewModel::class.java) -> {
                WorkoutViewModel(repository) as T
            }
            modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java) -> {
                WorkoutSessionViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProgressViewModel::class.java) -> {
                ProgressViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ExerciseLibraryViewModel::class.java) -> {
                ExerciseLibraryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            modelClass.isAssignableFrom(DevOptionsViewModel::class.java) -> {
                DevOptionsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
