package com.example.feature.queue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.common.ShareUtils
import com.example.core.designsystem.DeleteConfirmDialog
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.SongRowItem
import com.example.core.designsystem.localizedArtist
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.feature.common.AddToPlaylistDialog
import com.example.feature.common.RenameSongDialog
import com.example.feature.common.SongDetailsDialog
import com.example.feature.common.SongOptionsDialog

@Composable
fun QueueScreen(
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onSkipToIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String, List<Long>) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var showSaveDialog by remember { mutableStateOf(false) }

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(
                    title = stringResource(R.string.queue_title),
                    onBack = onBack,
                    actions = {
                        if (playbackState.queue.isNotEmpty()) {
                            IconButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("queue_save_playlist_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = stringResource(R.string.queue_save_as_playlist), tint = tokens.accentPrimary)
                            }
                            IconButton(
                                onClick = onClearQueue,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("queue_clear_btn")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.queue_clear), tint = Color(0xFFFF5E7E))
                            }
                        }
                    }
                )

                if (playbackState.queue.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            title = stringResource(R.string.empty_queue_title),
                            description = stringResource(R.string.empty_queue_desc)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playbackState.queue) { index, song ->
                            val isCurrent = index == playbackState.currentIndex
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                highlighted = isCurrent,
                                onClick = { onSkipToIndex(index) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SongArtworkThumb(artworkUri = song.artworkUri, size = 44.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCurrent) tokens.accentPrimary else tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            FormatBadge(song.formatLabel)
                                            Text(
                                                text = song.localizedArtist(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { if (index > 0) onMoveItem(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", tint = tokens.textSecondary)
                                    }
                                    IconButton(
                                        onClick = { if (index < playbackState.queue.lastIndex) onMoveItem(index, index + 1) },
                                        enabled = index < playbackState.queue.lastIndex,
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", tint = tokens.textSecondary)
                                    }
                                    IconButton(
                                        onClick = { onRemoveItem(index) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = Color(0xFFFF5E7E))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = { Text(stringResource(R.string.queue_save_as_playlist), color = tokens.textPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.playlists_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onSaveQueueAsPlaylist(name, playbackState.queue.map { it.id })
                    }
                    showSaveDialog = false
                }) {
                    Text(stringResource(R.string.action_save), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }
}

@Composable
fun HistoryScreen(
    historySongs: List<Song>,
    playlists: List<Playlist>,
    playbackState: PlaybackState,
    initialSelectionMode: Boolean = false,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onRemoveFromHistory: (Long) -> Unit = {},
    onRemoveSongsFromHistory: (List<Long>) -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (Long, List<Long>) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onRenameSong: (Long, String) -> Unit = { _, _ -> },
    onRemoveFromLibrary: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val context = LocalContext.current
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var deleteTargetSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }
    var renameTargetSong by remember { mutableStateOf<Song?>(null) }
    var selectionMode by remember(initialSelectionMode) {
        mutableStateOf(initialSelectionMode)
    }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    BackHandler(enabled = selectionMode && !initialSelectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(
                    title = stringResource(R.string.history_title),
                    onBack = onBack,
                    actions = {
                        if (historySongs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    if (selectionMode) {
                                        selectionMode = false
                                        selectedIds = emptySet()
                                    } else {
                                        selectionMode = true
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("history_select_mode_btn")
                            ) {
                                Icon(
                                    imageVector = if (selectionMode) Icons.Rounded.Close else Icons.Rounded.Checklist,
                                    contentDescription = stringResource(R.string.history_select_to_delete),
                                    tint = if (selectionMode) tokens.accentPrimary else tokens.textPrimary
                                )
                            }
                            IconButton(
                                onClick = onClearHistory,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("history_clear_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.history_clear),
                                    tint = Color(0xFFFF5E7E)
                                )
                            }
                        }
                    }
                )

                if (historySongs.isNotEmpty() && selectionMode) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        highlighted = true
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedIds = if (selectedIds.size == historySongs.size) {
                                        emptySet()
                                    } else {
                                        historySongs.map { it.id }.toSet()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("history_select_all_btn")
                            ) {
                                Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = tokens.accentPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.history_select_all), color = tokens.textPrimary)
                            }

                            Button(
                                onClick = {
                                    onRemoveSongsFromHistory(selectedIds.toList())
                                    selectedIds = emptySet()
                                },
                                enabled = selectedIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5E7E),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("history_delete_selected_btn")
                            ) {
                                Text(
                                    text = stringResource(R.string.history_delete_selected, selectedIds.size),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (historySongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            icon = Icons.Rounded.History,
                            title = stringResource(R.string.empty_history_title),
                            description = stringResource(R.string.empty_history_desc)
                        )
                    }
                } else {
                    val uniqueHistory = remember(historySongs) { historySongs.distinctBy { it.id } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uniqueHistory, key = { _, s -> s.id }) { idx, song ->
                            val isSelected = selectedIds.contains(song.id)
                            SongRowItem(
                                song = song,
                                isCurrent = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                selectionMode = selectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = if (isSelected) selectedIds - song.id else selectedIds + song.id
                                    } else {
                                        onPlaySongs(uniqueHistory, idx)
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onMoreClick = {
                                    if (selectionMode) {
                                        onRemoveFromHistory(song.id)
                                        selectedIds = selectedIds - song.id
                                    } else {
                                        optionSong = song
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    optionSong?.let { song ->
        SongOptionsDialog(
            song = song,
            onDismiss = { optionSong = null },
            onPlayNow = { onPlaySongs(listOf(song), 0) },
            onPlayNext = { onPlayNext(song) },
            onAddToQueue = { onAddToQueue(listOf(song)) },
            onToggleFavorite = { onToggleFavorite(song.id) },
            onAddToPlaylist = { playlistPickerSong = song },
            onRename = { renameTargetSong = song },
            onShare = { ShareUtils.shareSong(context, song) },
            onShowDetails = { detailSong = song },
            onRemoveFromLibrary = { deleteTargetSong = song }
        )
    }

    renameTargetSong?.let { song ->
        RenameSongDialog(
            song = song,
            onDismiss = { renameTargetSong = null },
            onConfirm = { newTitle -> onRenameSong(song.id, newTitle) }
        )
    }

    playlistPickerSong?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { playlistPickerSong = null },
            onSelectPlaylist = { pid -> onAddToPlaylist(pid, listOf(song.id)) },
            onCreatePlaylist = { name -> onCreatePlaylist(name, listOf(song.id)) }
        )
    }

    detailSong?.let { song ->
        SongDetailsDialog(song = song, onDismiss = { detailSong = null })
    }

    deleteTargetSong?.let { song ->
        DeleteConfirmDialog(
            itemTitle = song.title,
            onConfirm = { onRemoveFromLibrary(song.id) },
            onDismiss = { deleteTargetSong = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerScreen(
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onSetMinutes: (Int) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val activeStatusText = when {
        playbackState.sleepAfterCurrentTrack -> stringResource(R.string.sleep_timer_end_track)
        playbackState.sleepTimerRemainingMs > 0L -> stringResource(
            R.string.sleep_timer_remaining,
            FormatUtils.formatDuration(playbackState.sleepTimerRemainingMs)
        )
        else -> stringResource(R.string.sleep_timer_off)
    }

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(title = stringResource(R.string.sleep_timer_title), onBack = onBack)

                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), highlighted = true) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = activeStatusText,
                                style = MaterialTheme.typography.headlineMedium,
                                color = tokens.textPrimary
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(10, 15, 30, 45, 60, 90).forEach { mins ->
                            Button(
                                onClick = { onSetMinutes(mins) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.glassSurfaceElevated,
                                    contentColor = tokens.textPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("sleep_min_$mins")
                            ) {
                                Text(stringResource(R.string.sleep_timer_minutes, mins), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Button(
                        onClick = onSetEndOfTrack,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("sleep_end_track"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accentPrimary,
                            contentColor = Color(0xFF031126)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.sleep_timer_end_track), fontWeight = FontWeight.Bold)
                    }

                    if (playbackState.sleepTimerRemainingMs > 0L || playbackState.sleepAfterCurrentTrack) {
                        OutlinedButton(
                            onClick = onCancelTimer,
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("sleep_cancel"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.sleep_timer_off), color = Color(0xFFFF5E7E))
                        }
                    }
                }
            }
        }
    }
}
