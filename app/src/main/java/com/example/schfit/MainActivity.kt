package com.example.schfit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.schfit.data.repository.SchFitRepository
import com.example.schfit.ui.navigation.AppNavigation
import com.example.schfit.ui.theme.SchFitTheme
import com.example.schfit.ui.viewmodel.ViewModelFactory
import com.example.schfit.util.LocaleHelper
import com.example.schfit.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("schfit_global_preferences", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "system") ?: "system"
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val repository = SchFitRepository(
            context = applicationContext
        )
        val viewModelFactory = ViewModelFactory(repository)

        setContent {
            val themeMode by repository.getThemeModeFlow().collectAsState(initial = repository.getThemeMode())
            val currentLanguage by repository.getAppLanguageFlow().collectAsState(initial = repository.getAppLanguage())

            val locale = remember(currentLanguage) {
                LocaleHelper.getLocale(currentLanguage)
            }
            val baseConfiguration = LocalConfiguration.current
            val localizedConfiguration = remember(baseConfiguration, currentLanguage) {
                Configuration(baseConfiguration).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setLocales(LocaleList(locale))
                    } else {
                        @Suppress("DEPRECATION")
                        this.locale = locale
                    }
                    setLayoutDirection(locale)
                }
            }

            LaunchedEffect(currentLanguage) {
                LocaleHelper.updateResourcesLocale(this@MainActivity, currentLanguage)
                LocaleHelper.updateResourcesLocale(applicationContext, currentLanguage)
            }

            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(
                LocalContext provides this@MainActivity,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                SchFitTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(viewModelFactory = viewModelFactory)
                    }
                }
            }
        }
    }
}