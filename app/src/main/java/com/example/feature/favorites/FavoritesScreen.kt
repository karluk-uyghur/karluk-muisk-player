package com.example.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongRowItem
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.feature.common.AddToPlaylistDialog
import com.example.feature.common.SongDetailsDialog
import com.example.feature.common.SongOptionsDialog

@Composable
fun FavoritesScreen(
    favoriteSongs: List<Song>,
    playlists: List<Playlist>,
    playbackState: PlaybackState,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShuffleSongs: (List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (Long, List<Long>) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            KarlukTopBar(title = stringResource(R.string.nav_favorites))

            if (favoriteSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Favorite,
                        title = stringResource(R.string.empty_favorites_title),
                        description = stringResource(R.string.empty_favorites_desc)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlaySongs(favoriteSongs, 0) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("favorites_play_all"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accentPrimary,
                            contentColor = Color(0xFF031126)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_play_all), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onShuffleSongs(favoriteSongs) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("favorites_shuffle_all"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = tokens.accentPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_shuffle_all), color = tokens.textPrimary)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(favoriteSongs, key = { _, song -> song.id }) { index, song ->
                        SongRowItem(
                            song = song,
                            isCurrent = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = { onPlaySongs(favoriteSongs, index) },
                            onToggleFavorite = { onToggleFavorite(song.id) },
                            onMoreClick = { optionSong = song }
                        )
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
