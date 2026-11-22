package com.example.feature.splash

import android.graphics.Matrix
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukLogoImage
import com.example.core.designsystem.LocalKarlukTokens
import com.example.domain.model.AppLanguage
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    skipIntroOnce: Boolean = false,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val latestCompleted by rememberUpdatedState(onboardingCompleted)
    var didNavigate by remember { mutableStateOf(false) }

    fun navigateNext() {
        if (didNavigate) return
        didNavigate = true
        if (latestCompleted) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    if (skipIntroOnce) {
        LaunchedEffect(Unit) {
            navigateNext()
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.karluk_intro}")
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    var latestVideoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var visibleVideoFractionH by remember { mutableFloatStateOf(1f) }

    fun applyPhoneAdaptedScale(textureView: TextureView, videoSize: VideoSize) {
        val viewWidth = textureView.width.toFloat()
        val viewHeight = textureView.height.toFloat()
        val videoWidth = videoSize.width.toFloat()
        val videoHeight = videoSize.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0f || videoHeight <= 0f) return

        // On portrait phone screens, adapt the 16:9 intro animation cleanly to fill the central phone viewport
        // without cropping the logo/title, while seamlessly feathering edges into pure black.
        val isPortraitScreen = viewHeight > viewWidth
        val fitWidthScale = viewWidth / videoWidth
        val fitHeightScale = viewHeight / videoHeight
        val scale = if (isPortraitScreen && videoWidth > videoHeight) {
            minOf(fitHeightScale, fitWidthScale * 1.48f)
        } else {
            maxOf(fitWidthScale, fitHeightScale)
        }

        val scaledWidth = videoWidth * scale
        val scaledHeight = videoHeight * scale
        visibleVideoFractionH = (scaledHeight / viewHeight).coerceIn(0f, 1f)

        val matrix = Matrix()
        matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, viewWidth / 2f, viewHeight / 2f)
        textureView.setTransform(matrix)
    }

    val textureView = remember(context) {
        TextureView(context)
    }

    DisposableEffect(exoPlayer, textureView) {
        exoPlayer.setVideoTextureView(textureView)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    navigateNext()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                latestVideoSize = videoSize
                applyPhoneAdaptedScale(textureView, videoSize)
            }

            override fun onPlayerError(error: PlaybackException) {
                navigateNext()
            }
        }
        exoPlayer.addListener(listener)

        val layoutListener = android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyPhoneAdaptedScale(textureView, latestVideoSize)
        }
        textureView.addOnLayoutChangeListener(layoutListener)

        onDispose {
            textureView.removeOnLayoutChangeListener(layoutListener)
            exoPlayer.removeListener(listener)
            exoPlayer.clearVideoTextureView(textureView)
            exoPlayer.release()
        }
    }

    // Safety fallback so headless/codec-less environments never stall on Splash
    LaunchedEffect(Unit) {
        delay(12_000L)
        navigateNext()
    }

    // Pure #000000 full-bleed phone container with no player chrome or Skip text
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { textureView },
            modifier = Modifier.fillMaxSize()
        )

        // Seamless vertical black edge blend so the video integrates natively into the screen without a visible box
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val videoH = h * visibleVideoFractionH
            if (videoH > 0f && videoH < h) {
                val topEdge = (h - videoH) / 2f
                val bottomEdge = topEdge + videoH
                val feather = (videoH * 0.14f).coerceAtLeast(24f)

                drawRect(
                    color = Color.Black,
                    topLeft = Offset.Zero,
                    size = Size(w, topEdge)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = topEdge,
                        endY = topEdge + feather
                    ),
                    topLeft = Offset(0f, topEdge),
                    size = Size(w, feather)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = bottomEdge - feather,
                        endY = bottomEdge
                    ),
                    topLeft = Offset(0f, bottomEdge - feather),
                    size = Size(w, feather)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(0f, bottomEdge),
                    size = Size(w, h - bottomEdge)
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onPickSafFolder: () -> Unit,
    onContinueOffline: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    KarlukBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                KarlukLogoImage(size = 104.dp)

                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = tokens.textPrimary,
                    textAlign = TextAlign.Center
                )

                // Language quick selector on Onboarding
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val selected = lang == currentLanguage
                                GlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("onboarding_lang_${lang.code}"),
                                    shape = RoundedCornerShape(12.dp),
                                    highlighted = selected,
                                    onClick = { onSelectLanguage(lang) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lang.nativeName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) tokens.accentPrimary else tokens.textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = tokens.accentPrimary
                            )
                            Text(
                                text = stringResource(R.string.onboarding_perm_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary
                            )
                        }
                        Text(
                            text = stringResource(R.string.onboarding_perm_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onRequestAudioPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_grant_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.accentPrimary,
                                contentColor = Color(0xFF031126)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.onboarding_grant_button),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onPickSafFolder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_pick_folder_button"),
                            border = BorderStroke(1.dp, tokens.glassBorderBright),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = tokens.accentPrimary)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.onboarding_pick_folder),
                                color = tokens.textPrimary
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onContinueOffline,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("onboarding_continue_button")
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.accentPrimary
                    )
                }
            }
        }
    }
}
