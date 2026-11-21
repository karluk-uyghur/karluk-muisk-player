package com.example.feature.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.SongRowItem
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.FolderItem
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import com.example.feature.common.AddToPlaylistDialog
import com.example.feature.common.SongDetailsDialog
import com.example.feature.common.SongOptionsDialog

@Composable
fun LibraryScreen(
    allSongs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    folders: List<FolderItem>,
    playlists: List<Playlist>,
    historySongs: List<Song>,
    playbackState: PlaybackState,
    isScanning: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAddFiles: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShuffleSongs: (List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onBatchFavorite: (List<Long>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddToPlaylist: (Long, List<Long>) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.TITLE) }
    var formatFilter by rememberSaveable { mutableStateOf("ALL") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Multi-select state
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog states
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerIds by remember { mutableStateOf<List<Long>?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var renamePlaylistTarget by remember { mutableStateOf<Playlist?>(null) }

    val tabTitles = listOf(
        stringResource(R.string.tab_all_songs),
        stringResource(R.string.tab_albums),
        stringResource(R.string.tab_artists),
        stringResource(R.string.tab_folders),
        stringResource(R.string.tab_playlists),
        stringResource(R.string.tab_recent)
    )

    val availableFormats = remember(allSongs) {
        listOf("ALL") + allSongs.map { it.formatLabel }.distinct().sorted()
    }

    val filteredSortedSongs = remember(allSongs, sortOption, formatFilter) {
        val filtered = if (formatFilter == "ALL") allSongs else allSongs.filter { it.formatLabel == formatFilter }
        when (sortOption) {
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
            SortOption.DURATION -> filtered.sortedByDescending { it.duration }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            if (selectionMode) {
                KarlukTopBar(
                    title = stringResource(R.string.selected_count, selectedIds.size),
                    onBack = {
                        selectionMode = false
                        selectedIds = emptySet()
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    onBatchFavorite(selectedIds.toList())
                                }
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = "Favorite selected", tint = Color(0xFFFF4D7D))
                        }
                        IconButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    playlistPickerIds = selectedIds.toList()
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to playlist", tint = tokens.accentPrimary)
                        }
                        IconButton(
                            onClick = {
                                val chosen = filteredSortedSongs.filter { it.id in selectedIds }
                                if (chosen.isNotEmpty()) onAddToQueue(chosen)
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Add to queue", tint = tokens.textPrimary)
                        }
                        IconButton(
                            onClick = {
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = tokens.textPrimary)
                        }
                    }
                )
            } else {
                KarlukTopBar(
                    title = stringResource(R.string.library_title),
                    actions = {
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_search_btn")
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title), tint = tokens.textPrimary)
                        }
                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_sort_btn")
                            ) {
                                Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.action_sort), tint = tokens.textPrimary)
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_title)) },
                                    onClick = { sortOption = SortOption.TITLE; sortMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_artist)) },
                                    onClick = { sortOption = SortOption.ARTIST; sortMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_date_added)) },
                                    onClick = { sortOption = SortOption.DATE_ADDED; sortMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_duration)) },
                                    onClick = { sortOption = SortOption.DURATION; sortMenuExpanded = false }
                                )
                            }
                        }
                        IconButton(
                            onClick = { selectionMode = true },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_multiselect_btn")
                        ) {
                            Icon(Icons.Rounded.CheckBox, contentDescription = stringResource(R.string.action_select), tint = tokens.textPrimary)
                        }
                        IconButton(
                            onClick = onRescan,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_rescan_btn")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = tokens.accentPrimary)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_rescan), tint = tokens.accentPrimary)
                            }
                        }
                    }
                )
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = tokens.accentPrimary,
                edgePadding = 12.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == idx) tokens.accentPrimary else tokens.textSecondary
                            )
                        },
                        modifier = Modifier.testTag("library_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // All Songs
                    if (allSongs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateCard(
                                icon = Icons.Rounded.LibraryMusic,
                                title = if (!hasAudioPermission) stringResource(R.string.permission_denied_title) else stringResource(R.string.empty_library_title),
                                description = if (!hasAudioPermission) stringResource(R.string.permission_denied_desc) else stringResource(R.string.empty_library_desc),
                                actionLabel = if (!hasAudioPermission) stringResource(R.string.onboarding_grant_button) else stringResource(R.string.nav_add_files),
                                onAction = if (!hasAudioPermission) onRequestPermission else onOpenAddFiles
                            )
                        }
                    } else {
                        // Format filter chips + Play/Shuffle row
                        if (availableFormats.size > 2) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(availableFormats) { fmt ->
                                    val isAll = fmt == "ALL"
                                    FilterChip(
                                        selected = formatFilter == fmt,
                                        onClick = { formatFilter = fmt },
                                        label = {
                                            Text(if (isAll) stringResource(R.string.filter_all_formats) else fmt)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = tokens.accentPrimary.copy(alpha = 0.24f),
                                            selectedLabelColor = tokens.accentPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onPlaySongs(filteredSortedSongs, 0) },
                                modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.accentPrimary,
                                    contentColor = Color(0xFF031126)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_play_all), fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onShuffleSongs(filteredSortedSongs) },
                                modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = tokens.accentPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_shuffle_all), color = tokens.textPrimary)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredSortedSongs, key = { _, s -> s.id }) { idx, song ->
                                val selected = song.id in selectedIds
                                SongRowItem(
                                    song = song,
                                    isCurrent = playbackState.currentSong?.id == song.id,
                                    isPlaying = playbackState.isPlaying,
                                    selectionMode = selectionMode,
                                    isSelected = selected,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedIds = if (selected) selectedIds - song.id else selectedIds + song.id
                                        } else {
                                            onPlaySongs(filteredSortedSongs, idx)
                                        }
                                    },
                                    onToggleFavorite = { onToggleFavorite(song.id) },
                                    onMoreClick = { optionSong = song }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Albums Grid
                    if (albums.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                            EmptyStateCard(
                                icon = Icons.Rounded.Album,
                                title = stringResource(R.string.empty_library_title),
                                description = stringResource(R.string.empty_library_desc),
                                actionLabel = stringResource(R.string.nav_add_files),
                                onAction = onOpenAddFiles
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(154.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(albums, key = { it.name }) { album ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenAlbum(album.name) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        SongArtworkThumb(
                                            artworkUri = album.artworkUri,
                                            size = 124.dp,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${album.artist} • ${stringResource(R.string.home_tracks_count, album.songCount)}",
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

                2 -> {
                    // Artists
                    if (artists.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                            EmptyStateCard(
                                icon = Icons.Rounded.Person,
                                title = stringResource(R.string.empty_library_title),
                                description = stringResource(R.string.empty_library_desc)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(artists, key = { it.name }) { artist ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenArtist(artist.name) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SongArtworkThumb(artworkUri = artist.artworkUri, size = 48.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = tokens.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.home_tracks_count, artist.songCount),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Folders
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            OutlinedButton(
                                onClick = onOpenAddFiles,
                                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = tokens.accentPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.files_pick_folder), color = tokens.textPrimary)
                            }
                        }
                        items(folders, key = { it.path }) { folder ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenFolder(folder) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = tokens.accentPrimary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.home_tracks_count, folder.songCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // Playlists
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("create_playlist_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.accentPrimary,
                                    contentColor = Color(0xFF031126)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.playlists_new), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (playlists.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    title = stringResource(R.string.empty_playlists_title),
                                    description = stringResource(R.string.empty_playlists_desc)
                                )
                            }
                        } else {
                            items(playlists, key = { it.id }) { pl ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenPlaylist(pl.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SongArtworkThumb(
                                            artworkUri = pl.songs.firstOrNull()?.artworkUri,
                                            size = 48.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pl.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = tokens.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.playlists_songs_count, pl.songs.size),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary
                                            )
                                        }
                                        IconButton(
                                            onClick = { renamePlaylistTarget = pl },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_rename), tint = tokens.textSecondary)
                                        }
                                        IconButton(
                                            onClick = { onDeletePlaylist(pl.id) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color(0xFFFF5E7E))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Recent & Most Played
                    if (historySongs.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                            EmptyStateCard(
                                icon = Icons.Rounded.LibraryMusic,
                                title = stringResource(R.string.empty_history_title),
                                description = stringResource(R.string.empty_history_desc)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(historySongs, key = { _, s -> s.id }) { idx, song ->
                                SongRowItem(
                                    song = song,
                                    isCurrent = playbackState.currentSong?.id == song.id,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { onPlaySongs(historySongs, idx) },
                                    onToggleFavorite = { onToggleFavorite(song.id) },
                                    onMoreClick = { optionSong = song }
                                )
                            }
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
            onAddToPlaylist = { playlistPickerIds = listOf(song.id) },
            onShowDetails = { detailSong = song },
            onRemoveFromLibrary = { onRemoveFromLibrary(song.id) }
        )
    }

    playlistPickerIds?.let { ids ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { playlistPickerIds = null },
            onSelectPlaylist = { pid -> onAddToPlaylist(pid, ids) },
            onCreatePlaylist = { name -> onCreatePlaylist(name, ids) }
        )
    }

    detailSong?.let { song ->
        SongDetailsDialog(song = song, onDismiss = { detailSong = null })
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = { Text(stringResource(R.string.playlists_new), color = tokens.textPrimary) },
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
                    if (name.isNotBlank()) onCreatePlaylist(name, emptyList())
                    showCreatePlaylistDialog = false
                }) {
                    Text(stringResource(R.string.action_create), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }

    renamePlaylistTarget?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamePlaylistTarget = null },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = { Text(stringResource(R.string.action_rename), color = tokens.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenamePlaylist(target.id, newName)
                    renamePlaylistTarget = null
                }) {
                    Text(stringResource(R.string.action_save), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamePlaylistTarget = null }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }
}

@Composable
fun CollectionDetailScreen(
    title: String,
    songs: List<Song>,
    playlists: List<Playlist>,
    playbackState: PlaybackState,
    canExcludeFolder: Boolean = false,
    playlistId: Long? = null,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShuffleSongs: (List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (Long, List<Long>) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onRemoveFromPlaylist: ((Long) -> Unit)? = null,
    onExcludeFolder: (() -> Unit)? = null,
    onRemoveFromLibrary: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            KarlukTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (canExcludeFolder && onExcludeFolder != null) {
                        TextButton(onClick = { onExcludeFolder(); onBack() }) {
                            Text(stringResource(R.string.files_exclude_folder), color = Color(0xFFFF6E8A))
                        }
                    }
                }
            )

            if (songs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlaySongs(songs, 0) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accentPrimary, contentColor = Color(0xFF031126)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_play_all), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onShuffleSongs(songs) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = tokens.accentPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_shuffle_all), color = tokens.textPrimary)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
                    SongRowItem(
                        song = song,
                        isCurrent = playbackState.currentSong?.id == song.id,
                        isPlaying = playbackState.isPlaying,
                        onClick = { onPlaySongs(songs, idx) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onMoreClick = { optionSong = song }
                    )
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
            onRemoveFromLibrary = {
                if (playlistId != null && onRemoveFromPlaylist != null) {
                    onRemoveFromPlaylist(song.id)
                } else {
                    onRemoveFromLibrary(song.id)
                }
            }
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
