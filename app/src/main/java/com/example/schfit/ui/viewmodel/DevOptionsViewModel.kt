package com.example.schfit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schfit.data.repository.ConfigModel
import com.example.schfit.data.repository.SchFitRepository
import com.example.schfit.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DevOptionsUiState(
    val activeConfigId: String = "default",
    val availableConfigs: List<ConfigModel> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalExercises: Int = 0,
    val totalMetrics: Int = 0,
    val totalRoutines: Int = 0,
    val isDeveloperModeEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null
)

private data class DevCountsIntermediate(
    val totalWorkouts: Int,
    val totalVolumeKg: Double,
    val totalExercises: Int,
    val totalMetrics: Int
)

class DevOptionsViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isProcessing = MutableStateFlow(false)

    val uiState: StateFlow<DevOptionsUiState> = combine(
        combine(
            repository.getTotalWorkoutsCount(),
            repository.getTotalVolumeFlow(),
            repository.getAllExercises(),
            repository.getAllMetrics()
        ) { workouts, vol, exercises, metrics ->
            DevCountsIntermediate(
                totalWorkouts = workouts,
                totalVolumeKg = vol,
                totalExercises = exercises.size,
                totalMetrics = metrics.size
            )
        },
        combine(
            repository.getAllRoutines(),
            repository.getIsDeveloperModeFlow(),
            repository.getActiveConfigFlow(),
            repository.getAvailableConfigsFlow()
        ) { routines, isDev, activeCfg, cfgs ->
            Tuple4(routines, isDev, activeCfg, cfgs)
        },
        _isProcessing,
        _statusMessage
    ) { intermediate, (routines, isDev, activeCfg, cfgs), processing, msg ->
        DevOptionsUiState(
            activeConfigId = activeCfg,
            availableConfigs = cfgs,
            totalWorkouts = intermediate.totalWorkouts,
            totalVolumeKg = intermediate.totalVolumeKg,
            totalExercises = intermediate.totalExercises,
            totalMetrics = intermediate.totalMetrics,
            totalRoutines = routines.size,
            isDeveloperModeEnabled = isDev,
            isProcessing = processing,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DevOptionsUiState(
            activeConfigId = repository.getActiveConfigId(),
            availableConfigs = repository.getAvailableConfigs()
        )
    )

    fun switchConfig(configId: String) {
        repository.switchConfig(configId)
        _statusMessage.value = "Aktif config değiştirildi: $configId"
    }

    fun seedMockData() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Test verileri yükleniyor..."
            try {
                repository.seedMockDataForDev()
                _statusMessage.value = "12 antrenman, PR'lar, kilo geçmişi ve 15+ ton hacim başarıyla yüklendi! 🚀"
            } catch (e: Exception) {
                _statusMessage.value = "Hata oluştu: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun flushAllData() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Veritabanı temizleniyor..."
            try {
                repository.flushAllDatabaseForDev()
                _statusMessage.value = "Aktif config'deki tüm antrenman, kilo ve program verileri sıfırlandı. 🧹"
            } catch (e: Exception) {
                _statusMessage.value = "Hata oluştu: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun disableDeveloperMode(onDisabled: () -> Unit) {
        repository.disableDeveloperMode()
        onDisabled()
    }

    fun sendTestNotification(context: Context) {
        NotificationHelper.sendRestTimerCompletedNotification(
            context = context,
            exerciseName = "Bench Press",
            setNumber = 3,
            isVibrationEnabled = true
        )
        _statusMessage.value = "Test bildirimi gönderildi! 🔔"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
