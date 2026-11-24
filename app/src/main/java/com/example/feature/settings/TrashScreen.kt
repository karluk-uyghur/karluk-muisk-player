package com.example.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.data.database.TrashItemEntity

@Composable
fun TrashScreen(
    trashItems: List<TrashItemEntity>,
    onBack: () -> Unit,
    onRestoreItem: (TrashItemEntity) -> Unit,
    onDeleteForeverItem: (TrashItemEntity) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var pendingPermanentItem by remember { mutableStateOf<TrashItemEntity?>(null) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(
                    title = stringResource(R.string.trash_title),
                    onBack = onBack,
                    actions = {
                        if (trashItems.isNotEmpty()) {
                            TextButton(
                                onClick = { showEmptyConfirm = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("trash_empty_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.trash_empty_action),
                                    tint = Color(0xFFFF5E7E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.trash_empty_action),
                                    color = Color(0xFFFF5E7E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )

                if (trashItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            icon = Icons.Rounded.DeleteOutline,
                            title = stringResource(R.string.trash_empty_title),
                            description = stringResource(R.string.trash_empty_desc)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.trash_subtitle_count, trashItems.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.textSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        items(trashItems, key = { it.trashId }) { item ->
                            val isFolder = item.itemType == "FOLDER"
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trash_item_${item.trashId}"),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isFolder) Icons.Rounded.Folder else Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = if (isFolder) tokens.emerald else tokens.accentPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (!isFolder && item.formatLabel.isNotBlank()) {
                                                FormatBadge(formatLabel = item.formatLabel)
                                            }
                                            val metaText = if (!isFolder && item.fileSize > 0L) {
                                                "${item.subtitle} • ${FormatUtils.formatFileSize(item.fileSize)}"
                                            } else {
                                                item.subtitle
                                            }
                                            Text(
                                                text = metaText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = tokens.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRestoreItem(item) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("trash_restore_${item.trashId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestoreFromTrash,
                                            contentDescription = stringResource(R.string.trash_restore),
                                            tint = tokens.accentPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { pendingPermanentItem = item },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("trash_delete_forever_${item.trashId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteForever,
                                            contentDescription = stringResource(R.string.trash_delete_forever),
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

    pendingPermanentItem?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingPermanentItem = null },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = {
                Text(
                    text = stringResource(R.string.trash_confirm_permanent),
                    color = tokens.textPrimary
                )
            },
            text = {
                Text(
                    text = "${target.title}\n\n${stringResource(R.string.trash_confirm_permanent_desc)}",
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteForeverItem(target)
                        pendingPermanentItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5E7E),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.trash_delete_forever), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentItem = null }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = {
                Text(
                    text = stringResource(R.string.trash_confirm_permanent),
                    color = tokens.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.trash_confirm_permanent_desc),
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyTrash()
                        showEmptyConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5E7E),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.trash_empty_action), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }
}
