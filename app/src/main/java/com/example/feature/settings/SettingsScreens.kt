package com.example.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukLogoImage
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.domain.model.AccentOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.PlaybackState
import com.example.domain.model.StorageStats
import com.example.domain.model.ThemeAtmosphere
import com.example.domain.model.UserSettings

@Composable
fun SettingsScreen(
    settings: UserSettings,
    playbackState: PlaybackState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleDayNight: (Boolean) -> Unit,
    onOpenThemeStudio: () -> Unit,
    onPickCustomBackground: (Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenStorageInfo: () -> Unit,
    onOpenAbout: () -> Unit,
    onToggleResumePlayback: (Boolean) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleAutoScan: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val isNightMode = settings.themeAtmosphere != ThemeAtmosphere.SILVER_FROST
    var showLanguageDialog by remember { mutableStateOf(false) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomBackground(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            KarlukTopBar(title = stringResource(R.string.settings_title))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SettingsSwitchRow(
                        icon = if (isNightMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        title = stringResource(if (isNightMode) R.string.theme_night_mode else R.string.theme_day_mode),
                        checked = isNightMode,
                        onCheckedChange = onToggleDayNight,
                        modifier = Modifier.testTag("settings_day_night_switch")
                    )
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = settings.language.nativeName,
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.testTag("settings_language_row")
                    )
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_theme_studio),
                        subtitle = atmosphereLabel(settings.themeAtmosphere),
                        onClick = onOpenThemeStudio,
                        modifier = Modifier.testTag("settings_theme_studio_row")
                    )
                }

                item {
                    val hasBg = !settings.customBackgroundUri.isNullOrBlank()
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_custom_bg_row"),
                        shape = RoundedCornerShape(18.dp),
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasBg) {
                                AsyncImage(
                                    model = settings.customBackgroundUri,
                                    contentDescription = stringResource(R.string.theme_custom_bg_title),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, tokens.accentPrimary, RoundedCornerShape(10.dp))
                                )
                            } else {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = tokens.accentPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.theme_custom_bg_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = stringResource(
                                        if (hasBg) R.string.theme_custom_bg_active else R.string.theme_custom_bg_pick
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                            if (hasBg) {
                                IconButton(
                                    onClick = onClearCustomBackground,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("settings_clear_bg_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.theme_custom_bg_reset),
                                        tint = Color(0xFFFF6E8A)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = tokens.textSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.Equalizer,
                        title = stringResource(R.string.settings_equalizer),
                        subtitle = stringResource(R.string.equalizer_summary),
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("settings_equalizer_row")
                    )
                }

                item {
                    val sleepSubtitle = when {
                        playbackState.sleepAfterCurrentTrack -> stringResource(R.string.sleep_timer_end_track)
                        playbackState.sleepTimerRemainingMs > 0L -> stringResource(
                            R.string.sleep_timer_remaining,
                            FormatUtils.formatDuration(playbackState.sleepTimerRemainingMs)
                        )
                        else -> stringResource(R.string.sleep_timer_off)
                    }
                    SettingsActionRow(
                        icon = Icons.Rounded.Timer,
                        title = stringResource(R.string.sleep_timer_title),
                        subtitle = sleepSubtitle,
                        onClick = onOpenSleepTimer,
                        modifier = Modifier.testTag("settings_sleep_row")
                    )
                }

                item {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = stringResource(R.string.settings_resume_playback),
                        checked = settings.resumePlaybackOnLaunch,
                        onCheckedChange = onToggleResumePlayback
                    )
                }

                item {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = stringResource(R.string.settings_gapless),
                        checked = settings.gaplessPlayback,
                        onCheckedChange = onToggleGapless
                    )
                }

                item {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(R.string.settings_auto_scan),
                        checked = settings.autoScanOnStartup,
                        onCheckedChange = onToggleAutoScan
                    )
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.Storage,
                        title = stringResource(R.string.settings_storage),
                        subtitle = null,
                        onClick = onOpenStorageInfo,
                        modifier = Modifier.testTag("settings_storage_row")
                    )
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = stringResource(R.string.history_clear),
                        subtitle = null,
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("settings_clear_history_row")
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = tokens.emerald)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                Text(stringResource(R.string.settings_privacy_note), style = MaterialTheme.typography.bodyMedium, color = tokens.textSecondary)
                            }
                        }
                    }
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.about_version),
                        onClick = onOpenAbout,
                        modifier = Modifier.testTag("settings_about_row")
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = { Text(stringResource(R.string.settings_language), color = tokens.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = lang == settings.language
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().testTag("lang_option_${lang.code}"),
                            shape = RoundedCornerShape(14.dp),
                            highlighted = selected,
                            onClick = {
                                showLanguageDialog = false
                                onSelectLanguage(lang)
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${lang.nativeName} (${if (lang.isRtl) "RTL" else "LTR"})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) tokens.accentPrimary else tokens.textPrimary
                                )
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = tokens.accentPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = tokens.accentPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    GlassCard(
        modifier = modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tokens.accentPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = tokens.textSecondary)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = tokens.textSecondary)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    GlassCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tokens.accentPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = tokens.accentPrimary)
            )
        }
    }
}

@Composable
private fun atmosphereLabel(atmosphere: ThemeAtmosphere): String = when (atmosphere) {
    ThemeAtmosphere.DEEP_NAVY -> stringResource(R.string.theme_dark_navy)
    ThemeAtmosphere.MIDNIGHT_VIOLET -> stringResource(R.string.theme_midnight_violet)
    ThemeAtmosphere.ROYAL_OCEAN -> stringResource(R.string.theme_ocean_blue)
    ThemeAtmosphere.SILVER_FROST -> stringResource(R.string.theme_light_frost)
}

@Composable
fun ThemeStudioScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onSelectAtmosphere: (ThemeAtmosphere) -> Unit,
    onSelectAccent: (AccentOption) -> Unit,
    onChangeGlassOpacity: (Float) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleLowPower: (Boolean) -> Unit,
    onPickCustomBackground: (Uri) -> Unit,
    onClearCustomBackground: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomBackground(uri)
        }
    }
    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(title = stringResource(R.string.settings_theme_studio), onBack = onBack)

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        val hasBg = !settings.customBackgroundUri.isNullOrBlank()
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.theme_custom_bg_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                                if (hasBg) {
                                    AsyncImage(
                                        model = settings.customBackgroundUri,
                                        contentDescription = stringResource(R.string.theme_custom_bg_title),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(124.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(1.dp, tokens.accentPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tokens.glassSurfaceElevated)
                                            .border(1.dp, tokens.glassBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = tokens.accentPrimary)
                                            Text(
                                                text = stringResource(R.string.theme_custom_bg_default),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = tokens.textSecondary
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            galleryPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = tokens.accentPrimary,
                                            contentColor = Color(0xFF031126)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("theme_pick_bg_btn")
                                    ) {
                                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.theme_custom_bg_pick), fontWeight = FontWeight.Bold)
                                    }
                                    if (hasBg) {
                                        OutlinedButton(
                                            onClick = onClearCustomBackground,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("theme_reset_bg_btn")
                                        ) {
                                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Color(0xFFFF6E8A))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.theme_custom_bg_reset), color = Color(0xFFFF6E8A))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                ThemeAtmosphere.entries.forEach { atm ->
                                    val selected = atm == settings.themeAtmosphere
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        highlighted = selected,
                                        onClick = { onSelectAtmosphere(atm) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(atmosphereLabel(atm), color = if (selected) tokens.accentPrimary else tokens.textPrimary)
                                            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = tokens.accentPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.theme_accent), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccentOption.entries.forEach { acc ->
                                        val selected = acc == settings.accentOption
                                        GlassCard(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            highlighted = selected,
                                            onClick = { onSelectAccent(acc) }
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = acc.name.replace('_', ' '),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) tokens.accentPrimary else tokens.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.theme_glass_opacity), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                Slider(
                                    value = settings.glassOpacity,
                                    onValueChange = onChangeGlassOpacity,
                                    valueRange = 0.06f..0.32f,
                                    colors = SliderDefaults.colors(thumbColor = tokens.accentPrimary, activeTrackColor = tokens.accentPrimary)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.theme_reduced_motion), color = tokens.textPrimary, modifier = Modifier.weight(1f))
                                    Switch(checked = settings.reducedMotion, onCheckedChange = onToggleReducedMotion)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.theme_low_power), color = tokens.textPrimary, modifier = Modifier.weight(1f))
                                    Switch(checked = settings.lowPowerMode, onCheckedChange = onToggleLowPower)
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
fun StorageInfoScreen(
    stats: StorageStats,
    onBack: () -> Unit,
    onClearCache: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(title = stringResource(R.string.storage_title), onBack = onBack)

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatLine(stringResource(R.string.storage_total_tracks), "${stats.totalTracks}")
                                StatLine(stringResource(R.string.storage_total_size), FormatUtils.formatFileSize(stats.totalSizeBytes))
                                StatLine(stringResource(R.string.storage_cache_size), FormatUtils.formatFileSize(stats.artworkCacheBytes))
                                StatLine(stringResource(R.string.files_added_folders), "${stats.safFoldersCount}")
                            }
                        }
                    }

                    if (stats.formatCounts.isNotEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(R.string.storage_by_format), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                    stats.formatCounts.entries.sortedByDescending { it.value }.forEach { (fmt, count) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FormatBadge(fmt)
                                            Text(
                                                text = stringResource(R.string.home_tracks_count, count),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = tokens.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onClearCache,
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.accentPrimary, contentColor = Color(0xFF031126)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.storage_clear_cache), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val tokens = LocalKarlukTokens.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tokens.textSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = tokens.accentPrimary)
    }
}

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(title = stringResource(R.string.settings_about), onBack = onBack)

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    KarlukLogoImage(size = 136.dp)
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.about_version),
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accentPrimary
                    )
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                        Text(
                            text = stringResource(R.string.about_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("MP3", "OPUS", "AAC", "M4A", "FLAC", "WAV", "OGG").forEach { fmt ->
                            FormatBadge(formatLabel = fmt)
                        }
                    }
                }
            }
        }
    }
}
