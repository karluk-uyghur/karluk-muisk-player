package com.example.feature.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukLogoImage
import com.example.core.designsystem.LanguageFlagBadge
import com.example.core.designsystem.LocalKarlukTokens
import com.example.domain.model.AppLanguage
import java.text.BreakIterator
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    skipIntroOnce: Boolean = false,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
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
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040916)))
        return
    }

    val brandTitle = "Karluk MP3 Pro"
    val rainbowPalette = remember {
        listOf(
            Color(0xFF00E5FF), // Neon Cyan
            Color(0xFF38BDF8), // Sky Blue
            Color(0xFF818CF8), // Indigo
            Color(0xFFC084FC), // Orchid Violet
            Color(0xFFF472B6), // Rose Pink
            Color(0xFFFB7185), // Coral
            Color(0xFFFBBF24), // Amber Gold
            Color(0xFF34D399), // Emerald Mint
            Color(0xFF2DD4BF)  // Turquoise
        )
    }

    val welcomeTitle = stringResource(R.string.onboarding_title)
    val welcomeCharBoundaries = remember(welcomeTitle) {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(welcomeTitle)
        buildList {
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                add(end)
                end = iterator.next()
            }
        }
    }

    var logoVisible by remember { mutableStateOf(false) }
    var visibleCharCount by remember { mutableIntStateOf(0) }
    var welcomeDividerVisible by remember { mutableStateOf(false) }
    var welcomeVisibleCharCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(welcomeTitle) {
        logoVisible = true
        delay(160L)
        for (i in 1..brandTitle.length) {
            visibleCharCount = i
            delay(56L)
        }
        delay(140L)
        welcomeDividerVisible = true
        for (i in 1..welcomeCharBoundaries.size) {
            welcomeVisibleCharCount = i
            delay(42L)
        }
        delay(950L)
        navigateNext()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mp3_intro")
    val eqPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val haloSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_spin"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.70f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "logo_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0E295E),
                        Color(0xFF060E24),
                        Color(0xFF02040C)
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Multi-color expanding pulse aura rings behind the centerpiece
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.40f)
            val baseR = 68.dp.toPx()
            val ringColors = listOf(
                Color(0xFF00E5FF),
                Color(0xFFC084FC),
                Color(0xFF34D399)
            )
            for (ring in 0..2) {
                val p = (pulseRadius + ring / 3f) % 1f
                val r = baseR + p * 106.dp.toPx()
                val alpha = (1f - p) * 0.34f
                drawCircle(
                    color = ringColors[ring].copy(alpha = alpha),
                    radius = r,
                    center = center,
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                // Rotating multi-color spectrum glow halo framing the Karluk emblem
                Canvas(modifier = Modifier.size(144.dp)) {
                    val strokeW = 3.5.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2f
                    withTransform({
                        rotate(degrees = haloSpinAngle, pivot = center)
                    }) {
                        drawCircle(
                            brush = Brush.sweepGradient(rainbowPalette + rainbowPalette.first()),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeW)
                        )
                    }
                }
                KarlukLogoImage(size = 118.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Multi-colored animated MP3 equalizer spectrum bars with bright floating peak dots
            Canvas(
                modifier = Modifier
                    .width(236.dp)
                    .height(40.dp)
            ) {
                val barCount = 21
                val gap = 4.dp.toPx()
                val barW = (size.width - gap * (barCount - 1)) / barCount
                val centerIdx = (barCount - 1) / 2f
                for (i in 0 until barCount) {
                    val distWeight = 1f - (kotlin.math.abs(i - centerIdx) / (centerIdx + 1f)) * 0.50f
                    val wave = 0.36f + 0.64f * ((sin(eqPhase + i * 0.52f) + 1f) * 0.5f)
                    val barH = (size.height * 0.86f * wave * distWeight).coerceAtLeast(5.dp.toPx())
                    val left = i * (barW + gap)
                    val top = (size.height - barH) / 2f

                    val barTopColor = rainbowPalette[i % rainbowPalette.size]
                    val barBottomColor = rainbowPalette[(i + 2) % rainbowPalette.size].copy(alpha = 0.72f)

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barTopColor, barBottomColor),
                            startY = top,
                            endY = top + barH
                        ),
                        topLeft = Offset(left, top),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = (barW * 0.34f).coerceAtLeast(1.4f),
                        center = Offset(left + barW / 2f, (top - 3.dp.toPx()).coerceAtLeast(2f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Multi-colored letter-by-letter spring reveal of "Karluk MP3 Pro" strictly LTR
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("splash_brand_letters")
                ) {
                    brandTitle.forEachIndexed { index, ch ->
                        val shown = index < visibleCharCount
                        val charAlpha by animateFloatAsState(
                            targetValue = if (shown) 1f else 0f,
                            animationSpec = tween(durationMillis = 190),
                            label = "char_alpha_$index"
                        )
                        val charOffsetY by animateDpAsState(
                            targetValue = if (shown) 0.dp else 16.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "char_offset_$index"
                        )
                        val letterTint = rainbowPalette[index % rainbowPalette.size]
                        Text(
                            text = ch.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = letterTint,
                            modifier = Modifier
                                .offset(y = charOffsetY)
                                .alpha(charAlpha)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = welcomeDividerVisible,
                enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(124.dp)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF00E5FF),
                                        Color(0xFFC084FC),
                                        Color(0xFFFBBF24),
                                        Color(0xFF34D399),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    val revealedWelcome = if (welcomeVisibleCharCount <= 0) {
                        ""
                    } else {
                        val endIdx = welcomeCharBoundaries[
                            (welcomeVisibleCharCount - 1).coerceIn(0, welcomeCharBoundaries.lastIndex)
                        ]
                        welcomeTitle.substring(0, endIdx)
                    }
                    Text(
                        text = revealedWelcome,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF34D399),
                                    Color(0xFFFBBF24),
                                    Color(0xFFF472B6),
                                    Color(0xFFC084FC)
                                )
                            )
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("splash_welcome_text")
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onPickSafFolder: () -> Unit
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

                // Language quick selector on Onboarding with Country/Nation Flags
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
                        AppLanguage.entries.chunked(2).forEach { rowLangs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowLangs.forEach { lang ->
                                    val selected = lang == currentLanguage
                                    GlassCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("onboarding_lang_${lang.code}"),
                                        shape = RoundedCornerShape(14.dp),
                                        highlighted = selected,
                                        onClick = { onSelectLanguage(lang) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            LanguageFlagBadge(language = lang)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = lang.nativeName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) tokens.accentPrimary else tokens.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
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
            }
        }
    }
}
