package com.example.feature.nowplaying

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.common.ShareUtils
import com.example.core.designsystem.DeleteConfirmDialog
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlowingPlayButton
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SpectrumVisualizerCanvas
import com.example.core.designsystem.SpinningVinylArtwork
import com.example.core.designsystem.localizedAlbum
import com.example.core.designsystem.localizedArtist
import com.example.domain.model.PlaybackState
import com.example.domain.model.PlaybackStatus
import com.example.domain.model.RepeatModeOption
import com.example.domain.model.Song
import com.example.feature.common.RenameSongDialog
import kotlin.math.abs

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    spectrum: FloatArray,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onRetry: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onPickSongBackground: (Long, Uri) -> Unit = { _, _ -> },
    onClearSongBackground: (Long) -> Unit = {},
    onRenameSong: (Long, String) -> Unit = { _, _ -> },
    onDeleteSong: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val tokens = LocalKarlukTokens.current
    val density = LocalDensity.current
    val song = playbackState.currentSong
    var speedMenuOpen by remember { mutableStateOf(false) }
    var bgMenuOpen by remember { mutableStateOf(false) }
    var pencilMenuOpen by remember { mutableStateOf(false) }
    var pendingSongIdForBg by remember { mutableStateOf<Long?>(null) }
    var renameTarget by remember { mutableStateOf<Song?>(null) }
    var deleteTarget by remember { mutableStateOf<Song?>(null) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    val songBgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val targetId = pendingSongIdForBg ?: song?.id
        pendingSongIdForBg = null
        if (uri != null && targetId != null) {
            onPickSongBackground(targetId, uri)
        }
    }

    KarlukBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeOffsetX = 0f },
                        onDragCancel = { swipeOffsetX = 0f },
                        onDragEnd = {
                            if (abs(swipeOffsetX) >= swipeThresholdPx) {
                                swipeOffsetX = 0f
                                onBack()
                            } else {
                                swipeOffsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-260f, 260f)
                        }
                    )
                }
                .graphicsLayer { translationX = swipeOffsetX * 0.45f },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(
                    title = stringResource(R.string.now_playing_title),
                    onBack = onBack,
                    actions = {
                        Box {
                            TextButton(
                                onClick = { speedMenuOpen = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("now_playing_speed_btn")
                            ) {
                                Icon(Icons.Rounded.Speed, contentDescription = null, tint = tokens.accentPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${playbackState.playbackSpeed}x", color = tokens.textPrimary)
                            }
                            DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    DropdownMenuItem(
                                        text = { Text("${spd}x") },
                                        onClick = {
                                            onChangeSpeed(spd)
                                            speedMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        if (song != null) {
                            IconButton(
                                onClick = { ShareUtils.shareSong(context, song) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("now_playing_share_btn")
                            ) {
                                Icon(
                                    Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.action_share),
                                    tint = tokens.textPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenQueue,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_queue_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.queue_title), tint = tokens.textPrimary)
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpinningVinylArtwork(
                            artworkUri = song?.artworkUri,
                            isPlaying = playbackState.isPlaying,
                            size = 228.dp,
                            modifier = Modifier.align(Alignment.Center),
                            customBackgroundUri = song?.customBackgroundUri
                        )

                        if (song != null) {
                            val hasSongBg = !song.customBackgroundUri.isNullOrBlank()
                            Box(
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Surface(
                                    onClick = {
                                        if (hasSongBg) {
                                            bgMenuOpen = true
                                        } else {
                                            pendingSongIdForBg = song.id
                                            songBgPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (hasSongBg) {
                                        tokens.accentPrimary.copy(alpha = 0.22f)
                                    } else {
                                        tokens.glassSurfaceElevated
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (hasSongBg) tokens.accentPrimary else tokens.glassBorder
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(48.dp)
                                        .testTag("now_playing_custom_bg_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (hasSongBg) Icons.Rounded.Wallpaper else Icons.Rounded.AddPhotoAlternate,
                                            contentDescription = stringResource(R.string.now_playing_custom_bg),
                                            tint = if (hasSongBg) tokens.accentPrimary else tokens.textPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = bgMenuOpen,
                                    onDismissRequest = { bgMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.now_playing_custom_bg_change)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = tokens.accentPrimary
                                            )
                                        },
                                        onClick = {
                                            bgMenuOpen = false
                                            pendingSongIdForBg = song.id
                                            songBgPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.now_playing_custom_bg_reset),
                                                color = Color(0xFFFF6E8A)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFFF6E8A)
                                            )
                                        },
                                        onClick = {
                                            bgMenuOpen = false
                                            onClearSongBackground(song.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SpectrumVisualizerCanvas(
                        spectrum = spectrum,
                        isPlaying = playbackState.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title + Artist + Favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (song != null) FormatBadge(song.formatLabel)
                                Text(
                                    text = song?.title ?: stringResource(R.string.home_no_track),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = song?.let { "${it.localizedArtist()} • ${it.localizedAlbum()}" }
                                    ?: stringResource(R.string.home_tap_to_scan),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (song != null) {
                            Box {
                                IconButton(
                                    onClick = { pencilMenuOpen = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("now_playing_rename_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.rename_nasheed_title),
                                        tint = tokens.accentPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = pencilMenuOpen,
                                    onDismissRequest = { pencilMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename_nasheed_title)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = null,
                                                tint = tokens.accentPrimary
                                            )
                                        },
                                        onClick = {
                                            pencilMenuOpen = false
                                            renameTarget = song
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.action_delete),
                                                color = Color(0xFFFF5E7E)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFFF5E7E)
                                            )
                                        },
                                        onClick = {
                                            pencilMenuOpen = false
                                            deleteTarget = song
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onToggleFavorite(song.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("now_playing_fav_btn")
                            ) {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (song.isFavorite) Color(0xFFFF4D7D) else tokens.textSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Error banner if playback error occurs
                    if (playbackState.status == PlaybackStatus.ERROR && playbackState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            highlighted = true
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = playbackState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRetry) {
                                    Text(stringResource(R.string.action_retry), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Seek slider & playback controls stay LTR in all locales
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        val duration = playbackState.durationMs.coerceAtLeast(1L)
                        var isSeeking by remember { mutableStateOf(false) }
                        var seekFraction by remember { mutableFloatStateOf(0f) }
                        val liveFraction = (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                        Slider(
                            value = if (isSeeking) seekFraction else liveFraction,
                            onValueChange = {
                                isSeeking = true
                                seekFraction = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                onSeekTo((seekFraction * duration).toLong())
                            },
                            enabled = song != null,
                            colors = SliderDefaults.colors(
                                thumbColor = tokens.accentPrimary,
                                activeTrackColor = tokens.accentPrimary,
                                inactiveTrackColor = tokens.glassBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("now_playing_seek")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val shownPos = if (isSeeking) (seekFraction * duration).toLong() else playbackState.positionMs
                            Text(FormatUtils.formatDuration(shownPos), style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                            Text(FormatUtils.formatDuration(playbackState.durationMs), style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Primary playback controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (playbackState.shuffleEnabled) tokens.accentPrimary else tokens.textSecondary
                                )
                            }
                            IconButton(onClick = onSkipPrev, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = tokens.textPrimary, modifier = Modifier.size(36.dp))
                            }
                            GlowingPlayButton(
                                isPlaying = playbackState.isPlaying,
                                onClick = onTogglePlayPause,
                                size = 72.dp,
                                testTag = "now_playing_play_pause"
                            )
                            IconButton(onClick = onSkipNext, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = tokens.textPrimary, modifier = Modifier.size(36.dp))
                            }
                            IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                                val icon = if (playbackState.repeatMode == RepeatModeOption.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                                Icon(
                                    icon,
                                    contentDescription = "Repeat",
                                    tint = if (playbackState.repeatMode != RepeatModeOption.OFF) tokens.accentPrimary else tokens.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Tools Row: Lyrics, Equalizer, Queue, Sleep Timer
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ToolActionButton(Icons.Rounded.Lyrics, stringResource(R.string.lyrics_title), onOpenLyrics, "now_playing_lyrics_btn")
                            ToolActionButton(Icons.Rounded.Equalizer, stringResource(R.string.home_quick_equalizer), onOpenEqualizer, "now_playing_eq_btn")
                            ToolActionButton(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.queue_title), onOpenQueue, "now_playing_queue_tool_btn")
                            ToolActionButton(Icons.Rounded.Timer, stringResource(R.string.sleep_timer_title), onOpenSleepTimer, "now_playing_sleep_btn")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameSongDialog(
            song = target,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle -> onRenameSong(target.id, newTitle) }
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            itemTitle = target.title,
            onConfirm = {
                deleteTarget = null
                onDeleteSong(target.id)
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ToolActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val tokens = LocalKarlukTokens.current
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tokens.accentPrimary)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
