package com.example.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.core.designsystem.DeleteConfirmDialog
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
import com.example.feature.common.RenameSongDialog

@OptIn(ExperimentalFoundationApi::class)
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
    onToggleFavorite: (Long) -> Unit = {},
    onChangeSpeed: (Float) -> Unit = {},
    onPlaySong: (List<Song>, Int) -> Unit,
    onRenameSong: (Long, String) -> Unit = { _, _ -> },
    onDeleteHeroSong: (Long) -> Unit = {},
    onDeleteRecentSong: (Long) -> Unit = {},
    onClearRecentHistory: () -> Unit = {},
    onMoveRecentSong: (Long, Int) -> Unit = { _, _ -> }
) {
    val tokens = LocalKarlukTokens.current
    val currentSong = playbackState.currentSong
    var renameTarget by remember { mutableStateOf<Song?>(null) }
    var deleteHeroTarget by remember { mutableStateOf<Song?>(null) }
    var deleteRecentTarget by remember { mutableStateOf<Song?>(null) }
    var showClearRecentConfirm by remember { mutableStateOf(false) }
    var heroPencilMenuOpen by remember { mutableStateOf(false) }
    var recentEditMode by remember { mutableStateOf(false) }
    var speedMenuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = recentEditMode) {
        recentEditMode = false
    }

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
                        text = stringResource(R.string.home_tracks_count, allSongsCount),
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
                            SpinningVinylArtwork(
                                artworkUri = currentSong?.artworkUri,
                                isPlaying = playbackState.isPlaying,
                                size = 164.dp,
                                customBackgroundUri = currentSong?.customBackgroundUri
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Full-width reactive spectrum bars clearly below the disc
                        SpectrumVisualizerCanvas(
                            spectrum = spectrum,
                            isPlaying = playbackState.isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentSong != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FormatBadge(formatLabel = currentSong.formatLabel)
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Box {
                                    IconButton(
                                        onClick = { heroPencilMenuOpen = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(36.dp)
                                            .testTag("home_rename_song_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = stringResource(R.string.rename_nasheed_title),
                                            tint = tokens.accentPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = heroPencilMenuOpen,
                                        onDismissRequest = { heroPencilMenuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.rename_nasheed_title)) },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                            onClick = {
                                                heroPencilMenuOpen = false
                                                renameTarget = currentSong
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete), color = Color(0xFFFF5E7E)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF5E7E)
                                                )
                                            },
                                            onClick = {
                                                heroPencilMenuOpen = false
                                                deleteHeroTarget = currentSong
                                            }
                                        )
                                    }
                                }
                                Box {
                                    Surface(
                                        onClick = { speedMenuOpen = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = tokens.glassSurfaceElevated,
                                        border = BorderStroke(0.8.dp, tokens.glassBorderBright.copy(alpha = 0.6f)),
                                        modifier = Modifier.testTag("home_speed_btn")
                                    ) {
                                        Text(
                                            text = "${playbackState.playbackSpeed}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.accentPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = speedMenuOpen,
                                        onDismissRequest = { speedMenuOpen = false }
                                    ) {
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

                        // Seek bar (1:00 ●━━━━━━─────── 3:05) & Transport Controls (⇆ ♡ ◁ ❚❚ ▷ ↻) strictly LTR
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            val duration = playbackState.durationMs.coerceAtLeast(1L)
                            var isSeeking by remember { mutableStateOf(false) }
                            var seekFraction by remember { mutableFloatStateOf(0f) }
                            val liveFraction = (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                            val shownPos = if (isSeeking) (seekFraction * duration).toLong() else playbackState.positionMs

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = FormatUtils.formatDuration(shownPos),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.textSecondary
                                )

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
                                        .weight(1f)
                                        .testTag("home_seek_slider")
                                )

                                Text(
                                    text = FormatUtils.formatDuration(playbackState.durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Transport controls: Shuffle, Favorite Heart, Prev, Play/Pause, Next, Repeat
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
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
                                    onClick = {
                                        if (currentSong != null) onToggleFavorite(currentSong.id)
                                    },
                                    enabled = currentSong != null,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_favorite_button")
                                ) {
                                    val isFav = currentSong?.isFavorite == true
                                    Icon(
                                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = stringResource(R.string.home_quick_favorites),
                                        tint = if (isFav) Color(0xFFFF4D7D) else tokens.textSecondary
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
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                GlowingPlayButton(
                                    isPlaying = playbackState.isPlaying,
                                    onClick = {
                                        if (currentSong != null) onTogglePlayPause() else onOpenLibrary()
                                    },
                                    size = 58.dp,
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
                                        modifier = Modifier.size(30.dp)
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

            // Recently Played Horizontal Strip (Long press -> Jiggle edit mode with Delete & Move, plus Clear button)
            if (recentSongs.isNotEmpty()) {
                item {
                    val displayedRecent = remember(recentSongs) {
                        recentSongs.distinctBy { it.id }.take(12)
                    }
                    val jiggleTransition = rememberInfiniteTransition(label = "recent_jiggle")
                    val jigglePhase by jiggleTransition.animateFloat(
                        initialValue = -1f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 155, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "jiggle_angle"
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_recent_tracks),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.recent_edit_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textSecondary
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { showClearRecentConfirm = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFF5E7E).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFFFF5E7E).copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_clear_recent_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteSweep,
                                            contentDescription = stringResource(R.string.action_clear),
                                            tint = Color(0xFFFF5E7E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.action_clear),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFFF5E7E),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (recentEditMode) {
                                    Surface(
                                        onClick = { recentEditMode = false },
                                        shape = RoundedCornerShape(14.dp),
                                        color = tokens.accentPrimary.copy(alpha = 0.22f),
                                        border = BorderStroke(1.dp, tokens.accentPrimary),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("recent_edit_done_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = tokens.accentPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.action_done),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = tokens.accentPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayedRecent, key = { it.id }) { song ->
                                val idx = displayedRecent.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                val fullIdx = recentSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                val jiggleAngle = if (recentEditMode) {
                                    (if (idx % 2 == 0) 2.6f else -2.6f) * jigglePhase
                                } else {
                                    0f
                                }
                                val jiggleBobY = if (recentEditMode) {
                                    (if (idx % 2 == 0) 1.5f else -1.5f) * jigglePhase
                                } else {
                                    0f
                                }

                                Box(
                                    modifier = Modifier
                                        .width(136.dp)
                                        .graphicsLayer {
                                            rotationZ = jiggleAngle
                                            translationY = jiggleBobY
                                        }
                                ) {
                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (recentEditMode) {
                                                        recentEditMode = false
                                                    } else {
                                                        onPlaySong(recentSongs, fullIdx)
                                                    }
                                                },
                                                onLongClick = { recentEditMode = true }
                                            )
                                            .testTag("recent_song_card_${song.id}"),
                                        shape = RoundedCornerShape(18.dp),
                                        highlighted = song.id == currentSong?.id || recentEditMode
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            SongArtworkThumb(
                                                artworkUri = song.customBackgroundUri?.takeIf { it.isNotBlank() } ?: song.artworkUri,
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

                                    // Floating Delete & Reorder bar on top when jiggle edit mode is active
                                    if (recentEditMode) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(6.dp)
                                                .align(Alignment.TopCenter),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Move earlier (-1)
                                            Surface(
                                                onClick = { onMoveRecentSong(song.id, -1) },
                                                enabled = idx > 0,
                                                shape = CircleShape,
                                                color = Color(0xFF0A1736).copy(alpha = 0.88f),
                                                border = BorderStroke(1.dp, tokens.accentPrimary.copy(alpha = 0.7f)),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ChevronLeft,
                                                        contentDescription = null,
                                                        tint = if (idx > 0) Color.White else Color.White.copy(alpha = 0.35f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Delete from library -> Recycle Bin
                                            Surface(
                                                onClick = { deleteRecentTarget = song },
                                                shape = CircleShape,
                                                color = Color(0xFFFF3B6B),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .testTag("recent_delete_btn_${song.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = stringResource(R.string.action_delete),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }

                                            // Move later (+1)
                                            Surface(
                                                onClick = { onMoveRecentSong(song.id, +1) },
                                                enabled = idx < displayedRecent.lastIndex,
                                                shape = CircleShape,
                                                color = Color(0xFF0A1736).copy(alpha = 0.88f),
                                                border = BorderStroke(1.dp, tokens.accentPrimary.copy(alpha = 0.7f)),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ChevronRight,
                                                        contentDescription = null,
                                                        tint = if (idx < displayedRecent.lastIndex) Color.White else Color.White.copy(alpha = 0.35f),
                                                        modifier = Modifier.size(18.dp)
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
        }
    }

    renameTarget?.let { target ->
        RenameSongDialog(
            song = target,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle -> onRenameSong(target.id, newTitle) }
        )
    }

    deleteHeroTarget?.let { target ->
        DeleteConfirmDialog(
            itemTitle = target.title,
            onConfirm = {
                deleteHeroTarget = null
                onDeleteHeroSong(target.id)
            },
            onDismiss = { deleteHeroTarget = null }
        )
    }

    deleteRecentTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteRecentTarget = null },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = {
                Text(
                    text = stringResource(R.string.action_delete_confirm_title),
                    color = tokens.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_remove_only_desc, target.title),
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDeleteRecentSong(target.id)
                        deleteRecentTarget = null
                    },
                    modifier = Modifier.testTag("confirm_remove_recent_btn")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = Color(0xFFFF5E7E),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteRecentTarget = null }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = tokens.textSecondary
                    )
                }
            }
        )
    }

    if (showClearRecentConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearRecentConfirm = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = {
                Text(
                    text = stringResource(R.string.history_clear),
                    color = tokens.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_clear_confirm_desc),
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showClearRecentConfirm = false
                        recentEditMode = false
                        onClearRecentHistory()
                    },
                    modifier = Modifier.testTag("confirm_clear_recent_btn")
                ) {
                    Text(
                        text = stringResource(R.string.action_clear),
                        color = Color(0xFFFF5E7E),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearRecentConfirm = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = tokens.textSecondary
                    )
                }
            }
        )
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
