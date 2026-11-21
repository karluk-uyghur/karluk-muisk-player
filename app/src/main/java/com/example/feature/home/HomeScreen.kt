package com.example.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.GlowingPlayButton
import com.example.core.designsystem.KarlukLogoImage
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.SpectrumVisualizerCanvas
import com.example.core.designsystem.SpinningVinylArtwork
import com.example.core.designsystem.localizedArtist
import com.example.domain.model.PlaybackState
import com.example.domain.model.RepeatModeOption
import com.example.domain.model.Song

@Composable
fun HomeDashboardScreen(
    playbackState: PlaybackState,
    spectrum: FloatArray,
    allSongsCount: Int,
    recentSongs: List<Song>,
    onOpenNowPlaying: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAddFiles: () -> Unit,
    onOpenHistory: () -> Unit,
    isNightMode: Boolean,
    onToggleDayNight: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val currentSong = playbackState.currentSong

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // Fixed Top Brand Header with safe statusBarsPadding so it never clips when scrolled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KarlukLogoImage(size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stringResource(R.string.brand_tagline)} • ${stringResource(R.string.home_tracks_count, allSongsCount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_title),
                        tint = tokens.textPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = tokens.textPrimary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // Centerpiece Hero Player Card (Vinyl + Spectrum + Controls)
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    highlighted = playbackState.isPlaying
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = currentSong != null, onClick = onOpenNowPlaying),
                            contentAlignment = Alignment.Center
                        ) {
                            // Spectrum bars behind/below vinyl
                            SpectrumVisualizerCanvas(
                                spectrum = spectrum,
                                isPlaying = playbackState.isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .align(Alignment.BottomCenter)
                            )
                            SpinningVinylArtwork(
                                artworkUri = currentSong?.artworkUri,
                                isPlaying = playbackState.isPlaying,
                                size = 168.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentSong != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FormatBadge(formatLabel = currentSong.formatLabel)
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentSong.localizedArtist(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.home_no_track),
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.home_tap_to_scan),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Seek bar & Transport Controls always LTR so progress sweeps left->right and Prev/Next stay oriented
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
                                enabled = currentSong != null,
                                colors = SliderDefaults.colors(
                                    thumbColor = tokens.accentPrimary,
                                    activeTrackColor = tokens.accentPrimary,
                                    inactiveTrackColor = tokens.glassBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_seek_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val shownPos = if (isSeeking) (seekFraction * duration).toLong() else playbackState.positionMs
                                Text(
                                    text = FormatUtils.formatDuration(shownPos),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.textSecondary
                                )
                                Text(
                                    text = FormatUtils.formatDuration(playbackState.durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Transport controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onToggleShuffle,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_shuffle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (playbackState.shuffleEnabled) tokens.accentPrimary else tokens.textSecondary
                                    )
                                }

                                IconButton(
                                    onClick = onSkipPrev,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_prev_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = tokens.textPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                GlowingPlayButton(
                                    isPlaying = playbackState.isPlaying,
                                    onClick = {
                                        if (currentSong != null) onTogglePlayPause() else onOpenLibrary()
                                    },
                                    size = 62.dp,
                                    testTag = "home_play_pause_button"
                                )

                                IconButton(
                                    onClick = onSkipNext,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        tint = tokens.textPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onCycleRepeat,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_repeat_button")
                                ) {
                                    val icon = if (playbackState.repeatMode == RepeatModeOption.ONE) {
                                        Icons.Rounded.RepeatOne
                                    } else {
                                        Icons.Rounded.Repeat
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Repeat",
                                        tint = if (playbackState.repeatMode != RepeatModeOption.OFF) tokens.accentPrimary else tokens.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Access 2x3 Glass Cards Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickTileCard(
                            icon = Icons.Rounded.LibraryMusic,
                            label = stringResource(R.string.home_quick_library),
                            tint = tokens.accentPrimary,
                            onClick = onOpenLibrary,
                            modifier = Modifier.weight(1f).testTag("quick_library")
                        )
                        QuickTileCard(
                            icon = Icons.Rounded.Favorite,
                            label = stringResource(R.string.home_quick_favorites),
                            tint = Color(0xFFFF4D7D),
                            onClick = onOpenFavorites,
                            modifier = Modifier.weight(1f).testTag("quick_favorites")
                        )
                        QuickTileCard(
                            icon = Icons.Rounded.Equalizer,
                            label = stringResource(R.string.home_quick_equalizer),
                            tint = tokens.accentViolet,
                            onClick = onOpenEqualizer,
                            modifier = Modifier.weight(1f).testTag("quick_equalizer")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickTileCard(
                            icon = Icons.Rounded.CreateNewFolder,
                            label = stringResource(R.string.home_quick_files),
                            tint = tokens.emerald,
                            onClick = onOpenAddFiles,
                            modifier = Modifier.weight(1f).testTag("quick_add_files")
                        )
                        QuickTileCard(
                            icon = Icons.Rounded.History,
                            label = stringResource(R.string.home_quick_history),
                            tint = tokens.accentSecondary,
                            onClick = onOpenHistory,
                            modifier = Modifier.weight(1f).testTag("quick_history")
                        )
                        QuickTileCard(
                            icon = if (isNightMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            label = stringResource(if (isNightMode) R.string.theme_night_mode else R.string.theme_day_mode),
                            tint = if (isNightMode) Color(0xFFFFB74D) else Color(0xFFFF9800),
                            onClick = onToggleDayNight,
                            modifier = Modifier.weight(1f).testTag("quick_day_night_mode")
                        )
                    }
                }
            }

            // Recently Played Horizontal Strip
            if (recentSongs.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_recent_tracks),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentSongs.take(12), key = { it.id }) { song ->
                                val idx = recentSongs.indexOfFirst { it.id == song.id }
                                GlassCard(
                                    modifier = Modifier.width(136.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    highlighted = song.id == currentSong?.id,
                                    onClick = { onPlaySong(recentSongs, idx.coerceAtLeast(0)) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        SongArtworkThumb(
                                            artworkUri = song.artworkUri,
                                            size = 116.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.localizedArtist(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun QuickTileCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    GlassCard(
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
