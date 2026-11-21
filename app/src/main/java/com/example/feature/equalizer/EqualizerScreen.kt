package com.example.feature.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.GlassCard
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.KarlukTopBar
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.SpectrumVisualizerCanvas
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.EqualizerPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    effectsState: AudioEffectsState,
    spectrum: FloatArray,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onToggleEqualizer: (Boolean) -> Unit,
    onSetBandLevel: (Int, Short) -> Unit,
    onApplyPreset: (EqualizerPreset) -> Unit,
    onSetBassBoost: (Boolean, Int) -> Unit,
    onSetVirtualizer: (Boolean, Int) -> Unit,
    onSetLoudness: (Boolean, Int) -> Unit,
    onSetBalance: (Float) -> Unit
) {
    val tokens = LocalKarlukTokens.current

    KarlukBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                KarlukTopBar(
                    title = stringResource(R.string.equalizer_title),
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = {
                                onApplyPreset(EqualizerPreset.FLAT)
                                onSetBalance(0f)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("eq_reset_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = tokens.accentPrimary)
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Spectrum Card
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.equalizer_enable),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary
                                    )
                                    Switch(
                                        checked = effectsState.isEqualizerEnabled,
                                        onCheckedChange = onToggleEqualizer,
                                        enabled = effectsState.isEqualizerSupported,
                                        colors = SwitchDefaults.colors(checkedThumbColor = tokens.accentPrimary),
                                        modifier = Modifier.testTag("eq_master_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                SpectrumVisualizerCanvas(
                                    spectrum = spectrum,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(82.dp)
                                )
                            }
                        }
                    }

                    // Frequency Bands (read dynamically from device Equalizer API)
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                            if (!effectsState.isEqualizerSupported || effectsState.bands.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.equalizer_unsupported),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                                )
                            } else {
                                val minMb = effectsState.minLevelMb.toFloat()
                                val maxMb = effectsState.maxLevelMb.toFloat().coerceAtLeast(minMb + 100f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    effectsState.bands.forEach { band ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val db = band.levelMb / 100f
                                            Text(
                                                text = String.format("%+.0fdB", db),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = tokens.accentPrimary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            VerticalBandSlider(
                                                value = band.levelMb.toFloat(),
                                                valueRange = minMb..maxMb,
                                                enabled = effectsState.isEqualizerEnabled,
                                                onValueChange = { onSetBandLevel(band.index, it.roundToInt().toShort()) },
                                                modifier = Modifier.height(164.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = band.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Presets
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EqualizerPreset.entries.forEach { preset ->
                                    val label = when (preset) {
                                        EqualizerPreset.FLAT -> stringResource(R.string.preset_flat)
                                        EqualizerPreset.CLASSICAL -> stringResource(R.string.preset_classical)
                                        EqualizerPreset.POP -> stringResource(R.string.preset_pop)
                                        EqualizerPreset.ROCK -> stringResource(R.string.preset_rock)
                                        EqualizerPreset.JAZZ -> stringResource(R.string.preset_jazz)
                                        EqualizerPreset.HIP_HOP -> stringResource(R.string.preset_hiphop)
                                        EqualizerPreset.VOCAL -> stringResource(R.string.preset_vocal)
                                        EqualizerPreset.CUSTOM -> stringResource(R.string.preset_custom)
                                    }
                                    val selected = effectsState.activePreset == preset
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onApplyPreset(preset) },
                                        enabled = effectsState.isEqualizerSupported,
                                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = tokens.accentPrimary.copy(alpha = 0.28f),
                                            selectedLabelColor = tokens.accentPrimary
                                        ),
                                        modifier = Modifier.testTag("eq_preset_${preset.id}")
                                    )
                                }
                            }
                        }
                    }

                    // Bass Boost, Virtualizer, Loudness, Balance
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                EffectSliderRow(
                                    title = stringResource(R.string.effect_bass_boost),
                                    supported = effectsState.isBassBoostSupported,
                                    enabled = effectsState.isBassBoostEnabled,
                                    value = effectsState.bassBoostStrength.toFloat(),
                                    valueRange = 0f..1000f,
                                    valueLabel = "${effectsState.bassBoostStrength / 10}%",
                                    onToggle = { onSetBassBoost(it, effectsState.bassBoostStrength) },
                                    onSlide = { onSetBassBoost(effectsState.isBassBoostEnabled, it.roundToInt()) }
                                )

                                EffectSliderRow(
                                    title = stringResource(R.string.effect_virtualizer),
                                    supported = effectsState.isVirtualizerSupported,
                                    enabled = effectsState.isVirtualizerEnabled,
                                    value = effectsState.virtualizerStrength.toFloat(),
                                    valueRange = 0f..1000f,
                                    valueLabel = "${effectsState.virtualizerStrength / 10}%",
                                    onToggle = { onSetVirtualizer(it, effectsState.virtualizerStrength) },
                                    onSlide = { onSetVirtualizer(effectsState.isVirtualizerEnabled, it.roundToInt()) }
                                )

                                EffectSliderRow(
                                    title = stringResource(R.string.effect_loudness),
                                    supported = effectsState.isLoudnessSupported,
                                    enabled = effectsState.isLoudnessEnabled,
                                    value = effectsState.loudnessGainMb.toFloat(),
                                    valueRange = 0f..1500f,
                                    valueLabel = "+${effectsState.loudnessGainMb / 100} dB",
                                    onToggle = { onSetLoudness(it, effectsState.loudnessGainMb) },
                                    onSlide = { onSetLoudness(effectsState.isLoudnessEnabled, it.roundToInt()) }
                                )

                                // Software Stereo L/R Balance (always supported via KarlukAudioProcessor)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.effect_balance),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary
                                        )
                                        val balLabel = when {
                                            effectsState.balance < -0.05f -> "L ${(effectsState.balance * -100).roundToInt()}%"
                                            effectsState.balance > 0.05f -> "R ${(effectsState.balance * 100).roundToInt()}%"
                                            else -> "Center"
                                        }
                                        Text(
                                            text = balLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = tokens.accentPrimary
                                        )
                                    }
                                    Slider(
                                        value = effectsState.balance,
                                        onValueChange = onSetBalance,
                                        valueRange = -1f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = tokens.accentPrimary,
                                            activeTrackColor = tokens.accentPrimary
                                        ),
                                        modifier = Modifier.testTag("eq_balance_slider")
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

@Composable
private fun EffectSliderRow(
    title: String,
    supported: Boolean,
    enabled: Boolean,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onToggle: (Boolean) -> Unit,
    onSlide: (Float) -> Unit
) {
    val tokens = LocalKarlukTokens.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                if (!supported) {
                    Text(
                        text = stringResource(R.string.effect_not_supported),
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary
                    )
                }
            }
            if (supported) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.accentPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Switch(
                checked = enabled && supported,
                onCheckedChange = onToggle,
                enabled = supported,
                colors = SwitchDefaults.colors(checkedThumbColor = tokens.accentPrimary)
            )
        }
        if (supported) {
            Slider(
                value = value,
                onValueChange = onSlide,
                valueRange = valueRange,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = tokens.accentPrimary,
                    activeTrackColor = tokens.accentPrimary,
                    inactiveTrackColor = tokens.glassBorder
                )
            )
        }
    }
}

@Composable
private fun VerticalBandSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKarlukTokens.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = tokens.accentPrimary,
            activeTrackColor = tokens.accentPrimary,
            inactiveTrackColor = tokens.glassBorder
        ),
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}
