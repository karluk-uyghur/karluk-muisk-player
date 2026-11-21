package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccentOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.ThemeAtmosphere
import com.example.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "karluk_preferences")

data class SavedEqualizerPrefs(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val bandLevels: List<Short> = emptyList(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,
    val loudnessEnabled: Boolean = false,
    val loudnessGainMb: Int = 0,
    val balance: Float = 0f
)

class UserPreferencesRepository(private val context: Context) {

    private val localeMirrorPrefs by lazy {
        context.getSharedPreferences("karluk_locale_mirror", Context.MODE_PRIVATE)
    }

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_ATMOSPHERE = stringPreferencesKey("theme_atmosphere")
        val ACCENT_OPTION = stringPreferencesKey("accent_option")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val AUTO_SCAN = booleanPreferencesKey("auto_scan")
        val LAST_SONG_ID = longPreferencesKey("last_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")

        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val BASS_ENABLED = booleanPreferencesKey("bass_enabled")
        val BASS_STRENGTH = intPreferencesKey("bass_strength")
        val VIRT_ENABLED = booleanPreferencesKey("virt_enabled")
        val VIRT_STRENGTH = intPreferencesKey("virt_strength")
        val LOUD_ENABLED = booleanPreferencesKey("loud_enabled")
        val LOUD_GAIN = intPreferencesKey("loud_gain")
        val BALANCE = floatPreferencesKey("balance")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.UYGHUR.code),
            themeAtmosphere = ThemeAtmosphere.fromId(prefs[Keys.THEME_ATMOSPHERE] ?: ThemeAtmosphere.DEEP_NAVY.id),
            accentOption = AccentOption.fromId(prefs[Keys.ACCENT_OPTION] ?: AccentOption.CYAN.id),
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.14f,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            lowPowerMode = prefs[Keys.LOW_POWER_MODE] ?: false,
            resumePlaybackOnLaunch = prefs[Keys.RESUME_PLAYBACK] ?: true,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            autoScanOnStartup = prefs[Keys.AUTO_SCAN] ?: true,
            lastSongId = prefs[Keys.LAST_SONG_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L
        )
    }

    val equalizerPrefsFlow: Flow<SavedEqualizerPrefs> = context.dataStore.data.map { prefs ->
        val rawBands = prefs[Keys.EQ_BANDS].orEmpty()
        val parsedBands = if (rawBands.isBlank()) {
            emptyList()
        } else {
            rawBands.split(",").mapNotNull { it.trim().toShortOrNull() }
        }
        val presetId = prefs[Keys.EQ_PRESET] ?: EqualizerPreset.FLAT.id
        SavedEqualizerPrefs(
            enabled = prefs[Keys.EQ_ENABLED] ?: false,
            preset = EqualizerPreset.entries.firstOrNull { it.id == presetId } ?: EqualizerPreset.FLAT,
            bandLevels = parsedBands,
            bassBoostEnabled = prefs[Keys.BASS_ENABLED] ?: false,
            bassBoostStrength = prefs[Keys.BASS_STRENGTH] ?: 0,
            virtualizerEnabled = prefs[Keys.VIRT_ENABLED] ?: false,
            virtualizerStrength = prefs[Keys.VIRT_STRENGTH] ?: 0,
            loudnessEnabled = prefs[Keys.LOUD_ENABLED] ?: false,
            loudnessGainMb = prefs[Keys.LOUD_GAIN] ?: 0,
            balance = prefs[Keys.BALANCE] ?: 0f
        )
    }

    fun getSavedLanguageSync(): AppLanguage {
        val code = localeMirrorPrefs.getString("lang_code", AppLanguage.UYGHUR.code) ?: AppLanguage.UYGHUR.code
        return AppLanguage.fromCode(code)
    }

    suspend fun setLanguage(language: AppLanguage) {
        localeMirrorPrefs.edit().putString("lang_code", language.code).apply()
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setThemeAtmosphere(atmosphere: ThemeAtmosphere) {
        context.dataStore.edit { it[Keys.THEME_ATMOSPHERE] = atmosphere.id }
    }

    suspend fun setAccentOption(accent: AccentOption) {
        context.dataStore.edit { it[Keys.ACCENT_OPTION] = accent.id }
    }

    suspend fun setGlassOpacity(opacity: Float) {
        context.dataStore.edit { it[Keys.GLASS_OPACITY] = opacity.coerceIn(0.06f, 0.32f) }
    }

    suspend fun setReducedMotion(reduced: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = reduced }
    }

    suspend fun setLowPowerMode(lowPower: Boolean) {
        context.dataStore.edit { it[Keys.LOW_POWER_MODE] = lowPower }
    }

    suspend fun setResumePlayback(resume: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = resume }
    }

    suspend fun setGaplessPlayback(gapless: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = gapless }
    }

    suspend fun setAutoScanOnStartup(autoScan: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SCAN] = autoScan }
    }

    suspend fun saveLastPlayback(songId: Long, positionMs: Long) {
        context.dataStore.edit {
            it[Keys.LAST_SONG_ID] = songId
            it[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun saveEqualizerPrefs(prefs: SavedEqualizerPrefs) {
        context.dataStore.edit {
            it[Keys.EQ_ENABLED] = prefs.enabled
            it[Keys.EQ_PRESET] = prefs.preset.id
            it[Keys.EQ_BANDS] = prefs.bandLevels.joinToString(",")
            it[Keys.BASS_ENABLED] = prefs.bassBoostEnabled
            it[Keys.BASS_STRENGTH] = prefs.bassBoostStrength
            it[Keys.VIRT_ENABLED] = prefs.virtualizerEnabled
            it[Keys.VIRT_STRENGTH] = prefs.virtualizerStrength
            it[Keys.LOUD_ENABLED] = prefs.loudnessEnabled
            it[Keys.LOUD_GAIN] = prefs.loudnessGainMb
            it[Keys.BALANCE] = prefs.balance
        }
    }
}
