package com.example.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(450))
        scale.animateTo(1f, animationSpec = tween(450))
        delay(850L)
        if (onboardingCompleted) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    KarlukBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .padding(24.dp)
            ) {
                KarlukLogoImage(size = 160.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.textPrimary
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.accentPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.5.dp,
                    color = tokens.accentPrimary
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
