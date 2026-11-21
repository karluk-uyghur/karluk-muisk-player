package com.example.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.FormatUtils
import com.example.domain.model.Song

@Composable
fun KarlukBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalKarlukTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(tokens.bgTop, tokens.bgMiddle, tokens.bgBottom)
                    )
                )
                if (!tokens.lowPowerMode) {
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
            },
        content = content
    )
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
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    val effectiveAngle = if (isPlaying && !tokens.reducedMotion) angle else 0f

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
        Box(
            modifier = Modifier
                .size(size)
                .rotate(effectiveAngle)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF0A142D),
                            Color(0xFF1A3168),
                            Color(0xFF070F26),
                            Color(0xFF1B3A7A),
                            Color(0xFF0A142D)
                        )
                    )
                )
                .border(2.5.dp, tokens.accentPrimary.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Subtle vinyl grooves
            Box(
                modifier = Modifier
                    .size(size * 0.86f)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(size * 0.72f)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
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
                    .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = "Album artwork",
                        contentScale = ContentScale.Crop,
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

@Composable
fun SpectrumVisualizerCanvas(
    spectrum: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    Canvas(modifier = modifier) {
        val count = spectrum.size.coerceAtLeast(1)
        val totalWidth = size.width
        val slotWidth = totalWidth / count
        val barWidth = (slotWidth * 0.58f).coerceAtLeast(4f)
        val maxHeight = size.height

        for (i in 0 until count) {
            val energy = if (isPlaying) spectrum[i].coerceIn(0.05f, 1f) else 0.06f
            val barHeight = (maxHeight * energy).coerceAtLeast(4f)
            val x = i * slotWidth + (slotWidth - barWidth) / 2f
            val y = maxHeight - barHeight
            val ratio = i.toFloat() / (count - 1).coerceAtLeast(1)
            val barColor = Color(
                red = tokens.accentPrimary.red * (1f - ratio) + tokens.accentViolet.red * ratio,
                green = tokens.accentPrimary.green * (1f - ratio) + tokens.accentViolet.green * ratio,
                blue = tokens.accentPrimary.blue * (1f - ratio) + tokens.accentViolet.blue * ratio,
                alpha = if (isPlaying) 0.92f else 0.35f
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
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
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = tokens.accentPrimary)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            SongArtworkThumb(artworkUri = song.artworkUri, size = 50.dp)

            Spacer(modifier = Modifier.width(12.dp))

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
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = FormatUtils.formatDuration(song.duration),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent && isPlaying) tokens.accentPrimary else tokens.textSecondary
            )

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) Color(0xFFFF4B7A) else tokens.textSecondary
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
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
                SongArtworkThumb(artworkUri = song.artworkUri, size = 44.dp)
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
                        text = "${song.artist} • ${FormatUtils.formatDuration(positionMs)} / ${FormatUtils.formatDuration(durationMs)}",
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
                        contentDescription = "Queue",
                        tint = tokens.textSecondary
                    )
                }
            }
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
