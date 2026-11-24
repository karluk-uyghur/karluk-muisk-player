package com.example.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.common.ShareUtils
import com.example.core.designsystem.DeleteConfirmDialog
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.SongRowItem
import com.example.core.designsystem.localizedAlbumName
import com.example.core.designsystem.localizedArtistName
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.FolderItem
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import com.example.feature.common.AddToPlaylistDialog
import com.example.feature.common.RenameSongDialog
import com.example.feature.common.SongDetailsDialog
import com.example.feature.common.SongOptionsDialog
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
    onRenameSong: (Long, String) -> Unit = { _, _ -> },
    onRemoveFromLibrary: (Long) -> Unit,
    onBatchRemoveFromLibrary: (List<Long>) -> Unit = { ids -> ids.forEach(onRemoveFromLibrary) },
    filteredMusicCount: Int = 0,
    onOpenFilteredMusic: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokens = LocalKarlukTokens.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.TITLE) }
    var formatFilter by rememberSaveable { mutableStateOf("ALL") }

    // Multi-select state
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog states
    var showControlsMenu by remember { mutableStateOf(false) }
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var deleteTargetSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerIds by remember { mutableStateOf<List<Long>?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }
    var renameTargetSong by remember { mutableStateOf<Song?>(null) }
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
                                if (selectedIds.isNotEmpty()) {
                                    onBatchRemoveFromLibrary(selectedIds.toList())
                                }
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("multiselect_delete_btn")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = Color(0xFFFF6E8A))
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
                // Clean header: Search + Three-Dots menu (categories, filters, sort, multi-select, rescan all live inside ⋮)
                KarlukTopBar(
                    title = if (selectedTab == 0) {
                        stringResource(R.string.library_title)
                    } else {
                        tabTitles[selectedTab]
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("library_search_btn")
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.search_title),
                                tint = tokens.textPrimary
                            )
                        }
                        IconButton(
                            onClick = { showControlsMenu = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("library_more_menu_btn")
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.library_menu_title),
                                tint = tokens.accentPrimary
                            )
                        }
                    }
                )

                if (isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = tokens.accentPrimary,
                        trackColor = tokens.glassBorder.copy(alpha = 0.25f)
                    )
                }

                // Compact active filter/category badge only when user switched away from defaults
                if (selectedTab != 0 || formatFilter != "ALL") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTab != 0) {
                            FilterChip(
                                selected = true,
                                onClick = { selectedTab = 0 },
                                label = { Text(tabTitles[selectedTab]) },
                                trailingIcon = {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.accentPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = tokens.accentPrimary
                                )
                            )
                        }
                        if (formatFilter != "ALL") {
                            FilterChip(
                                selected = true,
                                onClick = { formatFilter = "ALL" },
                                label = { Text(formatFilter) },
                                trailingIcon = {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.accentViolet.copy(alpha = 0.22f),
                                    selectedLabelColor = tokens.accentViolet
                                )
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // All Nasheeds with Left Alphabet Fast-Scroller + Right Draggable Scrollbar
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

                        FastScrollNasheedList(
                            songs = filteredSortedSongs,
                            currentSongId = playbackState.currentSong?.id,
                            isPlaying = playbackState.isPlaying,
                            selectionMode = selectionMode,
                            selectedIds = selectedIds,
                            onSongClick = { idx, song ->
                                val selected = song.id in selectedIds
                                if (selectionMode) {
                                    selectedIds = if (selected) selectedIds - song.id else selectedIds + song.id
                                } else {
                                    onPlaySongs(filteredSortedSongs, idx)
                                }
                            },
                            onToggleFavorite = onToggleFavorite,
                            onMoreClick = { optionSong = it },
                            modifier = Modifier.weight(1f)
                        )
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
                                            text = localizedAlbumName(album.name),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${localizedArtistName(album.artist)} • ${stringResource(R.string.home_tracks_count, album.songCount)}",
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
                                                text = localizedArtistName(artist.name),
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
                        if (filteredMusicCount > 0) {
                            item {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("library_filtered_music_folder_card"),
                                    highlighted = true,
                                    onClick = onOpenFilteredMusic
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicOff,
                                            contentDescription = null,
                                            tint = tokens.accentViolet,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.filtered_music_folder_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = tokens.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.filtered_music_folder_desc, filteredMusicCount),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = tokens.textSecondary
                                        )
                                    }
                                }
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
                                            artworkUri = pl.songs.firstOrNull()?.let { it.customBackgroundUri ?: it.artworkUri },
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

    if (showControlsMenu) {
        LibraryControlsDialog(
            tabTitles = tabTitles,
            selectedTab = selectedTab,
            onSelectTab = { idx ->
                selectedTab = idx
                showControlsMenu = false
            },
            availableFormats = availableFormats,
            selectedFormat = formatFilter,
            onSelectFormat = { fmt -> formatFilter = fmt },
            selectedSort = sortOption,
            onSelectSort = { opt -> sortOption = opt },
            isScanning = isScanning,
            onRescan = {
                showControlsMenu = false
                onRescan()
            },
            onStartMultiSelect = {
                showControlsMenu = false
                selectedTab = 0
                selectionMode = true
            },
            onDismiss = { showControlsMenu = false }
        )
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

    deleteTargetSong?.let { song ->
        DeleteConfirmDialog(
            itemTitle = song.title,
            onConfirm = { onRemoveFromLibrary(song.id) },
            onDismiss = { deleteTargetSong = null }
        )
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
private fun FastScrollNasheedList(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onSongClick: (Int, Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val originalDirection = LocalLayoutDirection.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Map distinct starting characters to their first appearance index in the sorted list
    val letterIndexPairs = remember(songs) {
        val seen = linkedMapOf<String, Int>()
        songs.forEachIndexed { idx, s ->
            val ch = s.title.trim().firstOrNull()
            val label = when {
                ch == null -> "#"
                ch.isDigit() -> "#"
                else -> ch.uppercaseChar().toString()
            }
            if (!seen.containsKey(label)) {
                seen[label] = idx
            }
        }
        seen.entries.map { it.key to it.value }
    }

    // Visual labels shown vertically on the left rail (capped so letters never overlap on smaller screens)
    val displayLetters = remember(letterIndexPairs) {
        if (letterIndexPairs.size <= 22) {
            letterIndexPairs.map { it.first }
        } else {
            List(22) { slot ->
                val mappedIdx = (slot.toFloat() / 21f * (letterIndexPairs.size - 1)).roundToInt()
                letterIndexPairs[mappedIdx].first
            }
        }
    }

    var activeLetter by remember { mutableStateOf<String?>(null) }
    var leftRailHeightPx by remember { mutableIntStateOf(1) }

    var rightTrackHeightPx by remember { mutableIntStateOf(1) }
    var isDraggingScrollbar by remember { mutableStateOf(false) }
    var dragScrollFraction by remember { mutableFloatStateOf(0f) }

    fun jumpByLeftY(y: Float) {
        if (letterIndexPairs.isEmpty() || leftRailHeightPx <= 0) return
        val fraction = (y / leftRailHeightPx.toFloat()).coerceIn(0f, 0.9999f)
        val idx = (fraction * letterIndexPairs.size).toInt().coerceIn(0, letterIndexPairs.lastIndex)
        val (letter, songIdx) = letterIndexPairs[idx]
        if (activeLetter != letter) {
            activeLetter = letter
            scope.launch { listState.scrollToItem(songIdx) }
        }
    }

    fun scrollByRightY(y: Float) {
        if (songs.isEmpty() || rightTrackHeightPx <= 0) return
        val fraction = (y / rightTrackHeightPx.toFloat()).coerceIn(0f, 1f)
        dragScrollFraction = fraction
        val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        val maxFirstIndex = (songs.size - visibleCount).coerceAtLeast(0)
        val targetIdx = (fraction * maxFirstIndex).roundToInt().coerceIn(0, songs.lastIndex)
        scope.launch { listState.scrollToItem(targetIdx) }
    }

    val totalCount = songs.size
    val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val maxFirstIndex = (totalCount - visibleCount).coerceAtLeast(1)
    val currentScrollFraction = if (isDraggingScrollbar) {
        dragScrollFraction
    } else if (totalCount <= visibleCount) {
        0f
    } else {
        (listState.firstVisibleItemIndex.toFloat() / maxFirstIndex.toFloat()).coerceIn(0f, 1f)
    }

    // Use physical LTR outer Box so Alphabet Scroller is ALWAYS on physical LEFT and Scrollbar ALWAYS on physical RIGHT
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxSize()) {
            // Keep LazyColumn items in the user's actual locale direction (RTL for Uyghur/Arabic)
            CompositionLocalProvider(LocalLayoutDirection provides originalDirection) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_nasheed_list"),
                    contentPadding = PaddingValues(start = 24.dp, end = 34.dp, top = 6.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
                        val selected = song.id in selectedIds
                        SongRowItem(
                            song = song,
                            isCurrent = currentSongId == song.id,
                            isPlaying = isPlaying,
                            selectionMode = selectionMode,
                            isSelected = selected,
                            onClick = { onSongClick(idx, song) },
                            onToggleFavorite = { onToggleFavorite(song.id) },
                            onMoreClick = { onMoreClick(song) }
                        )
                    }
                }
            }

            // Physical LEFT edge: Bidirectional vertical Alphabet Fast-Scroller (top->bottom & bottom->top)
            if (displayLetters.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .padding(start = 2.dp, top = 12.dp, bottom = 112.dp)
                        .width(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (activeLetter != null) tokens.accentPrimary.copy(alpha = 0.14f)
                            else tokens.glassSurface.copy(alpha = 0.45f)
                        )
                        .border(0.6.dp, tokens.glassBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .onSizeChanged { leftRailHeightPx = it.height }
                        .pointerInput(letterIndexPairs) {
                            detectTapGestures(
                                onPress = { offset ->
                                    jumpByLeftY(offset.y)
                                    tryAwaitRelease()
                                    activeLetter = null
                                }
                            )
                        }
                        .pointerInput(letterIndexPairs) {
                            detectVerticalDragGestures(
                                onDragStart = { offset -> jumpByLeftY(offset.y) },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    jumpByLeftY(change.position.y)
                                },
                                onDragEnd = { activeLetter = null },
                                onDragCancel = { activeLetter = null }
                            )
                        }
                        .padding(vertical = 4.dp)
                        .testTag("library_left_alphabet_scroller"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    displayLetters.forEach { ch ->
                        val isHighlighted = activeLetter == ch
                        Text(
                            text = ch,
                            fontSize = if (isHighlighted) 12.sp else 10.sp,
                            fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isHighlighted) tokens.accentPrimary else tokens.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            // Floating Alphabet Preview Bubble while dragging on the left rail
            activeLetter?.let { ch ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 42.dp)
                        .size(60.dp),
                    shape = CircleShape,
                    color = tokens.accentPrimary,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ch,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF031126)
                        )
                    }
                }
            }

            // Physical RIGHT edge: Visible vertical scrollbar track + draggable pill thumb
            if (songs.size > 3) {
                val thumbHeightDp = 46.dp
                val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
                val travelPx = (rightTrackHeightPx - thumbHeightPx).coerceAtLeast(0f)
                val thumbOffsetY = (currentScrollFraction * travelPx).roundToInt()

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(end = 3.dp, top = 12.dp, bottom = 112.dp)
                        .width(20.dp)
                        .onSizeChanged { rightTrackHeightPx = it.height }
                        .pointerInput(songs.size) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isDraggingScrollbar = true
                                    scrollByRightY(offset.y)
                                    tryAwaitRelease()
                                    isDraggingScrollbar = false
                                }
                            )
                        }
                        .pointerInput(songs.size) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    isDraggingScrollbar = true
                                    scrollByRightY(offset.y)
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    scrollByRightY(change.position.y)
                                },
                                onDragEnd = { isDraggingScrollbar = false },
                                onDragCancel = { isDraggingScrollbar = false }
                            )
                        }
                        .testTag("library_right_scrollbar"),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Visible vertical track rail
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(tokens.glassBorder.copy(alpha = 0.42f))
                    )
                    // Visible glowing draggable scrollbar thumb
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, thumbOffsetY) }
                            .width(if (isDraggingScrollbar) 10.dp else 7.dp)
                            .height(thumbHeightDp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDraggingScrollbar) tokens.accentPrimary
                                else tokens.accentPrimary.copy(alpha = 0.85f)
                            )
                            .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryControlsDialog(
    tabTitles: List<String>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    availableFormats: List<String>,
    selectedFormat: String,
    onSelectFormat: (String) -> Unit,
    selectedSort: SortOption,
    onSelectSort: (SortOption) -> Unit,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onStartMultiSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val tabIcons = listOf(
        Icons.Rounded.LibraryMusic,
        Icons.Rounded.Album,
        Icons.Rounded.Person,
        Icons.Rounded.Folder,
        Icons.AutoMirrored.Rounded.QueueMusic,
        Icons.Rounded.History
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
        title = {
            Text(
                text = stringResource(R.string.library_menu_title),
                color = tokens.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick actions: Rescan & Multi-Select
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRescan,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("library_rescan_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = tokens.accentPrimary
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = tokens.accentPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_rescan),
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onStartMultiSelect,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("library_multiselect_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckBox, contentDescription = null, tint = tokens.accentPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_select),
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = tokens.glassBorder.copy(alpha = 0.4f))

                // Categories (formerly top ScrollableTabRow)
                Text(
                    text = stringResource(R.string.library_category),
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabTitles.forEachIndexed { idx, title ->
                        val isSelected = selectedTab == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTab(idx) },
                            leadingIcon = {
                                Icon(
                                    imageVector = tabIcons.getOrElse(idx) { Icons.Rounded.LibraryMusic },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(title, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.accentPrimary.copy(alpha = 0.24f),
                                selectedLabelColor = tokens.accentPrimary,
                                selectedLeadingIconColor = tokens.accentPrimary
                            ),
                            modifier = Modifier.testTag("library_tab_$idx")
                        )
                    }
                }

                // Format filters (formerly horizontal FilterChip row)
                if (availableFormats.size > 2) {
                    HorizontalDivider(color = tokens.glassBorder.copy(alpha = 0.4f))
                    Text(
                        text = stringResource(R.string.action_filter_format),
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableFormats.forEach { fmt ->
                            val isAll = fmt == "ALL"
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { onSelectFormat(fmt) },
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

                HorizontalDivider(color = tokens.glassBorder.copy(alpha = 0.4f))

                // Sort options (formerly top-bar Sort DropdownMenu)
                Text(
                    text = stringResource(R.string.action_sort),
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
                val sortItems = listOf(
                    SortOption.TITLE to R.string.sort_title,
                    SortOption.ARTIST to R.string.sort_artist,
                    SortOption.DATE_ADDED to R.string.sort_date_added,
                    SortOption.DURATION to R.string.sort_duration
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortItems.forEach { (opt, labelRes) ->
                        FilterChip(
                            selected = selectedSort == opt,
                            onClick = { onSelectSort(opt) },
                            label = { Text(stringResource(labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.accentViolet.copy(alpha = 0.24f),
                                selectedLabelColor = tokens.accentViolet
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_done),
                    color = tokens.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
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
    onRenameSong: (Long, String) -> Unit = { _, _ -> },
    onRemoveFromPlaylist: ((Long) -> Unit)? = null,
    onExcludeFolder: (() -> Unit)? = null,
    onRemoveFromLibrary: (Long) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalKarlukTokens.current
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var deleteTargetSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var detailSong by remember { mutableStateOf<Song?>(null) }
    var renameTargetSong by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            KarlukTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (canExcludeFolder && onExcludeFolder != null) {
                        Surface(
                            onClick = { onExcludeFolder(); onBack() },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFF6E8A).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFFFF6E8A).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("hide_folder_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = stringResource(R.string.files_exclude_folder),
                                    tint = Color(0xFFFF6E8A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.files_exclude_folder),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFF6E8A),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
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
            onConfirm = {
                if (playlistId != null && onRemoveFromPlaylist != null) {
                    onRemoveFromPlaylist(song.id)
                } else {
                    onRemoveFromLibrary(song.id)
                }
            },
            onDismiss = { deleteTargetSong = null }
        )
    }
}
