package com.example.schfit.ui.viewmodel

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThemeOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: String
)

data class SoundOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: String
)

data class LanguageOption(
    val id: String,
    val title: String,
    val nativeName: String,
    val flagEmoji: String
)

data class SettingsUiState(
    val selectedTheme: String = "system",
    val selectedSound: String = "beep",
    val selectedLanguage: String = "system",
    val isVibrationEnabled: Boolean = true,
    val isKeepScreenOnEnabled: Boolean = true,
    val isDeveloperModeEnabled: Boolean = false,
    val activeConfigId: String = "default",
    val availableConfigs: List<ConfigModel> = emptyList(),
    val isNewConfigDialogOpen: Boolean = false,
    val newConfigName: String = "",
    val newConfigDescription: String = "",
    val isResetting: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val resetSuccessMessage: String? = null
)

private data class SettingsLocalState(
    val selectedTheme: String = "system",
    val selectedSound: String = "beep",
    val selectedLanguage: String = "system",
    val isVibrationEnabled: Boolean = true,
    val isKeepScreenOnEnabled: Boolean = true,
    val isNewConfigDialogOpen: Boolean = false,
    val newConfigName: String = "",
    val newConfigDescription: String = "",
    val isResetting: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val resetSuccessMessage: String? = null
)

class SettingsViewModel(
    private val repository: SchFitRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(
        SettingsLocalState(
            selectedTheme = repository.getThemeMode(),
            selectedSound = repository.getSoundType(),
            selectedLanguage = repository.getAppLanguage(),
            isVibrationEnabled = repository.isVibrationEnabled(),
            isKeepScreenOnEnabled = repository.isKeepScreenOnEnabled()
        )
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            repository.getIsDeveloperModeFlow(),
            repository.getActiveConfigFlow(),
            repository.getAvailableConfigsFlow()
        ) { isDev, activeCfg, cfgs ->
            Triple(isDev, activeCfg, cfgs)
        },
        _localState
    ) { (isDev, activeCfg, cfgs), local ->
        SettingsUiState(
            selectedTheme = local.selectedTheme,
            selectedSound = local.selectedSound,
            selectedLanguage = local.selectedLanguage,
            isVibrationEnabled = local.isVibrationEnabled,
            isKeepScreenOnEnabled = local.isKeepScreenOnEnabled,
            isDeveloperModeEnabled = isDev,
            activeConfigId = activeCfg,
            availableConfigs = cfgs,
            isNewConfigDialogOpen = local.isNewConfigDialogOpen,
            newConfigName = local.newConfigName,
            newConfigDescription = local.newConfigDescription,
            isResetting = local.isResetting,
            showResetConfirmDialog = local.showResetConfirmDialog,
            resetSuccessMessage = local.resetSuccessMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            selectedTheme = repository.getThemeMode(),
            selectedSound = repository.getSoundType(),
            selectedLanguage = repository.getAppLanguage(),
            isVibrationEnabled = repository.isVibrationEnabled(),
            isKeepScreenOnEnabled = repository.isKeepScreenOnEnabled(),
            isDeveloperModeEnabled = repository.isDeveloperMode(),
            activeConfigId = repository.getActiveConfigId(),
            availableConfigs = repository.getAvailableConfigs()
        )
    )

    val availableThemes = listOf(
        ThemeOption("system", "Sistem Varsayılanı", "Cihazın sistem temasını otomatik takip eder", "📱"),
        ThemeOption("dark", "Karanlık Tema", "Göz yormayan koyu renkli şık görünüm", "🌙"),
        ThemeOption("light", "Aydınlık Tema", "Ferah ve açık renkli görünüm", "☀️")
    )

    val availableLanguages = listOf(
        LanguageOption("tr", "Türkçe", "Türkçe", "🇹🇷"),
        LanguageOption("en", "English", "English", "🇬🇧"),
        LanguageOption("de", "Deutsch", "Deutsch", "🇩🇪"),
        LanguageOption("es", "Español", "Español", "🇪🇸"),
        LanguageOption("it", "Italiano", "Italiano", "🇮🇹")
    )

    val availableSounds = listOf(
        SoundOption("beep", "Klasik Bip (Varsayılan)", "Standart çift tonlu kısa bildirim sesi", "⚡"),
        SoundOption("bell", "Boks Gongu / Zil", "Ring gongu tarzında motive edici ton", "🔔"),
        SoundOption("chime", "Dijital Alarm / Chime", "Yumuşak ve net onay sesi", "🔊"),
        SoundOption("whistle", "Düdük Sesi", "Antrenör düdüğü tarzı yüksek ton", "📢"),
        SoundOption("silent", "Sessiz", "Yalnızca görsel ve titreşimli bildirim", "🔕")
    )

    fun setThemeMode(themeId: String) {
        repository.setThemeMode(themeId)
        _localState.update { it.copy(selectedTheme = themeId) }
    }

    fun setLanguage(langId: String, context: android.content.Context? = null) {
        repository.setAppLanguage(langId)
        _localState.update { it.copy(selectedLanguage = langId) }
        context?.let {
            com.example.schfit.util.LocaleHelper.updateResourcesLocale(it.applicationContext, langId)
        }
    }

    fun setSoundType(soundId: String, context: android.content.Context? = null) {
        repository.setSoundType(soundId)
        _localState.update { it.copy(selectedSound = soundId) }
        previewSound(soundId, context)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        repository.setVibrationEnabled(enabled)
        _localState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        repository.setKeepScreenOnEnabled(enabled)
        _localState.update { it.copy(isKeepScreenOnEnabled = enabled) }
    }

    fun enableDeveloperMode() {
        repository.enableDeveloperMode()
    }

    // --- Config Actions ---
    fun switchConfig(configId: String) {
        repository.switchConfig(configId)
    }

    fun openNewConfigDialog() {
        _localState.update {
            it.copy(
                isNewConfigDialogOpen = true,
                newConfigName = "",
                newConfigDescription = ""
            )
        }
    }

    fun closeNewConfigDialog() {
        _localState.update { it.copy(isNewConfigDialogOpen = false) }
    }

    fun onNewConfigNameChange(name: String) {
        _localState.update { it.copy(newConfigName = name) }
    }

    fun onNewConfigDescriptionChange(desc: String) {
        _localState.update { it.copy(newConfigDescription = desc) }
    }

    fun createNewConfig() {
        val name = _localState.value.newConfigName.trim()
        val desc = _localState.value.newConfigDescription.trim()
        if (name.isNotBlank()) {
            repository.createConfig(name, desc)
            closeNewConfigDialog()
        }
    }

    fun deleteConfig(configId: String) {
        repository.deleteConfig(configId)
    }

    fun previewSound(soundId: String, context: android.content.Context? = null) {
        NotificationHelper.playSoundByType(soundId, context)
    }

    fun openResetDialog() {
        _localState.update { it.copy(showResetConfirmDialog = true, resetSuccessMessage = null) }
    }

    fun closeResetDialog() {
        _localState.update { it.copy(showResetConfirmDialog = false) }
    }

    fun resetAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _localState.update { it.copy(isResetting = true, showResetConfirmDialog = false) }
            try {
                repository.resetAllUserData()
                _localState.update { it.copy(isResetting = false, resetSuccessMessage = "Aktif config verileri başarıyla sıfırlandı.") }
                onComplete()
            } catch (_: Exception) {
                _localState.update { it.copy(isResetting = false) }
            }
        }
    }
}
