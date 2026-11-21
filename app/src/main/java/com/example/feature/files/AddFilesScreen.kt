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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.data.database.ExcludedFolderEntity
import com.example.data.database.SafFolderEntity
import com.example.domain.model.FolderItem
import com.example.domain.model.StorageStats

@Composable
fun AddFilesScreen(
    safFolders: List<SafFolderEntity>,
    excludedFolders: List<ExcludedFolderEntity>,
    deviceFolders: List<FolderItem>,
    storageStats: StorageStats,
    isScanning: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRescanMedia: () -> Unit,
    onImportAudioFiles: (List<Uri>) -> Unit,
    onAddSafFolder: (Uri) -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onRestoreExcludedFolder: (String) -> Unit,
    onOpenFolderDetail: (FolderItem) -> Unit,
    onOpenStorageInfo: () -> Unit
) {
    val tokens = LocalKarlukTokens.current

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
                    IconButton(
                        onClick = onOpenStorageInfo,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("files_storage_btn")
                    ) {
                        Icon(Icons.Rounded.Storage, contentDescription = stringResource(R.string.storage_title), tint = tokens.accentPrimary)
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

                // Storage Quick Summary
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        onClick = onOpenStorageInfo
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Storage, contentDescription = null, tint = tokens.accentPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.storage_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                            }
                            Text(
                                text = "${stringResource(R.string.home_tracks_count, storageStats.totalTracks)} • ${FormatUtils.formatFileSize(storageStats.totalSizeBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.accentPrimary
                            )
                        }
                    }
                }

                // Added SAF Folders
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
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                IconButton(
                                    onClick = { onRemoveSafFolder(folder.treeUri) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.action_remove), tint = Color(0xFFFF5E7E))
                                }
                            }
                        }
                    }
                }

                // Device Folders Browser
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
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { onOpenFolderDetail(folder) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
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
