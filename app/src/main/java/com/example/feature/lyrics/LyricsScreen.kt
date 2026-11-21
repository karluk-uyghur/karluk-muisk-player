package com.example.feature.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Lyrics
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.EmptyStateCard
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.domain.model.LrcParser
import com.example.domain.model.LyricsData
import com.example.domain.model.PlaybackState

@Composable
fun LyricsScreen(
    playbackState: PlaybackState,
    lyricsData: LyricsData?,
    onBack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (Long, String) -> Unit,
    onImportLrcFile: (Long, Uri) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val song = playbackState.currentSong
    var showEditDialog by remember { mutableStateOf(false) }

    val lrcPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && song != null) {
            onImportLrcFile(song.id, uri)
        }
    }

    val listState = rememberLazyListState()
    val lines = lyricsData?.lines.orEmpty()
    val activeIdx = remember(lines, playbackState.positionMs, lyricsData?.isSynced) {
        if (lyricsData?.isSynced == true) LrcParser.findActiveLineIndex(lines, playbackState.positionMs) else -1
    }

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && activeIdx < lines.size) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
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
                KarlukTopBar(
                    title = song?.title ?: stringResource(R.string.lyrics_title),
                    onBack = onBack,
                    actions = {
                        if (song != null) {
                            IconButton(
                                onClick = { lrcPicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("lyrics_import_btn")
                            ) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = stringResource(R.string.lyrics_import_lrc), tint = tokens.accentPrimary)
                            }
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("lyrics_edit_btn")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.lyrics_edit), tint = tokens.textPrimary)
                            }
                        }
                    }
                )

                if (song == null || lines.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EmptyStateCard(
                                icon = Icons.Rounded.Lyrics,
                                title = stringResource(R.string.lyrics_empty_title),
                                description = stringResource(R.string.lyrics_empty_desc)
                            )
                            if (song != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Button(
                                        onClick = { lrcPicker.launch(arrayOf("*/*")) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = tokens.accentPrimary,
                                            contentColor = Color(0xFF031126)
                                        ),
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                    ) {
                                        Text(stringResource(R.string.lyrics_import_lrc), fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showEditDialog = true },
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                    ) {
                                        Text(stringResource(R.string.lyrics_edit), color = tokens.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(lines) { idx, line ->
                            val isActive = idx == activeIdx
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = line.timestampMs >= 0L) {
                                        if (line.timestampMs >= 0L) onSeekTo(line.timestampMs)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                highlighted = isActive
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (line.timestampMs >= 0L) {
                                        Text(
                                            text = FormatUtils.formatDuration(line.timestampMs),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isActive) tokens.accentPrimary else tokens.textSecondary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(
                                        text = line.text,
                                        style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                        color = if (isActive) tokens.accentPrimary else tokens.textPrimary,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && song != null) {
        var text by remember(song.id, lyricsData?.rawContent) {
            mutableStateOf(lyricsData?.rawContent.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = { Text(stringResource(R.string.lyrics_edit), color = tokens.textPrimary) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.lyrics_hint)) },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveLyrics(song.id, text)
                    showEditDialog = false
                }) {
                    Text(stringResource(R.string.action_save), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                }
            }
        )
    }
}
