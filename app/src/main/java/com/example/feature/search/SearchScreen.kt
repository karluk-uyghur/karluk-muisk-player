package com.example.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongRowItem
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.feature.common.AddToPlaylistDialog
import com.example.feature.common.SongDetailsDialog
import com.example.feature.common.SongOptionsDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    allSongs: List<Song>,
    playlists: List<Playlist>,
    searchHistory: List<String>,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onSaveSearchQuery: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (Long, List<Long>) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var query by rememberSaveable { mutableStateOf("") }
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }

    val results = remember(query, allSongs) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else allSongs.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.formatLabel.lowercase().contains(q) ||
                it.folderName.lowercase().contains(q)
        }
    }

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(title = stringResource(R.string.search_title), onBack = onBack)

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_hint), color = tokens.textSecondary) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = tokens.accentPrimary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear), tint = tokens.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) onSaveSearchQuery(query)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.accentPrimary,
                        unfocusedBorderColor = tokens.glassBorder,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_input")
                )

                if (query.isBlank()) {
                    if (searchHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.search_recent),
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary
                            )
                            TextButton(onClick = onClearSearchHistory) {
                                Text(stringResource(R.string.action_clear), color = tokens.accentPrimary)
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { term ->
                                AssistChip(
                                    onClick = { query = term },
                                    label = { Text(term, color = tokens.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.History, contentDescription = null, tint = tokens.accentPrimary)
                                    }
                                )
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            icon = Icons.Rounded.Search,
                            title = stringResource(R.string.search_no_results),
                            description = query
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(results, key = { _, s -> s.id }) { idx, song ->
                            SongRowItem(
                                song = song,
                                isCurrent = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                onClick = {
                                    onSaveSearchQuery(query)
                                    onPlaySongs(results, idx)
                                },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onMoreClick = { optionSong = song }
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
            onShowDetails = { detailSong = song },
            onRemoveFromLibrary = { onRemoveFromLibrary(song.id) }
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
}
