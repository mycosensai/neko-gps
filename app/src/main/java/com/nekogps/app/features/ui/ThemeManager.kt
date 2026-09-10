package com.nekogps.app.features.ui

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.uiThemeDataStore by preferencesDataStore(name = "ui_theme_prefs")

/** Available app themes. */
enum class AppTheme(val id: String, val displayName: String, val styleRes: Int) {
    SUPERHUMAN_DARK("superhuman_dark", "Superhuman Dark", com.nekogps.app.R.style.Theme_NekoGps),
    LIGHT("light", "Light", com.nekogps.app.R.style.Theme_NekoGps_Light),
    HIGH_CONTRAST("high_contrast", "High Contrast", com.nekogps.app.R.style.Theme_NekoGps_HighContrast),
    AMOLED("amoled", "AMOLED Black", com.nekogps.app.R.style.Theme_NekoGps_Amoled),
    SOLARIZED("solarized", "Solarized", com.nekogps.app.R.style.Theme_NekoGps_Solarized);

    companion object {
        fun fromId(id: String?): AppTheme = values().firstOrNull { it.id == id } ?: SUPERHUMAN_DARK
    }
}

/**
 * Manages app theme selection, persisted in DataStore and applied dynamically.
 * Call [applyStoredTheme] before super.onCreate / setContentView, and [setTheme]
 * to change at runtime (recreates the activity).
 */
class ThemeManager(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        @Volatile private var cached: AppTheme? = null

        fun cachedTheme(): AppTheme = cached ?: AppTheme.SUPERHUMAN_DARK
    }

    val themeFlow: Flow<AppTheme> = context.uiThemeDataStore.data
        .map { prefs -> AppTheme.fromId(prefs[THEME_KEY]) }

    suspend fun setTheme(theme: AppTheme) {
        cached = theme
        context.uiThemeDataStore.edit { it[THEME_KEY] = theme.id }
        applyNightMode(theme)
    }

    suspend fun currentTheme(): AppTheme {
        val id: String? = context.uiThemeDataStore.data.map { it[THEME_KEY] }.first()
        return AppTheme.fromId(id).also { cached = it }
    }

    /** Apply the stored theme to an activity. Must be called before setContentView. */
    fun applyStoredTheme(activity: Activity) {
        val theme = cached ?: AppTheme.SUPERHUMAN_DARK
        activity.setTheme(theme.styleRes)
        applyNightMode(theme)
    }

    private fun applyNightMode(theme: AppTheme) {
        val mode = when (theme) {
            AppTheme.LIGHT, AppTheme.SOLARIZED -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
