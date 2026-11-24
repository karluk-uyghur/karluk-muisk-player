package com.example.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.FormatUtils
import com.example.domain.model.AppLanguage
import com.example.domain.model.Song
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun localizedArtistName(raw: String): String {
    return if (raw.isBlank() || raw == "Unknown Artist" || raw == "<unknown>") {
        stringResource(R.string.unknown_artist)
    } else {
        raw
    }
}

@Composable
fun localizedAlbumName(raw: String): String {
    return if (raw.isBlank() || raw == "Unknown Album" || raw == "<unknown>") {
        stringResource(R.string.unknown_album)
    } else {
        raw
    }
}

@Composable
fun Song.localizedArtist(): String = localizedArtistName(artist)

@Composable
fun Song.localizedAlbum(): String = localizedAlbumName(album)

@Composable
fun DeleteConfirmDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
        title = {
            Text(
                text = stringResource(R.string.action_delete_confirm_title),
                color = tokens.textPrimary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.action_delete_confirm_msg, itemTitle),
                color = tokens.textSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_delete_btn")
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = Color(0xFFFF5E7E),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = tokens.textSecondary
                )
            }
        }
    )
}

@Composable
fun KarlukBackground(
    modifier: Modifier = Modifier,
    overrideBackgroundUri: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val activeBgUri = overrideBackgroundUri?.takeIf { it.isNotBlank() } ?: tokens.customBackgroundUri
    val hasCustomBg = !activeBgUri.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(tokens.bgTop, tokens.bgMiddle, tokens.bgBottom)
                    )
                )
                if (!tokens.lowPowerMode && !hasCustomBg) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.accentPrimary.copy(alpha = if (tokens.isLightTheme) 0.14f else 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.65f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.14f),
                        radius = size.width * 0.65f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.accentViolet.copy(alpha = if (tokens.isLightTheme) 0.10f else 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.72f),
                            radius = size.width * 0.7f
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.72f),
                        radius = size.width * 0.7f
                    )
                }
            }
    ) {
        if (hasCustomBg) {
            AsyncImage(
                model = activeBgUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (tokens.isLightTheme) {
                                listOf(
                                    Color(0xFFEEF4FF).copy(alpha = 0.56f),
                                    Color(0xFFDCE8FC).copy(alpha = 0.48f),
                                    Color(0xFFCADBFA).copy(alpha = 0.58f)
                                )
                            } else {
                                listOf(
                                    Color(0xFF040B1F).copy(alpha = 0.58f),
                                    Color(0xFF081840).copy(alpha = 0.44f),
                                    Color(0xFF040B1F).copy(alpha = 0.64f)
                                )
                            }
                        )
                    )
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val borderBrush = Brush.linearGradient(
        colors = if (highlighted) {
            listOf(tokens.accentPrimary.copy(alpha = 0.85f), tokens.accentViolet.copy(alpha = 0.55f))
        } else {
            listOf(tokens.glassBorder, tokens.glassBorder.copy(alpha = 0.06f))
        }
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = if (highlighted) tokens.glassSurfaceElevated else tokens.glassSurface,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = 0.dp
    ) {
        Box(content = content)
    }
}

@Composable
fun KarlukTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val tokens = LocalKarlukTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = tokens.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = actions
        )
    }
}

@Composable
fun GlowingPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    size: Dp = 68.dp,
    testTag: String = "play_pause_button"
) {
    val tokens = LocalKarlukTokens.current
    Box(
        modifier = Modifier
            .size(size + 14.dp)
            .drawBehind {
                if (!tokens.lowPowerMode) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.accentPrimary.copy(alpha = if (isPlaying) 0.45f else 0.25f),
                                Color.Transparent
                            )
                        ),
                        radius = this.size.minDimension / 2f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(10.dp, CircleShape, ambientColor = tokens.accentPrimary, spotColor = tokens.accentPrimary)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(tokens.accentSecondary, tokens.accentPrimary)
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun SpinningVinylArtwork(
    artworkUri: String?,
    isPlaying: Boolean,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier,
    customBackgroundUri: String? = null
) {
    val tokens = LocalKarlukTokens.current
    val spinAnim = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                val startAngle = spinAnim.value % 360f
                spinAnim.snapTo(startAngle)
                spinAnim.animateTo(
                    targetValue = startAngle + 360f,
                    animationSpec = tween(durationMillis = 8500, easing = LinearEasing)
                )
            }
        }
    }

    val effectiveAngle = spinAnim.value
    val hasCustomDiscBg = !customBackgroundUri.isNullOrBlank()
    var artworkFailed by remember(artworkUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size + 26.dp)
            .drawBehind {
                if (!tokens.lowPowerMode) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.accentPrimary.copy(alpha = 0.34f),
                                tokens.accentViolet.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        ),
                        radius = this.size.minDimension / 2f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasCustomDiscBg) {
            Box(
                modifier = Modifier
                    .size(size)
                    .rotate(effectiveAngle)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(tokens.accentSecondary, tokens.accentViolet)
                        )
                    )
                    .border(2.5.dp, tokens.accentPrimary.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = customBackgroundUri,
                    contentDescription = stringResource(R.string.now_playing_custom_bg),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Spinning highlight ring & center spindle so motion is clear
                Box(
                    modifier = Modifier
                        .size(size * 0.88f)
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(size * 0.16f)
                        .clip(CircleShape)
                        .background(Color(0xFF071128).copy(alpha = 0.82f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .rotate(effectiveAngle)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF0A142D),
                                Color(0xFF284F9E),
                                Color(0xFF070F26),
                                Color(0xFF3364BA),
                                Color(0xFF0A142D)
                            )
                        )
                    )
                    .border(2.5.dp, tokens.accentPrimary.copy(alpha = 0.78f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Concentric vinyl grooves with subtle shiny spot
                Box(
                    modifier = Modifier
                        .size(size * 0.86f)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(size * 0.72f)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                )
                // Visible marker notch on outer vinyl ring so rotation is clearly visible even without album art
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(tokens.accentPrimary.copy(alpha = 0.88f))
                )

                // Center album art or music emblem
                Box(
                    modifier = Modifier
                        .size(size * 0.56f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(tokens.accentSecondary, tokens.accentViolet)
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!artworkUri.isNullOrBlank() && !artworkFailed) {
                        AsyncImage(
                            model = artworkUri,
                            contentDescription = "Album artwork",
                            contentScale = ContentScale.Crop,
                            onError = { artworkFailed = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.28f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpectrumVisualizerCanvas(
    spectrum: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val visualBarCount = 28
    val peaks = remember { FloatArray(visualBarCount) { 0.08f } }

    Canvas(modifier = modifier) {
        val rawCount = spectrum.size.coerceAtLeast(1)
        val totalWidth = size.width
        val maxHeight = size.height
        if (totalWidth <= 0f || maxHeight <= 0f) return@Canvas

        val slotWidth = totalWidth / visualBarCount
        val barWidth = (slotWidth * 0.56f).coerceIn(4f, 14f)
        val dotRadius = (barWidth * 0.48f).coerceIn(2f, 5f)
        val minBarHeight = 5f

        for (i in 0 until visualBarCount) {
            val mappedPos = (i.toFloat() / (visualBarCount - 1).coerceAtLeast(1)) * (rawCount - 1)
            val lowIdx = mappedPos.toInt().coerceIn(0, rawCount - 1)
            val highIdx = (lowIdx + 1).coerceIn(0, rawCount - 1)
            val frac = mappedPos - lowIdx
            val interpolated = spectrum[lowIdx] * (1f - frac) + spectrum[highIdx] * frac

            // Gentle center-weighted lift when audio is active so outer and inner bars both dance smoothly
            val energy = if (isPlaying) {
                interpolated.coerceIn(0.08f, 1f)
            } else {
                0.08f
            }

            if (energy >= peaks[i]) {
                peaks[i] = energy
            } else {
                peaks[i] = (peaks[i] - 0.018f).coerceAtLeast(energy)
            }

            val barHeight = (maxHeight * 0.84f * energy).coerceAtLeast(minBarHeight)
            val x = i * slotWidth + (slotWidth - barWidth) / 2f
            val y = maxHeight - barHeight

            val ratio = i.toFloat() / (visualBarCount - 1).coerceAtLeast(1)
            val topColor = Color(
                red = tokens.accentPrimary.red * (1f - ratio) + tokens.accentViolet.red * ratio,
                green = tokens.accentPrimary.green * (1f - ratio) + tokens.accentViolet.green * ratio,
                blue = tokens.accentPrimary.blue * (1f - ratio) + tokens.accentViolet.blue * ratio,
                alpha = if (isPlaying) 0.96f else 0.34f
            )
            val baseColor = tokens.accentSecondary.copy(alpha = if (isPlaying) 0.55f else 0.18f)

            // Soft glow halo behind active bar
            if (isPlaying && !tokens.lowPowerMode && energy > 0.18f) {
                drawRoundRect(
                    color = topColor.copy(alpha = 0.22f),
                    topLeft = Offset(x - 1.5f, (y - 2f).coerceAtLeast(0f)),
                    size = Size(barWidth + 3f, (barHeight + 2f).coerceAtMost(maxHeight)),
                    cornerRadius = CornerRadius(barWidth, barWidth)
                )
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, baseColor),
                    startY = y,
                    endY = maxHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Floating peak cap dot above bar
            if (isPlaying) {
                val peakY = (maxHeight - (maxHeight * 0.84f * peaks[i]).coerceAtLeast(minBarHeight) - dotRadius * 1.9f)
                    .coerceIn(dotRadius, maxHeight - minBarHeight)
                drawCircle(
                    color = Color.White.copy(alpha = 0.78f),
                    radius = dotRadius * 0.75f,
                    center = Offset(x + barWidth / 2f, peakY)
                )
            }
        }
    }
}

@Composable
fun FormatBadge(
    formatLabel: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val badgeColor = when (formatLabel.uppercase()) {
        "FLAC", "WAV" -> tokens.emerald
        "OPUS", "OGG" -> tokens.accentViolet
        else -> tokens.accentPrimary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .border(0.8.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = formatLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = badgeColor,
            maxLines = 1
        )
    }
}

@Composable
fun SongArtworkThumb(
    artworkUri: String?,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    var imageFailed by remember(artworkUri) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(tokens.accentSecondary.copy(alpha = 0.55f), tokens.accentViolet.copy(alpha = 0.55f))
                )
            )
            .border(1.dp, tokens.glassBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUri.isNullOrBlank() && !imageFailed) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { imageFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(16.dp),
        highlighted = isCurrent || isSelected,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = tokens.accentPrimary)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            SongArtworkThumb(
                artworkUri = song.customBackgroundUri?.takeIf { it.isNotBlank() } ?: song.artworkUri,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormatBadge(formatLabel = song.formatLabel)
                    Text(
                        text = FormatUtils.formatDuration(song.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent && isPlaying) tokens.accentPrimary else tokens.textSecondary
                    )
                    Text(
                        text = song.localizedArtist(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.home_quick_favorites),
                    tint = if (song.isFavorite) Color(0xFFFF4B7A) else tokens.textSecondary
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("song_more_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.files_details),
                    tint = tokens.textSecondary
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_player_bar"),
        shape = RoundedCornerShape(18.dp),
        highlighted = true,
        onClick = onOpenNowPlaying
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtworkThumb(
                    artworkUri = song.customBackgroundUri?.takeIf { it.isNotBlank() } ?: song.artworkUri,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.localizedArtist()} • ${FormatUtils.formatDuration(positionMs)} / ${FormatUtils.formatDuration(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = tokens.accentPrimary
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = tokens.textPrimary
                    )
                }

                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_queue")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.queue_title),
                        tint = tokens.textSecondary
                    )
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = tokens.accentPrimary,
                    trackColor = tokens.glassBorder.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(tokens.accentPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.accentPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accentPrimary,
                        contentColor = Color(0xFF031126)
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(text = actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KarlukLogoImage(
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    Image(
        painter = painterResource(id = R.drawable.karluk_logo),
        contentDescription = stringResource(R.string.app_name),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .border(1.5.dp, tokens.accentPrimary.copy(alpha = 0.55f), RoundedCornerShape(size * 0.26f))
    )
}

@Composable
fun LanguageFlagBadge(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val shape = RoundedCornerShape(5.dp)
    if (language == AppLanguage.ARABIC) {
        Image(
            painter = painterResource(id = R.drawable.flag_saudi),
            contentDescription = AppLanguage.ARABIC.nativeName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(width = 32.dp, height = 22.dp)
                .clip(shape)
                .border(0.8.dp, tokens.glassBorderBright, shape)
        )
        return
    }

    Canvas(
        modifier = modifier
            .size(width = 32.dp, height = 22.dp)
            .clip(shape)
            .border(0.8.dp, tokens.glassBorderBright, shape)
    ) {
        val w = size.width
        val h = size.height
        when (language) {
            AppLanguage.UYGHUR -> {
                val skyBlue = Color(0xFF0888F3)
                drawRect(color = skyBlue)
                drawCrescentAndStar(
                    w = w,
                    h = h,
                    bgColor = skyBlue,
                    fgColor = Color.White
                )
            }

            AppLanguage.TURKISH -> {
                val red = Color(0xFFE30A17)
                drawRect(color = red)
                drawCrescentAndStar(
                    w = w,
                    h = h,
                    bgColor = red,
                    fgColor = Color.White
                )
            }

            AppLanguage.ARABIC -> Unit

            AppLanguage.ENGLISH -> {
                // Crisp Union Jack
                drawRect(color = Color(0xFF012169))
                val diagWhite = h * 0.22f
                val diagRed = h * 0.11f
                drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = diagWhite)
                drawLine(Color.White, Offset(w, 0f), Offset(0f, h), strokeWidth = diagWhite)
                drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(w, h), strokeWidth = diagRed)
                drawLine(Color(0xFFC8102E), Offset(w, 0f), Offset(0f, h), strokeWidth = diagRed)
                // Central white cross
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, h * 0.34f),
                    size = Size(w, h * 0.32f)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.39f, 0f),
                    size = Size(w * 0.22f, h)
                )
                // Central red cross
                drawRect(
                    color = Color(0xFFC8102E),
                    topLeft = Offset(0f, h * 0.41f),
                    size = Size(w, h * 0.18f)
                )
                drawRect(
                    color = Color(0xFFC8102E),
                    topLeft = Offset(w * 0.435f, 0f),
                    size = Size(w * 0.13f, h)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrescentAndStar(
    w: Float,
    h: Float,
    bgColor: Color,
    fgColor: Color
) {
    val outerCx = w * 0.39f
    val cy = h * 0.50f
    val outerR = h * 0.29f
    drawCircle(
        color = fgColor,
        radius = outerR,
        center = Offset(outerCx, cy)
    )
    drawCircle(
        color = bgColor,
        radius = h * 0.232f,
        center = Offset(outerCx + h * 0.08f, cy)
    )

    val starCx = w * 0.67f
    val starOuterR = h * 0.145f
    val starInnerR = starOuterR * 0.39f
    val starPath = androidx.compose.ui.graphics.Path()
    for (i in 0 until 10) {
        val angleRad = (-90.0 + i * 36.0) * PI / 180.0
        val r = if (i % 2 == 0) starOuterR else starInnerR
        val x = starCx + (r * cos(angleRad)).toFloat()
        val y = cy + (r * sin(angleRad)).toFloat()
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()
    drawPath(path = starPath, color = fgColor)
}

@Composable
fun KarlukSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val tokens = LocalKarlukTokens.current
    val pillShape = RoundedCornerShape(50)

    val trackBg by animateColorAsState(
        targetValue = when {
            !enabled -> tokens.glassSurface.copy(alpha = 0.25f)
            checked -> tokens.accentPrimary.copy(alpha = 0.20f)
            tokens.isLightTheme -> Color(0xFFD5E2FA)
            else -> Color(0xFF081430)
        },
        animationSpec = tween(durationMillis = 220),
        label = "switch_track"
    )
    val frameBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> tokens.glassBorder.copy(alpha = 0.35f)
            checked -> tokens.accentPrimary
            else -> tokens.textSecondary.copy(alpha = 0.65f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "switch_border"
    )
    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> tokens.textSecondary.copy(alpha = 0.45f)
            checked -> tokens.accentPrimary
            else -> tokens.textSecondary
        },
        animationSpec = tween(durationMillis = 220),
        label = "switch_thumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switch_offset"
    )

    // Keep visual slide orientation predictable (LTR inside frame: Off = left, On = right)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .clip(pillShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Switch,
                    onClick = { onCheckedChange(!checked) }
                )
                .padding(horizontal = 2.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 30.dp)
                    .clip(pillShape)
                    .background(trackBg)
                    .border(width = 1.6.dp, color = frameBorderColor, shape = pillShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(thumbColor)
                        .border(
                            width = 1.4.dp,
                            color = if (checked && enabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.30f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
