package com.example.feature.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.DeleteConfirmDialog
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.data.database.ExcludedFolderEntity
import com.example.data.database.SafFolderEntity
import com.example.domain.model.FolderItem
import com.example.domain.model.Song
import com.example.domain.model.StorageStats

@Composable
fun AddFilesScreen(
    safFolders: List<SafFolderEntity>,
    excludedFolders: List<ExcludedFolderEntity>,
    deviceFolders: List<FolderItem>,
    importedSongs: List<Song> = emptyList(),
    storageStats: StorageStats,
    isScanning: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRescanMedia: () -> Unit,
    onImportAudioFiles: (List<Uri>) -> Unit,
    onAddSafFolder: (Uri) -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onRemoveAllSafFolders: () -> Unit = {},
    onExcludeFolder: (String, String) -> Unit = { _, _ -> },
    onRestoreExcludedFolder: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit = { _, _ -> },
    onRemoveImportedSong: (Long) -> Unit = {},
    onRemoveAllImportedSongs: (List<Long>) -> Unit = {},
    onOpenFolderDetail: (FolderItem) -> Unit,
    onOpenStorageInfo: () -> Unit,
    filteredMusicCount: Int = 0,
    onOpenFilteredMusic: () -> Unit = {}
) {
    val tokens = LocalKarlukTokens.current
    var topMenuOpen by remember { mutableStateOf(false) }
    var deleteSongTarget by remember { mutableStateOf<Song?>(null) }

    val audioFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportAudioFiles(uris)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onAddSafFolder(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            KarlukTopBar(
                title = stringResource(R.string.files_title),
                actions = {
                    if (safFolders.isNotEmpty() || importedSongs.isNotEmpty()) {
                        Box {
                            IconButton(
                                onClick = { topMenuOpen = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("files_top_more_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.files_more_options),
                                    tint = tokens.textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = topMenuOpen,
                                onDismissRequest = { topMenuOpen = false }
                            ) {
                                if (safFolders.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.files_clear_added_folders),
                                                color = Color(0xFFFF5E7E)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFFF5E7E)
                                            )
                                        },
                                        onClick = {
                                            topMenuOpen = false
                                            onRemoveAllSafFolders()
                                        }
                                    )
                                }
                                if (importedSongs.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.action_delete),
                                                color = Color(0xFFFF5E7E)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFFF5E7E)
                                            )
                                        },
                                        onClick = {
                                            topMenuOpen = false
                                            onRemoveAllImportedSongs(importedSongs.map { it.id })
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Supported Formats Banner
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.files_supported_formats),
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.textPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("MP3", "OPUS", "AAC", "M4A", "FLAC", "WAV", "OGG").forEach { fmt ->
                                    FormatBadge(formatLabel = fmt)
                                }
                            }
                        }
                    }
                }

                // Primary Import Actions
                item {
                    ActionImportCard(
                        icon = Icons.Rounded.AudioFile,
                        title = stringResource(R.string.files_pick_audio),
                        subtitle = stringResource(R.string.files_pick_audio_desc),
                        accent = tokens.accentPrimary,
                        onClick = {
                            audioFilesLauncher.launch(
                                arrayOf(
                                    "audio/*",
                                    "application/ogg",
                                    "application/x-flac",
                                    "application/octet-stream"
                                )
                            )
                        },
                        modifier = Modifier.testTag("pick_audio_files_card")
                    )
                }

                item {
                    ActionImportCard(
                        icon = Icons.Rounded.CreateNewFolder,
                        title = stringResource(R.string.files_pick_folder),
                        subtitle = stringResource(R.string.files_pick_folder_desc),
                        accent = tokens.emerald,
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.testTag("pick_saf_folder_card")
                    )
                }

                item {
                    ActionImportCard(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(R.string.files_scan_device),
                        subtitle = if (hasAudioPermission) {
                            stringResource(R.string.files_scan_device_desc)
                        } else {
                            stringResource(R.string.permission_denied_desc)
                        },
                        accent = tokens.accentViolet,
                        isLoading = isScanning,
                        onClick = {
                            if (hasAudioPermission) onRescanMedia() else onRequestPermission()
                        },
                        modifier = Modifier.testTag("scan_device_media_card")
                    )
                }

                // Auto-created "Filtered Music" (مۇزىكىلار) Folder Card
                if (filteredMusicCount > 0) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("files_filtered_music_folder_card"),
                            shape = RoundedCornerShape(18.dp),
                            highlighted = true,
                            onClick = onOpenFilteredMusic
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicOff,
                                    contentDescription = null,
                                    tint = tokens.accentViolet,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.filtered_music_folder_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.filtered_music_folder_desc, filteredMusicCount),
                                        style = MaterialTheme.typography.bodySmall,
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

                // Added Individual Nasheed Files with 3-dots Delete menu
                if (importedSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.files_imported_files),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(importedSongs, key = { "imp_${it.id}" }) { song ->
                        var menuOpen by remember { mutableStateOf(false) }
                        val idx = importedSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { onPlaySong(importedSongs, idx) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AudioFile, contentDescription = null, tint = tokens.accentPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FormatBadge(song.formatLabel)
                                        Text(
                                            text = FormatUtils.formatDuration(song.duration),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.textSecondary
                                        )
                                    }
                                }
                                Box {
                                    IconButton(
                                        onClick = { menuOpen = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("imported_file_more_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(R.string.files_more_options),
                                            tint = tokens.textSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_play)) },
                                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = tokens.accentPrimary) },
                                            onClick = {
                                                menuOpen = false
                                                onPlaySong(importedSongs, idx)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.action_delete),
                                                    color = Color(0xFFFF5E7E)
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF5E7E)
                                                )
                                            },
                                            onClick = {
                                                menuOpen = false
                                                deleteSongTarget = song
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Added SAF Folders with 3-dots Delete menu
                if (safFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.files_added_folders),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(safFolders, key = { it.treeUri }) { folder ->
                        var menuOpen by remember { mutableStateOf(false) }
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = tokens.emerald)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    IconButton(
                                        onClick = { menuOpen = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("saf_folder_more_${folder.treeUri.hashCode()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(R.string.files_more_options),
                                            tint = tokens.textSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.action_delete),
                                                    color = Color(0xFFFF5E7E)
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = stringResource(R.string.action_delete),
                                                    tint = Color(0xFFFF5E7E)
                                                )
                                            },
                                            onClick = {
                                                menuOpen = false
                                                onRemoveSafFolder(folder.treeUri)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Device Folders Browser with 3-dots Delete / Hide menu
                if (deviceFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.tab_folders),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(deviceFolders, key = { it.path }) { folder ->
                        var menuOpen by remember { mutableStateOf(false) }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { onOpenFolderDetail(folder) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = tokens.accentPrimary)
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
                                Box {
                                    IconButton(
                                        onClick = { menuOpen = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("device_folder_more_${folder.path.hashCode()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(R.string.files_more_options),
                                            tint = tokens.textSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.action_delete),
                                                    color = Color(0xFFFF5E7E)
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = stringResource(R.string.action_delete),
                                                    tint = Color(0xFFFF5E7E)
                                                )
                                            },
                                            onClick = {
                                                menuOpen = false
                                                onExcludeFolder(folder.path, folder.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Excluded Folders
                if (excludedFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.files_excluded_folders),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(excludedFolders, key = { it.folderPath }) { folder ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.FolderOff, contentDescription = null, tint = tokens.textSecondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folder.folderName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = tokens.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onRestoreExcludedFolder(folder.folderPath) }) {
                                    Icon(Icons.Rounded.Restore, contentDescription = null, tint = tokens.accentPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.files_restore_folder), color = tokens.accentPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteSongTarget?.let { target ->
        DeleteConfirmDialog(
            itemTitle = target.title,
            onConfirm = { onRemoveImportedSong(target.id) },
            onDismiss = { deleteSongTarget = null }
        )
    }
}

@Composable
private fun ActionImportCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    GlassCard(
        modifier = modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(20.dp),
        highlighted = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.5.dp, color = accent)
            } else {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = tokens.textSecondary)
            }
        }
    }
}
