package com.example.ui

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.BuildConfig
import com.example.R
import com.example.localization.LocaleHelper
import com.example.data.database.ExcludedFolderEntity
import com.example.data.database.SafFolderEntity
import com.example.data.database.TrashItemEntity
import com.example.data.update.AppUpdateChecker
import com.example.domain.model.AccentOption
import com.example.domain.model.Album
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.Artist
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.FolderItem
import com.example.domain.model.LyricsData
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.StorageStats
import com.example.domain.model.TextColorOption
import com.example.domain.model.ThemeAtmosphere
import com.example.domain.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.musicRepository
    private val prefs = container.preferencesRepository
    private val player = container.playbackController
    private val effects = container.audioEffectsManager
    private val audioProcessor = container.audioProcessor

    val userSettings: StateFlow<UserSettings> = prefs.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, prefs.getInitialSettingsSync())

    val allSongs: StateFlow<List<Song>> = repo.allSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMusicSongs: StateFlow<List<Song>> = repo.filteredMusicSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repo.favoriteSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historySongs: StateFlow<List<Song>> = repo.historySongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = repo.albumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = repo.artistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderItem>> = repo.foldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repo.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val safFolders: StateFlow<List<SafFolderEntity>> = repo.safFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = repo.excludedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashItems: StateFlow<List<TrashItemEntity>> = repo.trashItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = repo.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storageStats: StateFlow<StorageStats> = repo.storageStatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageStats())

    val playbackState: StateFlow<PlaybackState> = player.playbackState
    val audioEffectsState: StateFlow<AudioEffectsState> = effects.state
    val spectrumFlow: StateFlow<FloatArray> = audioProcessor.spectrumFlow

    val currentLyrics: StateFlow<LyricsData?> = playbackState
        .map { it.currentSong?.id }
        .distinctUntilChanged()
        .flatMapLatest { songId ->
            if (songId == null) flowOf(null) else repo.observeLyrics(songId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _updateCheckFailed = MutableStateFlow(false)
    val updateCheckFailed: StateFlow<Boolean> = _updateCheckFailed.asStateFlow()

    private val _pendingDeleteRequest = MutableStateFlow<Pair<IntentSender, List<TrashItemEntity>>?>(null)
    val pendingDeleteRequest: StateFlow<Pair<IntentSender, List<TrashItemEntity>>?> = _pendingDeleteRequest.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var initialSetupDone = false

    init {
        player.connect()

        // Keep player queue metadata (like favorite heart state) synced with Room
        viewModelScope.launch {
            allSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    player.updateSongsMetadata(songs)
                }
            }
        }

        // Apply saved Equalizer & DSP preferences immediately on launch
        viewModelScope.launch {
            val saved = prefs.equalizerPrefsFlow.first()
            effects.applySavedPrefs(saved)
        }

        // Restore last played Nasheed immediately and also re-check after optional startup scan
        viewModelScope.launch {
            if (initialSetupDone) return@launch
            initialSetupDone = true
            val settings = prefs.userSettingsFlow.first()
            restoreLastPlaybackIfNeeded(settings)
            if (settings.onboardingCompleted && settings.autoScanOnStartup) {
                runScanInternal(silent = true)
                restoreLastPlaybackIfNeeded(settings)
            }
            checkForUpdates(manual = false)
        }
    }

    private suspend fun restoreLastPlaybackIfNeeded(settings: UserSettings) {
        if (!settings.resumePlaybackOnLaunch || player.playbackState.value.currentSong != null) return
        val targetSongId = settings.lastSongId.takeIf { it != -1L }
            ?: prefs.getLastSongIdSync().takeIf { it != -1L }
            ?: return
        val targetPos = if (settings.lastPositionMs > 0L) settings.lastPositionMs else prefs.getLastPositionMsSync()

        val librarySongs = repo.allSongsFlow.first()
        val libraryMap = librarySongs.associateBy { it.id }
        val savedQueueIds = prefs.getLastQueueIdsSync()
        val savedQueue = savedQueueIds.mapNotNull { libraryMap[it] }

        val targetQueue: List<Song>
        val targetIdx: Int
        when {
            savedQueue.any { it.id == targetSongId } -> {
                targetQueue = savedQueue
                targetIdx = savedQueue.indexOfFirst { it.id == targetSongId }
            }
            librarySongs.any { it.id == targetSongId } -> {
                targetQueue = librarySongs
                targetIdx = librarySongs.indexOfFirst { it.id == targetSongId }
            }
            else -> {
                val singleSong = repo.getSongById(targetSongId) ?: return
                targetQueue = listOf(singleSong)
                targetIdx = 0
            }
        }

        if (targetIdx >= 0) {
            player.playSongs(
                songs = targetQueue,
                startIndex = targetIdx,
                autoPlay = false,
                startPositionMs = targetPos
            )
        }
    }

    fun rescanMedia() {
        viewModelScope.launch {
            runScanInternal(silent = false)
        }
    }

    private suspend fun runScanInternal(silent: Boolean) {
        if (_isScanning.value) return
        _isScanning.value = true
        try {
            val count = repo.rescanAllMedia()
            if (!silent) {
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                _events.tryEmit(localizedCtx.getString(R.string.home_tracks_count, count))
            }
        } finally {
            _isScanning.value = false
        }
    }

    fun addSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val added = repo.addSafFolder(treeUri)
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                _events.tryEmit("+" + localizedCtx.getString(R.string.home_tracks_count, added))
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun removeSafFolder(treeUri: String) {
        viewModelScope.launch {
            runCatching {
                val removedIds = withContext(Dispatchers.IO) { repo.removeSafFolder(treeUri) }
                if (removedIds.isNotEmpty()) {
                    player.removeSongsFromQueueByIds(removedIds.toSet())
                }
            }
        }
    }

    fun removeAllSafFolders() {
        val current = safFolders.value
        if (current.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val removedIds = withContext(Dispatchers.IO) {
                    current.flatMap { repo.removeSafFolder(it.treeUri) }.toSet()
                }
                if (removedIds.isNotEmpty()) {
                    player.removeSongsFromQueueByIds(removedIds)
                }
            }
        }
    }

    fun importAudioFiles(uris: List<Uri>, playImmediately: Boolean = true) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val outcome = repo.importAudioFiles(uris)
                if (outcome.songs.isNotEmpty() && playImmediately) {
                    player.playSongs(outcome.songs, 0, autoPlay = true)
                }
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                when {
                    outcome.songs.isNotEmpty() && outcome.skippedMusicCount > 0 -> {
                        _events.tryEmit(
                            "+${outcome.songs.size} • " +
                                localizedCtx.getString(R.string.music_filter_skipped_notice, outcome.skippedMusicCount)
                        )
                    }
                    outcome.songs.isNotEmpty() -> {
                        _events.tryEmit("+${outcome.songs.size}")
                    }
                    outcome.skippedMusicCount > 0 -> {
                        _events.tryEmit(
                            localizedCtx.getString(R.string.music_filter_skipped_notice, outcome.skippedMusicCount)
                        )
                    }
                }
            } catch (_: Throwable) {
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun playExternalUri(uri: Uri) {
        viewModelScope.launch {
            val song = runCatching { repo.importSingleExternalUri(uri) }.getOrNull()
            if (song == null) {
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                _events.tryEmit(localizedCtx.getString(R.string.music_filter_skipped_notice, 1))
                return@launch
            }
            player.playSongs(listOf(song), 0, autoPlay = true)
        }
    }

    // Playback actions
    fun playSongs(songs: List<Song>, startIndex: Int = 0) = player.playSongs(songs, startIndex, autoPlay = true)
    fun shuffleAndPlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        player.playSongs(shuffled, 0, autoPlay = true)
    }

    fun togglePlayPause() = player.togglePlayPause()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun skipToNext() = player.skipToNext()
    fun skipToPrevious() = player.skipToPrevious()
    fun skipToQueueIndex(index: Int) = player.skipToQueueIndex(index)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeatMode() = player.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)
    fun addToQueue(songs: List<Song>) = player.addToQueue(songs)
    fun playNext(song: Song) = player.playNext(song)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = player.moveQueueItem(fromIndex, toIndex)
    fun removeQueueItem(index: Int) = player.removeQueueItem(index)
    fun clearQueue() = player.clearQueue()
    fun retryPlayback() = player.retryPlayback()

    // Sleep timer
    fun setSleepTimerMinutes(minutes: Int) = player.setSleepTimerMinutes(minutes)
    fun setSleepAfterCurrentTrack() = player.setSleepAfterCurrentTrack()
    fun cancelSleepTimer() = player.cancelSleepTimer()

    // Favorites, Playlists, History, Lyrics, Search
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.toggleFavorite(songId)
        }
    }

    fun addSongsToFavorites(songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setFavorites(songIds, true)
        }
    }

    fun createPlaylist(name: String, initialSongIds: List<Long> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.createPlaylist(name, initialSongIds)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deletePlaylist(playlistId)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repo.clearHistory() }
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                _events.tryEmit(localizedCtx.getString(R.string.history_removed_notice))
            }
        }
    }

    fun removeSongFromHistory(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeSongFromHistory(songId)
            val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
            _events.tryEmit(localizedCtx.getString(R.string.history_removed_notice))
        }
    }

    fun removeSongsFromHistory(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeSongsFromHistory(songIds)
            val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
            _events.tryEmit(localizedCtx.getString(R.string.history_removed_notice))
        }
    }

    fun saveLyrics(songId: Long, rawContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveLyrics(songId, rawContent)
        }
    }

    fun importLrcFile(songId: Long, lrcUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.importLrcFileForSong(songId, lrcUri)
        }
    }

    fun saveSearchQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveSearchQuery(query)
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearSearchHistory()
        }
    }

    fun excludeFolder(folderPath: String, folderName: String) {
        viewModelScope.launch {
            runCatching {
                val removedIds = withContext(Dispatchers.IO) {
                    repo.excludeFolder(folderPath, folderName)
                }
                if (removedIds.isNotEmpty()) {
                    player.removeSongsFromQueueByIds(removedIds.toSet())
                }
            }
        }
    }

    fun restoreFolder(folderPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.restoreFolder(folderPath) }
        }
    }

    fun approveFilteredSongsAsNasheed(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repo.approveFilteredSongsAsNasheed(songIds)
                }
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                _events.tryEmit(localizedCtx.getString(R.string.filtered_music_moved_notice))
            }
        }
    }

    fun removeSongFromLibrary(songId: Long) {
        removeSongsFromLibrary(listOf(songId))
    }

    fun removeSongsFromLibrary(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val distinctIds = songIds.distinct()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repo.removeSongsFromLibrary(distinctIds)
                }
                player.removeSongsFromQueueByIds(distinctIds.toSet())
            }
        }
    }

    fun moveRecentSong(songId: Long, delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.moveRecentSong(songId, delta) }
        }
    }

    fun restoreTrashItem(item: TrashItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repo.restoreTrashItem(item)
                val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
                _events.tryEmit(localizedCtx.getString(R.string.trash_restored_notice))
            }
        }
    }

    fun requestDeleteTrashItem(item: TrashItemEntity) {
        requestDeleteTrashItems(listOf(item))
    }

    fun requestEmptyTrash() {
        val currentItems = trashItems.value
        if (currentItems.isNotEmpty()) {
            requestDeleteTrashItems(currentItems)
        }
    }

    private fun requestDeleteTrashItems(items: List<TrashItemEntity>) {
        if (items.isEmpty() || _pendingDeleteRequest.value != null) return
        viewModelScope.launch {
            runCatching {
                val plan = withContext(Dispatchers.IO) { repo.preparePermanentDelete(items) }
                if (plan.intentSender != null) {
                    _pendingDeleteRequest.value = plan.intentSender to plan.itemsToFinalize
                }
            }.onFailure {
                runCatching {
                    withContext(Dispatchers.IO) { repo.finalizePermanentDelete(items) }
                }
            }
        }
    }

    fun onSystemDeleteResult(granted: Boolean) {
        val pending = _pendingDeleteRequest.value
        _pendingDeleteRequest.value = null
        if (granted && pending != null) {
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { repo.finalizePermanentDelete(pending.second) }
                }
            }
        }
    }

    fun renameSong(songId: Long, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.renameSong(songId, newTitle)
        }
    }

    fun setSongCustomBackground(songId: Long, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveSongBackgroundFromUri(songId, uri)
        }
    }

    fun clearSongCustomBackground(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearSongBackground(songId)
        }
    }

    fun clearArtworkCache() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearArtworkCache()
            val localizedCtx = LocaleHelper.wrapContext(container.appContext, userSettings.value.language)
            _events.tryEmit(localizedCtx.getString(R.string.storage_cache_cleared))
        }
    }

    fun openUpdateDialogAndCheck() {
        _showUpdateDialog.value = true
        checkForUpdates(manual = true, openDialog = true)
    }

    fun checkForUpdates(manual: Boolean = true, openDialog: Boolean = manual) {
        if (_isCheckingUpdate.value) return
        if (openDialog) {
            _showUpdateDialog.value = true
        }
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateCheckFailed.value = false
            try {
                val result = AppUpdateChecker.checkTelegramChannelForUpdate(BuildConfig.VERSION_NAME)
                val info = result.getOrNull()
                if (info != null) {
                    _updateInfo.value = info
                    if (info.isUpdateAvailable) {
                        _showUpdateDialog.value = true
                    }
                } else {
                    _updateCheckFailed.value = true
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateBanner() {
        _showUpdateDialog.value = false
    }

    // Equalizer & Audio Effects
    fun setEqualizerEnabled(enabled: Boolean) {
        effects.setEqualizerEnabled(enabled)
        persistEffects()
    }

    fun setEqualizerBand(bandIndex: Int, levelMb: Short) {
        effects.setBandLevel(bandIndex, levelMb)
        persistEffects()
    }

    fun applyEqualizerPreset(preset: EqualizerPreset) {
        effects.applyPreset(preset)
        persistEffects()
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        effects.setBassBoost(enabled, strength)
        persistEffects()
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        effects.setVirtualizer(enabled, strength)
        persistEffects()
    }

    fun setLoudnessEnhancer(enabled: Boolean, gainMb: Int) {
        effects.setLoudnessEnhancer(enabled, gainMb)
        persistEffects()
    }

    fun setBalance(balance: Float) {
        audioProcessor.balance = balance
        effects.setBalance(balance)
        persistEffects()
    }

    private fun persistEffects() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.saveEqualizerPrefs(effects.toSavedPrefs())
        }
    }

    // Settings
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.setOnboardingCompletedSync(completed)
        viewModelScope.launch { prefs.setOnboardingCompleted(completed) }
    }

    fun setLanguage(language: AppLanguage, onApplied: () -> Unit = {}) {
        viewModelScope.launch {
            prefs.setLanguage(language)
            onApplied()
        }
    }

    fun setThemeAtmosphere(atmosphere: ThemeAtmosphere) {
        viewModelScope.launch { prefs.setThemeAtmosphere(atmosphere) }
    }

    fun setAccentOption(accent: AccentOption) {
        viewModelScope.launch { prefs.setAccentOption(accent) }
    }

    fun setTextColorOption(option: TextColorOption) {
        viewModelScope.launch { prefs.setTextColorOption(option) }
    }

    fun setCustomTextColorArgb(argb: Long) {
        viewModelScope.launch { prefs.setCustomTextColorArgb(argb) }
    }

    fun setGlassOpacity(opacity: Float) {
        viewModelScope.launch { prefs.setGlassOpacity(opacity) }
    }

    fun setReducedMotion(reduced: Boolean) {
        viewModelScope.launch { prefs.setReducedMotion(reduced) }
    }

    fun setLowPowerMode(lowPower: Boolean) {
        viewModelScope.launch { prefs.setLowPowerMode(lowPower) }
    }

    fun setResumePlayback(resume: Boolean) {
        viewModelScope.launch { prefs.setResumePlayback(resume) }
    }

    fun setGaplessPlayback(gapless: Boolean) {
        viewModelScope.launch { prefs.setGaplessPlayback(gapless) }
    }

    fun setAutoScanOnStartup(autoScan: Boolean) {
        viewModelScope.launch { prefs.setAutoScanOnStartup(autoScan) }
    }

    fun setCustomBackground(uri: Uri) {
        viewModelScope.launch { prefs.saveCustomBackgroundFromUri(uri) }
    }

    fun clearCustomBackground() {
        viewModelScope.launch { prefs.clearCustomBackground() }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
