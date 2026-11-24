package com.example.feature.settings

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.core.common.FormatUtils
import com.example.core.common.ShareUtils
import com.example.core.designsystem.FormatBadge
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukLogoImage
import com.example.core.designsystem.KarlukSwitch
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LanguageFlagBadge
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SongArtworkThumb
import com.example.core.designsystem.localizedArtist
import com.example.core.designsystem.resolveTextColorPair
import com.example.data.update.AppUpdateChecker
import kotlinx.coroutines.launch
import com.example.domain.model.AccentOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.PlaybackState
import com.example.domain.model.Song
import com.example.domain.model.StorageStats
import com.example.domain.model.TextColorOption
import com.example.domain.model.ThemeAtmosphere
import com.example.domain.model.UserSettings

@Composable
fun SettingsScreen(
    settings: UserSettings,
    playbackState: PlaybackState,
    trashCount: Int,
    isCheckingUpdate: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleDayNight: (Boolean) -> Unit,
    onOpenThemeStudio: () -> Unit,
    onSelectTextColor: (TextColorOption) -> Unit,
    onSelectCustomTextColor: (Long) -> Unit,
    onPickCustomBackground: (Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenStorageInfo: () -> Unit,
    onOpenTrash: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenAbout: () -> Unit,
    onToggleResumePlayback: (Boolean) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleAutoScan: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalKarlukTokens.current
    val isNightMode = settings.themeAtmosphere != ThemeAtmosphere.SILVER_FROST
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTextColorDialog by remember { mutableStateOf(false) }

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
                    val activeSwatch = resolveTextColorPair(
                        option = settings.textColorOption,
                        isLight = tokens.isLightTheme,
                        customTextColorArgb = settings.customTextColorArgb
                    ).first
                    val textColorSummary = if (settings.customTextColorArgb != 0L) {
                        val hex = String.format("#%06X", (settings.customTextColorArgb and 0xFFFFFFL).toInt())
                        "${stringResource(R.string.text_color_custom_badge)} ($hex)"
                    } else {
                        textColorLabel(settings.textColorOption)
                    }
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_text_color_row"),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { showTextColorDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.FormatColorText, contentDescription = null, tint = tokens.accentPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_text_color),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = textColorSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(activeSwatch)
                                    .border(1.4.dp, tokens.glassBorderBright, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = tokens.textSecondary)
                        }
                    }
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
                        icon = Icons.Rounded.DeleteOutline,
                        title = stringResource(R.string.trash_title),
                        subtitle = stringResource(R.string.trash_subtitle_count, trashCount),
                        onClick = onOpenTrash,
                        modifier = Modifier.testTag("settings_trash_row")
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
                    SettingsActionRow(
                        icon = Icons.Rounded.Share,
                        title = stringResource(R.string.settings_share_app),
                        subtitle = stringResource(R.string.settings_share_app_desc),
                        onClick = { scope.launch { ShareUtils.shareApp(context) } },
                        modifier = Modifier.testTag("settings_share_app_row")
                    )
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = stringResource(R.string.settings_updates),
                        subtitle = if (isCheckingUpdate) {
                            stringResource(R.string.update_check_now) + "..."
                        } else {
                            stringResource(R.string.update_channel_subtitle)
                        },
                        onClick = onCheckUpdates,
                        modifier = Modifier.testTag("settings_updates_row")
                    )
                }

                item {
                    SettingsActionRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(
                            R.string.about_version_format,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    LanguageFlagBadge(language = lang)
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) tokens.accentPrimary else tokens.textPrimary
                                    )
                                }
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

    if (showTextColorDialog) {
        val initialSwatch = resolveTextColorPair(
            option = settings.textColorOption,
            isLight = tokens.isLightTheme,
            customTextColorArgb = settings.customTextColorArgb
        ).first
        AlertDialog(
            onDismissRequest = { showTextColorDialog = false },
            containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
            title = { Text(stringResource(R.string.settings_text_color), color = tokens.textPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextColorOption.entries.forEach { option ->
                        val selected = settings.customTextColorArgb == 0L && option == settings.textColorOption
                        val swatchColor = resolveTextColorPair(option, tokens.isLightTheme).first
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_color_option_${option.id}"),
                            shape = RoundedCornerShape(14.dp),
                            highlighted = selected,
                            onClick = {
                                onSelectTextColor(option)
                                showTextColorDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(1.4.dp, tokens.glassBorderBright, CircleShape)
                                    )
                                    Text(
                                        text = textColorLabel(option),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) tokens.accentPrimary else tokens.textPrimary
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = tokens.accentPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    ManualTextColorPicker(
                        initialColor = initialSwatch,
                        hasCustom = settings.customTextColorArgb != 0L,
                        onApplyCustomColor = { argb ->
                            onSelectCustomTextColor(argb)
                            showTextColorDialog = false
                        },
                        onResetAuto = {
                            onSelectTextColor(TextColorOption.AUTO)
                            showTextColorDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTextColorDialog = false }) {
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
            KarlukSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
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
private fun textColorLabel(option: TextColorOption): String = when (option) {
    TextColorOption.AUTO -> stringResource(R.string.text_color_auto)
    TextColorOption.PURE_WHITE -> stringResource(R.string.text_color_white)
    TextColorOption.SKY_CYAN -> stringResource(R.string.text_color_cyan)
    TextColorOption.GOLD_AMBER -> stringResource(R.string.text_color_gold)
    TextColorOption.EMERALD_MINT -> stringResource(R.string.text_color_emerald)
    TextColorOption.ROSE_CORAL -> stringResource(R.string.text_color_rose)
    TextColorOption.OBSIDIAN_BLACK -> stringResource(R.string.text_color_obsidian)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeStudioScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onSelectAtmosphere: (ThemeAtmosphere) -> Unit,
    onSelectAccent: (AccentOption) -> Unit,
    onSelectTextColor: (TextColorOption) -> Unit,
    onSelectCustomTextColor: (Long) -> Unit,
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
                        val activeSwatch = resolveTextColorPair(
                            option = settings.textColorOption,
                            isLight = tokens.isLightTheme,
                            customTextColorArgb = settings.customTextColorArgb
                        ).first
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.settings_text_color), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextColorOption.entries.forEach { option ->
                                        val selected = settings.customTextColorArgb == 0L && option == settings.textColorOption
                                        val swatch = resolveTextColorPair(option, tokens.isLightTheme).first
                                        GlassCard(
                                            shape = RoundedCornerShape(14.dp),
                                            highlighted = selected,
                                            onClick = { onSelectTextColor(option) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(swatch)
                                                        .border(1.2.dp, tokens.glassBorderBright, CircleShape)
                                                )
                                                Text(
                                                    text = textColorLabel(option),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) tokens.accentPrimary else tokens.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                ManualTextColorPicker(
                                    initialColor = activeSwatch,
                                    hasCustom = settings.customTextColorArgb != 0L,
                                    onApplyCustomColor = onSelectCustomTextColor,
                                    onResetAuto = { onSelectTextColor(TextColorOption.AUTO) }
                                )
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
                                    KarlukSwitch(checked = settings.reducedMotion, onCheckedChange = onToggleReducedMotion)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.theme_low_power), color = tokens.textPrimary, modifier = Modifier.weight(1f))
                                    KarlukSwitch(checked = settings.lowPowerMode, onCheckedChange = onToggleLowPower)
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
fun ManualTextColorPicker(
    initialColor: Color,
    hasCustom: Boolean,
    onApplyCustomColor: (Long) -> Unit,
    onResetAuto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val fallbackSeed = if (!hasCustom) {
        val rawHsv = FloatArray(3).also { AndroidColor.colorToHSV(initialColor.toArgb(), it) }
        if (rawHsv[1] < 0.15f) tokens.accentPrimary else initialColor
    } else {
        initialColor
    }
    val initialHsv = remember(fallbackSeed) {
        FloatArray(3).also { AndroidColor.colorToHSV(fallbackSeed.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceAtLeast(if (hasCustom) 0f else 0.85f)) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.25f)) }

    LaunchedEffect(fallbackSeed, hasCustom) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(fallbackSeed.toArgb(), hsv)
        hue = hsv[0]
        saturation = if (hasCustom) hsv[1] else hsv[1].coerceAtLeast(0.85f)
        brightness = hsv[2].coerceAtLeast(0.25f)
    }

    val pickedInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    val pickedColor = Color(pickedInt)
    val pickedArgbLong = pickedInt.toLong() and 0xFFFFFFFFL
    val hexText = String.format("#%06X", (pickedArgbLong and 0xFFFFFFL).toInt())

    val satZeroColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 0f, brightness)))
    val satFullColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, brightness)))
    val valLowColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 0.08f)))
    val valHighColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f)))

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        highlighted = hasCustom
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_color_manual),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = hexText,
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(pickedColor)
                            .border(1.5.dp, tokens.glassBorderBright, CircleShape)
                    )
                }
            }

            // Live contrasting preview strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (tokens.isLightTheme) Color(0xFFE7EFFD) else Color(0xFF07132E))
                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.text_color_preview),
                    style = MaterialTheme.typography.titleMedium,
                    color = pickedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Unified pixel-accurate LTR Hue bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.text_color_hue), style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                GradientColorSlider(
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = { hue = it },
                    gradientColors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    ),
                    thumbColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                    modifier = Modifier.testTag("manual_color_hue_slider")
                )
            }

            // Unified Saturation bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.text_color_saturation), style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                GradientColorSlider(
                    value = saturation,
                    valueRange = 0f..1f,
                    onValueChange = { saturation = it },
                    gradientColors = listOf(satZeroColor, satFullColor),
                    thumbColor = pickedColor,
                    modifier = Modifier.testTag("manual_color_sat_slider")
                )
            }

            // Unified Brightness bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.text_color_brightness), style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                GradientColorSlider(
                    value = brightness,
                    valueRange = 0.08f..1f,
                    onValueChange = { brightness = it },
                    gradientColors = listOf(valLowColor, valHighColor),
                    thumbColor = pickedColor,
                    modifier = Modifier.testTag("manual_color_val_slider")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onApplyCustomColor(pickedArgbLong) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accentPrimary,
                        contentColor = Color(0xFF031126)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("manual_color_apply_btn")
                ) {
                    Text(stringResource(R.string.text_color_apply), fontWeight = FontWeight.Bold)
                }
                if (hasCustom) {
                    OutlinedButton(
                        onClick = onResetAuto,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("manual_color_reset_btn")
                    ) {
                        Text(stringResource(R.string.text_color_reset), color = tokens.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientColorSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    gradientColors: List<Color>,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(36.dp)
                .minimumInteractiveComponentSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val thumbRadiusDp = 12.dp
            val thumbSizeDp = 24.dp
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val thumbRadiusPx = with(density) { thumbRadiusDp.toPx() }
            val trackWidthPx = (totalWidthPx - thumbRadiusPx * 2f).coerceAtLeast(1f)
            val usableWidthDp = (maxWidth - thumbSizeDp).coerceAtLeast(1.dp)

            fun updateFromX(xPx: Float) {
                val f = ((xPx - thumbRadiusPx) / trackWidthPx).coerceIn(0f, 1f)
                currentOnValueChange(valueRange.start + f * span)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(valueRange.start, valueRange.endInclusive, trackWidthPx) {
                        detectTapGestures { offset ->
                            updateFromX(offset.x)
                        }
                    }
                    .pointerInput(valueRange.start, valueRange.endInclusive, trackWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset -> updateFromX(offset.x) },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                updateFromX(change.position.x)
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = thumbRadiusDp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(gradientColors))
                        .border(1.dp, tokens.glassBorder, RoundedCornerShape(8.dp))
                )

                Box(
                    modifier = Modifier
                        .offset(x = usableWidthDp * fraction)
                        .size(thumbSizeDp)
                        .clip(CircleShape)
                        .background(thumbColor)
                        .border(2.5.dp, Color.White, CircleShape)
                        .border(1.dp, Color(0x66000000), CircleShape)
                )
            }
        }
    }
}

@Composable
fun StorageInfoScreen(
    stats: StorageStats,
    allSongs: List<Song>,
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenAddFiles: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onDeleteSong: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    var showLargestDialog by remember { mutableStateOf(false) }
    var selectedFormatFilter by remember { mutableStateOf<String?>(null) }

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
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatLine(
                                    label = stringResource(R.string.storage_total_tracks),
                                    value = "${stats.totalTracks}",
                                    onClick = onOpenLibrary
                                )
                                StatLine(
                                    label = stringResource(R.string.storage_total_size),
                                    value = FormatUtils.formatFileSize(stats.totalSizeBytes),
                                    onClick = { showLargestDialog = true }
                                )
                                StatLine(
                                    label = stringResource(R.string.storage_cache_size),
                                    value = FormatUtils.formatFileSize(stats.artworkCacheBytes),
                                    onClick = onClearCache
                                )
                                StatLine(
                                    label = stringResource(R.string.files_added_folders),
                                    value = "${stats.safFoldersCount}",
                                    onClick = onOpenAddFiles
                                )
                            }
                        }
                    }

                    if (stats.formatCounts.isNotEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.storage_by_format), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                                    stats.formatCounts.entries.sortedByDescending { it.value }.forEach { (fmt, count) ->
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .testTag("storage_format_${fmt.lowercase()}"),
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = { selectedFormatFilter = fmt }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                FormatBadge(fmt)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.home_tracks_count, count),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = tokens.textPrimary
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        tint = tokens.textSecondary
                                                    )
                                                }
                                            }
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

    if (showLargestDialog) {
        val largestSongs = remember(allSongs) {
            allSongs.sortedByDescending { it.fileSize }.take(40)
        }
        StorageSongsDialog(
            title = stringResource(R.string.storage_largest_tracks),
            songs = largestSongs,
            showFileSize = true,
            onDismiss = { showLargestDialog = false },
            onPlaySongs = onPlaySongs,
            onDeleteSong = onDeleteSong
        )
    }

    selectedFormatFilter?.let { format ->
        val filtered = remember(allSongs, format) {
            allSongs.filter { it.formatLabel.equals(format, ignoreCase = true) }
        }
        StorageSongsDialog(
            title = "$format (${filtered.size})",
            songs = filtered,
            showFileSize = true,
            onDismiss = { selectedFormatFilter = null },
            onPlaySongs = onPlaySongs,
            onDeleteSong = onDeleteSong
        )
    }
}

@Composable
private fun StorageSongsDialog(
    title: String,
    songs: List<Song>,
    showFileSize: Boolean,
    onDismiss: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onDeleteSong: (Long) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
        title = { Text(title, color = tokens.textPrimary) },
        text = {
            if (songs.isEmpty()) {
                Text(stringResource(R.string.empty_library_desc), color = tokens.textSecondary)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { idx, song ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                onPlaySongs(songs, idx)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SongArtworkThumb(
                                    artworkUri = song.customBackgroundUri?.takeIf { it.isNotBlank() } ?: song.artworkUri,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = if (showFileSize && song.fileSize > 0L) {
                                        "${FormatUtils.formatFileSize(song.fileSize)} • ${song.localizedArtist()}"
                                    } else {
                                        song.localizedArtist()
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSong(song.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = Color(0xFFFF5E7E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (songs.isNotEmpty()) {
                TextButton(
                    onClick = {
                        onPlaySongs(songs, 0)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = tokens.accentPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_play_all), color = tokens.accentPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
            }
        }
    )
}

@Composable
private fun StatLine(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val tokens = LocalKarlukTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tokens.textSecondary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = tokens.accentPrimary)
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens.textSecondary
            )
        }
    }
}

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    updateInfo: AppUpdateInfo? = null,
    isCheckingUpdate: Boolean = false,
    onCheckUpdates: () -> Unit = {}
) {
    val tokens = LocalKarlukTokens.current
    val uriHandler = LocalUriHandler.current
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    KarlukLogoImage(size = 136.dp)
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = stringResource(
                            R.string.about_version_format,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
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

                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = tokens.emerald)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_privacy),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_privacy_note),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_telegram_card"),
                        shape = RoundedCornerShape(22.dp),
                        onClick = onCheckUpdates
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = tokens.accentPrimary)
                                Text(
                                    text = stringResource(R.string.settings_updates),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                            }
                            Text(
                                text = AppUpdateChecker.TELEGRAM_CHANNEL_URL,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.accentPrimary,
                                textAlign = TextAlign.Center
                            )
                            if (updateInfo != null) {
                                Text(
                                    text = if (updateInfo.isUpdateAvailable) {
                                        stringResource(R.string.update_available_title, updateInfo.latestVersion)
                                    } else {
                                        stringResource(R.string.update_up_to_date, BuildConfig.VERSION_NAME)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (updateInfo.isUpdateAvailable) tokens.emerald else tokens.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCheckUpdates,
                                    enabled = !isCheckingUpdate,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tokens.accentPrimary,
                                        contentColor = Color(0xFF031126)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("about_check_update_btn")
                                ) {
                                    if (isCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF031126)
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.update_check_version),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching { uriHandler.openUri(AppUpdateChecker.TELEGRAM_CHANNEL_URL) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("about_download_latest_btn")
                                ) {
                                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = tokens.accentPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.update_download_latest),
                                        color = tokens.textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_sharia_notice_card"),
                        shape = RoundedCornerShape(22.dp),
                        highlighted = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = tokens.emerald
                                )
                                Text(
                                    text = stringResource(R.string.about_sharia_notice_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.emerald,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = stringResource(R.string.about_sharia_notice_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.about_copyright_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.accentPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.about_copyright_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
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
