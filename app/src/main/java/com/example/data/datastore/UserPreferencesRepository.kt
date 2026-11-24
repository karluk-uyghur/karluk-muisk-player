package com.example.data.datastore

import android.content.Context
import android.net.Uri
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
import com.example.domain.model.RepeatModeOption
import com.example.domain.model.TextColorOption
import com.example.domain.model.ThemeAtmosphere
import com.example.domain.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_welcome_v3_done")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_ATMOSPHERE = stringPreferencesKey("theme_atmosphere")
        val ACCENT_OPTION = stringPreferencesKey("accent_option")
        val TEXT_COLOR_OPTION = stringPreferencesKey("text_color_option")
        val CUSTOM_TEXT_COLOR_ARGB = longPreferencesKey("custom_text_color_argb")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val AUTO_SCAN = booleanPreferencesKey("auto_scan")
        val LAST_SONG_ID = longPreferencesKey("last_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val CUSTOM_BACKGROUND_URI = stringPreferencesKey("custom_background_uri")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")

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
        val onboardingDone = prefs[Keys.ONBOARDING_DONE]
            ?: localeMirrorPrefs.getBoolean("onboarding_welcome_v3_done", false)
        val dsSongId = prefs[Keys.LAST_SONG_ID] ?: -1L
        val mirrorSongId = localeMirrorPrefs.getLong("last_song_id", -1L)
        val resolvedSongId = if (dsSongId != -1L) dsSongId else mirrorSongId
        val resolvedPosMs = if (dsSongId != -1L) {
            prefs[Keys.LAST_POSITION_MS] ?: localeMirrorPrefs.getLong("last_position_ms", 0L)
        } else {
            localeMirrorPrefs.getLong("last_position_ms", 0L)
        }
        UserSettings(
            onboardingCompleted = onboardingDone,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: getSavedLanguageSync().code),
            themeAtmosphere = ThemeAtmosphere.fromId(
                prefs[Keys.THEME_ATMOSPHERE]
                    ?: localeMirrorPrefs.getString("theme_atmosphere", ThemeAtmosphere.DEEP_NAVY.id)
                    ?: ThemeAtmosphere.DEEP_NAVY.id
            ),
            accentOption = AccentOption.fromId(
                prefs[Keys.ACCENT_OPTION]
                    ?: localeMirrorPrefs.getString("accent_option", AccentOption.CYAN.id)
                    ?: AccentOption.CYAN.id
            ),
            textColorOption = TextColorOption.fromId(
                prefs[Keys.TEXT_COLOR_OPTION]
                    ?: localeMirrorPrefs.getString("text_color_option", TextColorOption.AUTO.id)
                    ?: TextColorOption.AUTO.id
            ),
            customTextColorArgb = prefs[Keys.CUSTOM_TEXT_COLOR_ARGB]
                ?: localeMirrorPrefs.getLong("custom_text_color_argb", 0L),
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.14f,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            lowPowerMode = prefs[Keys.LOW_POWER_MODE] ?: false,
            resumePlaybackOnLaunch = prefs[Keys.RESUME_PLAYBACK]
                ?: localeMirrorPrefs.getBoolean("resume_playback", true),
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK]
                ?: localeMirrorPrefs.getBoolean("gapless_playback", true),
            autoScanOnStartup = prefs[Keys.AUTO_SCAN]
                ?: localeMirrorPrefs.getBoolean("auto_scan", true),
            lastSongId = resolvedSongId,
            lastPositionMs = resolvedPosMs.coerceAtLeast(0L),
            customBackgroundUri = prefs[Keys.CUSTOM_BACKGROUND_URI]?.takeIf { it.isNotBlank() }
                ?: localeMirrorPrefs.getString("custom_background_uri", null)?.takeIf { it.isNotBlank() },
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED]
                ?: localeMirrorPrefs.getBoolean("shuffle_enabled", false),
            repeatMode = RepeatModeOption.fromName(
                prefs[Keys.REPEAT_MODE]
                    ?: localeMirrorPrefs.getString("repeat_mode", RepeatModeOption.OFF.name)
                    ?: RepeatModeOption.OFF.name
            )
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

    fun getOnboardingCompletedSync(): Boolean {
        return localeMirrorPrefs.getBoolean("onboarding_welcome_v3_done", false)
    }

    fun getShuffleEnabledSync(): Boolean {
        return localeMirrorPrefs.getBoolean("shuffle_enabled", false)
    }

    fun getRepeatModeSync(): RepeatModeOption {
        val raw = localeMirrorPrefs.getString("repeat_mode", RepeatModeOption.OFF.name) ?: RepeatModeOption.OFF.name
        return RepeatModeOption.fromName(raw)
    }

    fun getTextColorOptionSync(): TextColorOption {
        val raw = localeMirrorPrefs.getString("text_color_option", TextColorOption.AUTO.id) ?: TextColorOption.AUTO.id
        return TextColorOption.fromId(raw)
    }

    fun getCustomTextColorArgbSync(): Long = localeMirrorPrefs.getLong("custom_text_color_argb", 0L)

    fun getLastSongIdSync(): Long = localeMirrorPrefs.getLong("last_song_id", -1L)

    fun getLastPositionMsSync(): Long = localeMirrorPrefs.getLong("last_position_ms", 0L).coerceAtLeast(0L)

    fun getLastQueueIdsSync(): List<Long> {
        val raw = localeMirrorPrefs.getString("last_queue_ids", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun getResumePlaybackSync(): Boolean = localeMirrorPrefs.getBoolean("resume_playback", true)

    fun getInitialSettingsSync(): UserSettings {
        return UserSettings(
            onboardingCompleted = getOnboardingCompletedSync(),
            language = getSavedLanguageSync(),
            themeAtmosphere = ThemeAtmosphere.fromId(
                localeMirrorPrefs.getString("theme_atmosphere", ThemeAtmosphere.DEEP_NAVY.id)
                    ?: ThemeAtmosphere.DEEP_NAVY.id
            ),
            accentOption = AccentOption.fromId(
                localeMirrorPrefs.getString("accent_option", AccentOption.CYAN.id) ?: AccentOption.CYAN.id
            ),
            textColorOption = getTextColorOptionSync(),
            customTextColorArgb = getCustomTextColorArgbSync(),
            resumePlaybackOnLaunch = getResumePlaybackSync(),
            gaplessPlayback = localeMirrorPrefs.getBoolean("gapless_playback", true),
            autoScanOnStartup = localeMirrorPrefs.getBoolean("auto_scan", true),
            lastSongId = getLastSongIdSync(),
            lastPositionMs = getLastPositionMsSync(),
            customBackgroundUri = localeMirrorPrefs.getString("custom_background_uri", null)?.takeIf { it.isNotBlank() },
            shuffleEnabled = getShuffleEnabledSync(),
            repeatMode = getRepeatModeSync()
        )
    }

    fun setOnboardingCompletedSync(completed: Boolean) {
        localeMirrorPrefs.edit().putBoolean("onboarding_welcome_v3_done", completed).apply()
    }

    suspend fun setLanguage(language: AppLanguage) {
        localeMirrorPrefs.edit().putString("lang_code", language.code).apply()
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        setOnboardingCompletedSync(completed)
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun setThemeAtmosphere(atmosphere: ThemeAtmosphere) {
        localeMirrorPrefs.edit().putString("theme_atmosphere", atmosphere.id).apply()
        context.dataStore.edit { it[Keys.THEME_ATMOSPHERE] = atmosphere.id }
    }

    suspend fun setAccentOption(accent: AccentOption) {
        localeMirrorPrefs.edit().putString("accent_option", accent.id).apply()
        context.dataStore.edit { it[Keys.ACCENT_OPTION] = accent.id }
    }

    suspend fun setTextColorOption(option: TextColorOption) {
        localeMirrorPrefs.edit()
            .putString("text_color_option", option.id)
            .putLong("custom_text_color_argb", 0L)
            .apply()
        context.dataStore.edit {
            it[Keys.TEXT_COLOR_OPTION] = option.id
            it[Keys.CUSTOM_TEXT_COLOR_ARGB] = 0L
        }
    }

    suspend fun setCustomTextColorArgb(argb: Long) {
        localeMirrorPrefs.edit().putLong("custom_text_color_argb", argb).apply()
        context.dataStore.edit { it[Keys.CUSTOM_TEXT_COLOR_ARGB] = argb }
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
        localeMirrorPrefs.edit().putBoolean("resume_playback", resume).apply()
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = resume }
    }

    suspend fun setGaplessPlayback(gapless: Boolean) {
        localeMirrorPrefs.edit().putBoolean("gapless_playback", gapless).apply()
        context.dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = gapless }
    }

    suspend fun setAutoScanOnStartup(autoScan: Boolean) {
        localeMirrorPrefs.edit().putBoolean("auto_scan", autoScan).apply()
        context.dataStore.edit { it[Keys.AUTO_SCAN] = autoScan }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        localeMirrorPrefs.edit().putBoolean("shuffle_enabled", enabled).apply()
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun setRepeatMode(mode: RepeatModeOption) {
        localeMirrorPrefs.edit().putString("repeat_mode", mode.name).apply()
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
    }

    suspend fun saveCustomBackgroundFromUri(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        val bgDir = File(context.filesDir, "backgrounds").apply { if (!exists()) mkdirs() }
        val destFile = File(bgDir, "bg_${System.currentTimeMillis()}.jpg")
        val copied = try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } != null
        } catch (_: Exception) {
            false
        }
        if (!copied || !destFile.exists() || destFile.length() <= 0L) {
            destFile.delete()
            return@withContext null
        }
        bgDir.listFiles()?.forEach { file ->
            if (file.absolutePath != destFile.absolutePath) {
                file.delete()
            }
        }
        val savedUri = Uri.fromFile(destFile).toString()
        localeMirrorPrefs.edit().putString("custom_background_uri", savedUri).apply()
        context.dataStore.edit { it[Keys.CUSTOM_BACKGROUND_URI] = savedUri }
        savedUri
    }

    suspend fun clearCustomBackground() = withContext(Dispatchers.IO) {
        val bgDir = File(context.filesDir, "backgrounds")
        if (bgDir.exists()) {
            bgDir.listFiles()?.forEach { it.delete() }
        }
        localeMirrorPrefs.edit().remove("custom_background_uri").apply()
        context.dataStore.edit { it.remove(Keys.CUSTOM_BACKGROUND_URI) }
    }

    fun saveLastPlaybackSync(
        songId: Long,
        positionMs: Long,
        queueIds: List<Long> = emptyList(),
        commit: Boolean = false
    ) {
        val safePos = positionMs.coerceAtLeast(0L)
        val editor = localeMirrorPrefs.edit()
            .putLong("last_song_id", songId)
            .putLong("last_position_ms", safePos)
        if (queueIds.isNotEmpty()) {
            editor.putString("last_queue_ids", queueIds.joinToString(","))
        }
        if (commit) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    suspend fun saveLastPlayback(songId: Long, positionMs: Long, queueIds: List<Long> = emptyList()) {
        val safePos = positionMs.coerceAtLeast(0L)
        saveLastPlaybackSync(songId, safePos, queueIds, commit = false)
        context.dataStore.edit {
            it[Keys.LAST_SONG_ID] = songId
            it[Keys.LAST_POSITION_MS] = safePos
            if (queueIds.isNotEmpty()) {
                it[Keys.LAST_QUEUE_IDS] = queueIds.joinToString(",")
            }
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
