package com.example.playback.controller

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.repository.MusicRepository
import com.example.domain.model.PlaybackState
import com.example.domain.model.PlaybackStatus
import com.example.domain.model.RepeatModeOption
import com.example.domain.model.Song
import com.example.playback.service.KarlukPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackController(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastRecordedSongId: Long = -1L

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val songMap = LinkedHashMap<Long, Song>()
    private var pendingAction: ((MediaController) -> Unit)? = null

    fun connect() {
        if (mediaController != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, KarlukPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                attachListener(controller)
                syncFromPlayer(controller)
                pendingAction?.invoke(controller)
                pendingAction = null
            } catch (_: Exception) {
                controllerFuture = null
            }
        }, MoreExecutors.directExecutor())
    }

    private fun withController(block: (MediaController) -> Unit) {
        val current = mediaController
        if (current != null) {
            block(current)
        } else {
            pendingAction = block
            connect()
        }
    }

    private fun attachListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && _playbackState.value.sleepAfterCurrentTrack) {
                    player.pause()
                    cancelSleepTimer()
                }
                syncFromPlayer(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startProgressLoop() else stopProgressLoop()
                syncFromPlayer(player)
                if (!isPlaying) persistCurrentPosition(player)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (_playbackState.value.sleepAfterCurrentTrack &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) {
                    player.pause()
                    cancelSleepTimer()
                }
                syncFromPlayer(player)
                val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
                if (songId != lastRecordedSongId) {
                    lastRecordedSongId = songId
                    scope.launch(Dispatchers.IO) {
                        musicRepository.recordPlayHistory(songId)
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                syncFromPlayer(player)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                syncFromPlayer(player)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                syncFromPlayer(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.update {
                    it.copy(
                        status = PlaybackStatus.ERROR,
                        isPlaying = false,
                        errorMessage = error.localizedMessage ?: "Unsupported or inaccessible audio file"
                    )
                }
            }
        })
    }

    private fun syncFromPlayer(player: Player) {
        val count = player.mediaItemCount
        val queue = (0 until count).mapNotNull { idx ->
            val id = player.getMediaItemAt(idx).mediaId.toLongOrNull()
            id?.let { songMap[it] }
        }
        val currentIdx = player.currentMediaItemIndex.takeIf { it in queue.indices } ?: -1
        val currentSong = player.currentMediaItem?.mediaId?.toLongOrNull()?.let { songMap[it] }
            ?: queue.getOrNull(currentIdx)

        val status = when (player.playbackState) {
            Player.STATE_IDLE -> if (_playbackState.value.status == PlaybackStatus.ERROR) PlaybackStatus.ERROR else PlaybackStatus.IDLE
            Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            Player.STATE_READY -> if (player.isPlaying) PlaybackStatus.PLAYING else if (currentSong != null) PlaybackStatus.PAUSED else PlaybackStatus.READY
            Player.STATE_ENDED -> PlaybackStatus.ENDED
            else -> PlaybackStatus.IDLE
        }

        val repeatOption = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
            Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
            else -> RepeatModeOption.OFF
        }

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                queue = queue,
                currentIndex = currentIdx,
                status = status,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { d -> d > 0 } ?: (currentSong?.duration ?: 0L),
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = repeatOption,
                playbackSpeed = player.playbackParameters.speed,
                errorMessage = if (status != PlaybackStatus.ERROR) null else it.errorMessage
            )
        }
    }

    private fun startProgressLoop() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaController
                if (player != null && player.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun persistCurrentPosition(player: Player) {
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        scope.launch(Dispatchers.IO) {
            preferencesRepository.saveLastPlayback(songId, pos)
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true, startPositionMs: Long = 0L) {
        if (songs.isEmpty()) return
        songs.forEach { songMap[it.id] = it }
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { it.toMediaItem() }

        withController { player ->
            player.setMediaItems(mediaItems, safeIndex, startPositionMs.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = autoPlay
            _playbackState.update { it.copy(errorMessage = null) }
            syncFromPlayer(player)
            if (autoPlay) {
                val selected = songs[safeIndex]
                lastRecordedSongId = selected.id
                scope.launch(Dispatchers.IO) {
                    musicRepository.recordPlayHistory(selected.id)
                }
            }
        }
    }

    fun updateSongsMetadata(songs: List<Song>) {
        songs.forEach { songMap[it.id] = it }
        _playbackState.update { state ->
            val updatedCurrent = state.currentSong?.let { songMap[it.id] ?: it }
            val updatedQueue = state.queue.map { songMap[it.id] ?: it }
            state.copy(currentSong = updatedCurrent, queue = updatedQueue)
        }
    }

    fun togglePlayPause() {
        withController { player ->
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
                player.prepare()
            }
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0, 0L)
                }
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        withController { player ->
            val clamped = positionMs.coerceAtLeast(0L)
            player.seekTo(clamped)
            _playbackState.update { it.copy(positionMs = clamped) }
        }
    }

    fun skipToNext() {
        withController { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.play()
            } else if (player.mediaItemCount > 0) {
                player.seekTo(0, 0L)
                player.play()
            }
        }
    }

    fun skipToPrevious() {
        withController { player ->
            if (player.currentPosition > 3500L) {
                player.seekTo(0L)
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                player.play()
            } else {
                player.seekTo(0L)
            }
        }
    }

    fun skipToQueueIndex(index: Int) {
        withController { player ->
            if (index in 0 until player.mediaItemCount) {
                player.seekTo(index, 0L)
                player.play()
            }
        }
    }

    fun toggleShuffle() {
        withController { player ->
            player.shuffleModeEnabled = !player.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        withController { player ->
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        withController { player ->
            player.setPlaybackParameters(PlaybackParameters(clamped))
        }
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        songs.forEach { songMap[it.id] = it }
        withController { player ->
            if (player.mediaItemCount == 0) {
                playSongs(songs, 0, autoPlay = true)
            } else {
                player.addMediaItems(songs.map { it.toMediaItem() })
                syncFromPlayer(player)
            }
        }
    }

    fun playNext(song: Song) {
        songMap[song.id] = song
        withController { player ->
            if (player.mediaItemCount == 0) {
                playSongs(listOf(song), 0, autoPlay = true)
            } else {
                val insertIdx = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
                player.addMediaItem(insertIdx, song.toMediaItem())
                syncFromPlayer(player)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        withController { player ->
            if (fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount && fromIndex != toIndex) {
                player.moveMediaItem(fromIndex, toIndex)
                syncFromPlayer(player)
            }
        }
    }

    fun removeQueueItem(index: Int) {
        withController { player ->
            if (index in 0 until player.mediaItemCount) {
                player.removeMediaItem(index)
                syncFromPlayer(player)
            }
        }
    }

    fun clearQueue() {
        withController { player ->
            player.clearMediaItems()
            player.stop()
            syncFromPlayer(player)
        }
    }

    fun retryPlayback() {
        withController { player ->
            _playbackState.update { it.copy(errorMessage = null) }
            player.prepare()
            player.play()
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val totalMs = minutes * 60_000L
        _playbackState.update {
            it.copy(sleepTimerRemainingMs = totalMs, sleepAfterCurrentTrack = false)
        }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0 && isActive) {
                delay(1_000L)
                remaining -= 1_000L
                _playbackState.update { it.copy(sleepTimerRemainingMs = remaining.coerceAtLeast(0L)) }
            }
            if (remaining <= 0L) {
                mediaController?.pause()
                _playbackState.update { it.copy(sleepTimerRemainingMs = 0L) }
            }
        }
    }

    fun setSleepAfterCurrentTrack() {
        sleepTimerJob?.cancel()
        _playbackState.update {
            it.copy(sleepTimerRemainingMs = 0L, sleepAfterCurrentTrack = true)
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playbackState.update {
            it.copy(sleepTimerRemainingMs = 0L, sleepAfterCurrentTrack = false)
        }
    }

    fun release() {
        stopProgressLoop()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    private fun Song.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putString("format", formatLabel)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
    }
}
