package com.example.feature.nowplaying

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlowingPlayButton
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SpectrumVisualizerCanvas
import com.example.core.designsystem.SpinningVinylArtwork
import com.example.domain.model.PlaybackState
import com.example.domain.model.PlaybackStatus
import com.example.domain.model.RepeatModeOption

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
    onOpenSleepTimer: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val song = playbackState.currentSong
    var speedMenuOpen by remember { mutableStateOf(false) }

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
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

                    SpinningVinylArtwork(
                        artworkUri = song?.artworkUri,
                        isPlaying = playbackState.isPlaying,
                        size = 228.dp
                    )

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
                                text = song?.let { "${it.artist} • ${it.album}" } ?: stringResource(R.string.home_tap_to_scan),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (song != null) {
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

                    // Seek slider
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
                            ToolActionButton(Icons.Rounded.Bedtime, stringResource(R.string.sleep_timer_title), onOpenSleepTimer, "now_playing_sleep_btn")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
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
