package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val mimeType: String,
    val formatLabel: String,
    val artworkUri: String?,
    val fileSize: Long,
    val dateAdded: Long,
    val trackNumber: Int?,
    val genre: String?,
    val folderPath: String,
    val folderName: String,
    val isSafTrack: Boolean = false,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val customBackgroundUri: String? = null,
    val isFilteredMusic: Boolean = false,
    val isUserApprovedNasheed: Boolean = false
)

data class Album(
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val totalDuration: Long
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkUri: String?
)

data class FolderItem(
    val path: String,
    val name: String,
    val songCount: Int,
    val isSaf: Boolean
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songs: List<Song> = emptyList()
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsData(
    val songId: Long,
    val rawContent: String,
    val lines: List<LyricLine>,
    val isSynced: Boolean
)

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class RepeatModeOption {
    OFF,
    ALL,
    ONE;

    companion object {
        fun fromName(name: String): RepeatModeOption =
            entries.firstOrNull { it.name == name } ?: OFF
    }
}

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playbackSpeed: Float = 1.0f,
    val audioSessionId: Int = 0,
    val errorMessage: String? = null,
    val sleepTimerRemainingMs: Long = 0L,
    val sleepAfterCurrentTrack: Boolean = false
)

data class AudioBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMb: Short
)

enum class EqualizerPreset(val id: String) {
    FLAT("flat"),
    CLASSICAL("classical"),
    POP("pop"),
    ROCK("rock"),
    JAZZ("jazz"),
    HIP_HOP("hiphop"),
    VOCAL("vocal"),
    CUSTOM("custom")
}

data class AudioEffectsState(
    val isEqualizerSupported: Boolean = false,
    val isEqualizerEnabled: Boolean = false,
    val bands: List<AudioBand> = emptyList(),
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500,
    val activePreset: EqualizerPreset = EqualizerPreset.FLAT,
    val isBassBoostSupported: Boolean = false,
    val isBassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val isVirtualizerSupported: Boolean = false,
    val isVirtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0, // 0..1000
    val isLoudnessSupported: Boolean = false,
    val isLoudnessEnabled: Boolean = false,
    val loudnessGainMb: Int = 0, // 0..1500
    val balance: Float = 0f // -1.0 (Left) .. +1.0 (Right)
)

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    UYGHUR("ug", "ئۇيغۇرچە", true),
    ARABIC("ar", "العربية", true),
    ENGLISH("en", "English", false),
    TURKISH("tr", "Türkçe", false);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: UYGHUR
    }
}

enum class ThemeAtmosphere(val id: String) {
    DEEP_NAVY("deep_navy"),
    MIDNIGHT_VIOLET("midnight_violet"),
    ROYAL_OCEAN("royal_ocean"),
    SILVER_FROST("silver_frost");

    companion object {
        fun fromId(id: String): ThemeAtmosphere =
            entries.firstOrNull { it.id == id } ?: DEEP_NAVY
    }
}

enum class AccentOption(val id: String) {
    CYAN("cyan"),
    ROYAL_BLUE("royal_blue"),
    VIOLET("violet"),
    EMERALD("emerald");

    companion object {
        fun fromId(id: String): AccentOption =
            entries.firstOrNull { it.id == id } ?: CYAN
    }
}

enum class TextColorOption(val id: String) {
    AUTO("auto"),
    PURE_WHITE("pure_white"),
    SKY_CYAN("sky_cyan"),
    GOLD_AMBER("gold_amber"),
    EMERALD_MINT("emerald_mint"),
    ROSE_CORAL("rose_coral"),
    OBSIDIAN_BLACK("obsidian_black");

    companion object {
        fun fromId(id: String): TextColorOption =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val language: AppLanguage = AppLanguage.UYGHUR,
    val themeAtmosphere: ThemeAtmosphere = ThemeAtmosphere.DEEP_NAVY,
    val accentOption: AccentOption = AccentOption.CYAN,
    val textColorOption: TextColorOption = TextColorOption.AUTO,
    val customTextColorArgb: Long = 0L,
    val glassOpacity: Float = 0.14f,
    val reducedMotion: Boolean = false,
    val lowPowerMode: Boolean = false,
    val resumePlaybackOnLaunch: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val autoScanOnStartup: Boolean = true,
    val lastSongId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val customBackgroundUri: String? = null,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF
)

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)

enum class SortOption {
    TITLE,
    ARTIST,
    DATE_ADDED,
    DURATION
}

data class StorageStats(
    val totalTracks: Int = 0,
    val totalSizeBytes: Long = 0L,
    val artworkCacheBytes: Long = 0L,
    val formatCounts: Map<String, Int> = emptyMap(),
    val safFoldersCount: Int = 0
)
