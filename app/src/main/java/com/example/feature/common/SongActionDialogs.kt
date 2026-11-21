package com.example.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.localizedAlbum
import com.example.core.designsystem.localizedArtist
import com.example.domain.model.Playlist
import com.example.domain.model.Song

@Composable
fun SongOptionsDialog(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit,
    onRemoveFromLibrary: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
        titleContentColor = tokens.textPrimary,
        textContentColor = tokens.textSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SongArtworkThumb(artworkUri = song.artworkUri, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FormatBadge(formatLabel = song.formatLabel)
                        Text(
                            text = song.localizedArtist(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OptionActionRow(Icons.Rounded.PlayArrow, stringResource(R.string.action_play_all)) {
                    onPlayNow(); onDismiss()
                }
                OptionActionRow(Icons.AutoMirrored.Rounded.PlaylistPlay, stringResource(R.string.action_play_next)) {
                    onPlayNext(); onDismiss()
                }
                OptionActionRow(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.action_add_to_queue)) {
                    onAddToQueue(); onDismiss()
                }
                OptionActionRow(
                    if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    stringResource(R.string.home_quick_favorites)
                ) {
                    onToggleFavorite(); onDismiss()
                }
                OptionActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.action_add_to_playlist)) {
                    onDismiss(); onAddToPlaylist()
                }
                OptionActionRow(Icons.Rounded.Info, stringResource(R.string.files_details)) {
                    onDismiss(); onShowDetails()
                }
                OptionActionRow(Icons.Rounded.DeleteOutline, stringResource(R.string.action_remove)) {
                    onRemoveFromLibrary(); onDismiss()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = tokens.accentPrimary)
            }
        }
    )
}

@Composable
private fun OptionActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tokens.accentPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tokens.textPrimary)
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
        title = {
            Text(stringResource(R.string.action_add_to_playlist), color = tokens.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text(stringResource(R.string.playlists_name_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_playlist_inline_input")
                    )
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreatePlaylist(newName.trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accentPrimary,
                            contentColor = Color(0xFF041126)
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_create))
                    }
                }

                if (playlists.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                onClick = {
                                    onSelectPlaylist(pl.id)
                                    onDismiss()
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = pl.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.playlists_songs_count, pl.songs.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = tokens.accentPrimary)
            }
        }
    )
}

@Composable
fun SongDetailsDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
        title = {
            Text(stringResource(R.string.files_details), color = tokens.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailLine(stringResource(R.string.sort_title), song.title)
                DetailLine(stringResource(R.string.sort_artist), song.localizedArtist())
                DetailLine(stringResource(R.string.tab_albums), song.localizedAlbum())
                DetailLine(stringResource(R.string.action_filter_format), "${song.formatLabel} (${song.mimeType})")
                DetailLine(stringResource(R.string.sort_duration), FormatUtils.formatDuration(song.duration))
                DetailLine(stringResource(R.string.storage_total_size), FormatUtils.formatFileSize(song.fileSize))
                DetailLine(stringResource(R.string.tab_folders), song.folderName)
                DetailLine("URI", song.uri)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    val tokens = LocalKarlukTokens.current
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tokens.accentPrimary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
