package com.example.feature.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.DeleteConfirmDialog
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.localizedArtist
import com.example.domain.model.PlaybackState
import com.example.domain.model.Song

@Composable
fun FilteredMusicScreen(
    songs: List<Song>,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onPreviewSongs: (List<Song>, Int) -> Unit,
    onApproveMoveToNasheeds: (List<Long>) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var pendingSingleMoveSong by remember { mutableStateOf<Song?>(null) }
    var pendingBatchMoveIds by remember { mutableStateOf<List<Long>?>(null) }
    var deleteTargetSong by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            KarlukTopBar(
                title = stringResource(R.string.filtered_music_folder_title),
                onBack = onBack,
                actions = {
                    if (songs.isNotEmpty()) {
                        if (selectionMode) {
                            IconButton(
                                onClick = {
                                    selectedIds = if (selectedIds.size == songs.size) {
                                        emptySet()
                                    } else {
                                        songs.map { it.id }.toSet()
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("filtered_music_select_all_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelectAll,
                                    contentDescription = stringResource(R.string.history_select_all),
                                    tint = tokens.accentPrimary
                                )
                            }
                            TextButton(
                                onClick = {
                                    selectionMode = false
                                    selectedIds = emptySet()
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                            }
                        } else {
                            TextButton(
                                onClick = { selectionMode = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("filtered_music_select_btn")
                            ) {
                                Text(stringResource(R.string.action_select), color = tokens.accentPrimary)
                            }
                        }
                    }
                }
            )

            if (selectionMode && selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pendingBatchMoveIds = selectedIds.toList() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.emerald,
                            contentColor = Color(0xFF041A12)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("filtered_music_batch_move_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.filtered_music_move_selected, selectedIds.size),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        icon = Icons.Rounded.MusicOff,
                        title = stringResource(R.string.filtered_music_empty_title),
                        description = stringResource(R.string.filtered_music_empty_desc)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            highlighted = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterAlt,
                                    contentDescription = null,
                                    tint = tokens.accentViolet,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stringResource(R.string.filtered_music_banner_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                        }
                    }

                    itemsIndexed(songs, key = { _, song -> song.id }) { idx, song ->
                        val isCurrent = playbackState.currentSong?.id == song.id
                        val isPlaying = isCurrent && playbackState.isPlaying
                        val isSelected = song.id in selectedIds

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("filtered_music_item_${song.id}"),
                            shape = RoundedCornerShape(18.dp),
                            highlighted = isCurrent || isSelected,
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = if (isSelected) selectedIds - song.id else selectedIds + song.id
                                } else {
                                    onPreviewSongs(songs, idx)
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectionMode) {
                                        IconButton(
                                            onClick = {
                                                selectedIds = if (isSelected) selectedIds - song.id else selectedIds + song.id
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = if (isSelected) tokens.accentPrimary else tokens.textSecondary
                                            )
                                        }
                                    } else {
                                        SongArtworkThumb(
                                            artworkUri = song.customBackgroundUri ?: song.artworkUri,
                                            size = 48.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCurrent) tokens.accentPrimary else tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FormatBadge(song.formatLabel)
                                            Text(
                                                text = "${song.localizedArtist()} • ${FormatUtils.formatDuration(song.duration)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onPreviewSongs(songs, idx) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("filtered_music_preview_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else if (isCurrent) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = stringResource(R.string.action_play),
                                            tint = tokens.accentPrimary
                                        )
                                    }
                                }

                                if (!selectionMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { pendingSingleMoveSong = song },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, tokens.emerald.copy(alpha = 0.7f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .testTag("filtered_music_move_${song.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                                                contentDescription = null,
                                                tint = tokens.emerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.filtered_music_move_action),
                                                color = tokens.emerald,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { deleteTargetSong = song },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("filtered_music_delete_${song.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = stringResource(R.string.action_delete),
                                                tint = Color(0xFFFF5E7E)
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

    pendingSingleMoveSong?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingSingleMoveSong = null },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                    contentDescription = null,
                    tint = tokens.emerald
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.filtered_music_move_permission_title),
                    color = tokens.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.filtered_music_move_permission_desc, target.title),
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApproveMoveToNasheeds(listOf(target.id))
                        pendingSingleMoveSong = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.emerald,
                        contentColor = Color(0xFF041A12)
                    ),
                    modifier = Modifier.testTag("confirm_move_filtered_music_btn")
                ) {
                    Text(stringResource(R.string.filtered_music_move_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSingleMoveSong = null }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }

    pendingBatchMoveIds?.let { ids ->
        AlertDialog(
            onDismissRequest = { pendingBatchMoveIds = null },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                    contentDescription = null,
                    tint = tokens.emerald
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.filtered_music_move_permission_title),
                    color = tokens.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.filtered_music_move_batch_permission_desc, ids.size),
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApproveMoveToNasheeds(ids)
                        pendingBatchMoveIds = null
                        selectionMode = false
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.emerald,
                        contentColor = Color(0xFF041A12)
                    )
                ) {
                    Text(stringResource(R.string.filtered_music_move_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBatchMoveIds = null }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }

    deleteTargetSong?.let { target ->
        DeleteConfirmDialog(
            itemTitle = target.title,
            onConfirm = { onRemoveFromLibrary(target.id) },
            onDismiss = { deleteTargetSong = null }
        )
    }
}
