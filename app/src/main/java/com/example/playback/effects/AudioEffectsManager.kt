package com.example.playback.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.data.datastore.SavedEqualizerPrefs
import com.example.domain.model.AudioBand
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.EqualizerPreset
import com.example.playback.audio.KarlukAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.math.roundToInt

class AudioEffectsManager(
    private val audioProcessor: KarlukAudioProcessor? = null
) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var hwEqSupported = false
    private var hwBassSupported = false
    private var hwVirtSupported = false
    private var hwLoudSupported = false

    private var currentSessionId: Int = 0
    private var lastSavedPrefs: SavedEqualizerPrefs = SavedEqualizerPrefs()

    private val defaultBands = KarlukAudioProcessor.DEFAULT_EQ_FREQUENCIES_HZ.mapIndexed { index, hz ->
        AudioBand(
            index = index,
            centerFreqHz = hz,
            label = formatFreq(hz),
            levelMb = 0
        )
    }

    private val _state = MutableStateFlow(
        AudioEffectsState(
            isEqualizerSupported = true,
            bands = defaultBands,
            minLevelMb = -1500,
            maxLevelMb = 1500,
            isBassBoostSupported = true,
            isVirtualizerSupported = true,
            isLoudnessSupported = true
        )
    )
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    fun attachToSession(audioSessionId: Int, savedPrefs: SavedEqualizerPrefs = lastSavedPrefs) {
        lastSavedPrefs = savedPrefs
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && (equalizer != null || audioProcessor != null)) {
            applySavedPrefs(savedPrefs)
            return
        }

        releaseHardwareEffects()
        currentSessionId = audioSessionId

        val bands = mutableListOf<AudioBand>()
        var minMb: Short = -1500
        var maxMb: Short = 1500
        hwEqSupported = false

        try {
            val eq = Equalizer(0, audioSessionId)
            val count = eq.numberOfBands.toInt()
            if (count > 0) {
                equalizer = eq
                hwEqSupported = true
                val range = eq.bandLevelRange
                if (range != null && range.size >= 2) {
                    minMb = range[0]
                    maxMb = range[1]
                }
                for (i in 0 until count) {
                    val centerHz = eq.getCenterFreq(i.toShort()) / 1000
                    val level = eq.getBandLevel(i.toShort())
                    bands.add(
                        AudioBand(
                            index = i,
                            centerFreqHz = centerHz,
                            label = formatFreq(centerHz),
                            levelMb = level
                        )
                    )
                }
            } else {
                eq.release()
            }
        } catch (_: Exception) {
            hwEqSupported = false
        }

        hwBassSupported = try {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bassBoost = bb
                true
            } else {
                bb.release()
                false
            }
        } catch (_: Exception) {
            false
        }

        hwVirtSupported = try {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virtualizer = virt
                true
            } else {
                virt.release()
                false
            }
        } catch (_: Exception) {
            false
        }

        hwLoudSupported = try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            true
        } catch (_: Exception) {
            false
        }

        val effectiveBands = if (hwEqSupported && bands.isNotEmpty()) bands else _state.value.bands.ifEmpty { defaultBands }

        _state.update {
            it.copy(
                isEqualizerSupported = true,
                bands = effectiveBands,
                minLevelMb = minMb,
                maxLevelMb = maxMb,
                isBassBoostSupported = true,
                isVirtualizerSupported = true,
                isLoudnessSupported = true
            )
        }

        applySavedPrefs(savedPrefs)
    }

    fun applySavedPrefs(prefs: SavedEqualizerPrefs) {
        lastSavedPrefs = prefs
        setEqualizerEnabled(prefs.enabled)
        if (prefs.preset != EqualizerPreset.CUSTOM) {
            applyPreset(prefs.preset)
        } else if (prefs.bandLevels.isNotEmpty()) {
            prefs.bandLevels.forEachIndexed { idx, level ->
                setBandLevel(idx, level, markCustom = false)
            }
        }
        setBassBoost(prefs.bassBoostEnabled, prefs.bassBoostStrength)
        setVirtualizer(prefs.virtualizerEnabled, prefs.virtualizerStrength)
        setLoudnessEnhancer(prefs.loudnessEnabled, prefs.loudnessGainMb)
        setBalance(prefs.balance)
        _state.update { it.copy(activePreset = prefs.preset, balance = prefs.balance) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val eq = equalizer
        val actuallyEnabled = if (hwEqSupported && eq != null) {
            try {
                eq.enabled = enabled
                audioProcessor?.updateSoftwareEq(false, emptyList())
                eq.enabled
            } catch (_: Exception) {
                hwEqSupported = false
                audioProcessor?.updateSoftwareEq(enabled, _state.value.bands.map { it.levelMb })
                enabled
            }
        } else {
            audioProcessor?.updateSoftwareEq(enabled, _state.value.bands.map { it.levelMb })
            enabled
        }

        _state.update { it.copy(isEqualizerEnabled = actuallyEnabled) }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Short, markCustom: Boolean = true) {
        val min = _state.value.minLevelMb
        val max = _state.value.maxLevelMb
        val clamped = levelMb.coerceIn(min, max)

        val eq = equalizer
        if (hwEqSupported && eq != null && bandIndex < _state.value.bands.size) {
            try {
                eq.setBandLevel(bandIndex.toShort(), clamped)
            } catch (_: Exception) {
            }
        }

        _state.update { current ->
            val updatedBands = current.bands.map { band ->
                if (band.index == bandIndex) band.copy(levelMb = clamped) else band
            }
            if (!hwEqSupported) {
                audioProcessor?.updateSoftwareEq(current.isEqualizerEnabled, updatedBands.map { it.levelMb })
            }
            current.copy(
                bands = updatedBands,
                activePreset = if (markCustom) EqualizerPreset.CUSTOM else current.activePreset
            )
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        val current = _state.value
        val bands = current.bands
        if (bands.isEmpty()) {
            _state.update { it.copy(activePreset = preset) }
            return
        }

        val normalizedCurve: List<Float> = when (preset) {
            EqualizerPreset.FLAT -> List(bands.size) { 0f }
            EqualizerPreset.CLASSICAL -> listOf(0.45f, 0.25f, -0.1f, 0.35f, 0.55f)
            EqualizerPreset.POP -> listOf(-0.15f, 0.35f, 0.5f, 0.25f, -0.1f)
            EqualizerPreset.ROCK -> listOf(0.55f, 0.25f, -0.15f, 0.35f, 0.6f)
            EqualizerPreset.JAZZ -> listOf(0.35f, 0.2f, -0.1f, 0.4f, 0.3f)
            EqualizerPreset.HIP_HOP -> listOf(0.7f, 0.45f, 0.05f, 0.2f, 0.35f)
            EqualizerPreset.VOCAL -> listOf(-0.35f, -0.1f, 0.6f, 0.55f, 0.1f)
            EqualizerPreset.CUSTOM -> bands.map { 0f }
        }

        if (preset != EqualizerPreset.CUSTOM) {
            bands.forEachIndexed { i, _ ->
                val curveIdx = ((i.toFloat() / bands.size.coerceAtLeast(1)) * normalizedCurve.size)
                    .toInt().coerceIn(0, normalizedCurve.lastIndex)
                val weight = normalizedCurve[curveIdx]
                val targetMb = if (weight >= 0f) {
                    (weight * current.maxLevelMb).roundToInt().toShort()
                } else {
                    ((-weight) * current.minLevelMb).roundToInt().toShort()
                }
                setBandLevel(i, targetMb, markCustom = false)
            }
        }
        _state.update { it.copy(activePreset = preset) }
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val bb = bassBoost
        val actualEnabled = if (hwBassSupported && bb != null) {
            try {
                bb.setStrength(clamped.toShort())
                bb.enabled = enabled
                audioProcessor?.updateSoftwareBass(false, 0)
                bb.enabled
            } catch (_: Exception) {
                hwBassSupported = false
                audioProcessor?.updateSoftwareBass(enabled, clamped)
                enabled
            }
        } else {
            audioProcessor?.updateSoftwareBass(enabled, clamped)
            enabled
        }
        _state.update {
            it.copy(
                isBassBoostEnabled = actualEnabled,
                bassBoostStrength = clamped
            )
        }
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        val virt = virtualizer
        val actualEnabled = if (hwVirtSupported && virt != null) {
            try {
                virt.setStrength(clamped.toShort())
                virt.enabled = enabled
                audioProcessor?.updateSoftwareVirtualizer(false, 0)
                virt.enabled
            } catch (_: Exception) {
                hwVirtSupported = false
                audioProcessor?.updateSoftwareVirtualizer(enabled, clamped)
                enabled
            }
        } else {
            audioProcessor?.updateSoftwareVirtualizer(enabled, clamped)
            enabled
        }
        _state.update {
            it.copy(
                isVirtualizerEnabled = actualEnabled,
                virtualizerStrength = clamped
            )
        }
    }

    fun setLoudnessEnhancer(enabled: Boolean, gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1500)
        val loud = loudnessEnhancer
        val actualEnabled = if (hwLoudSupported && loud != null) {
            try {
                loud.setTargetGain(clamped)
                loud.enabled = enabled
                audioProcessor?.updateSoftwareLoudness(false, 0)
                loud.enabled
            } catch (_: Exception) {
                hwLoudSupported = false
                audioProcessor?.updateSoftwareLoudness(enabled, clamped)
                enabled
            }
        } else {
            audioProcessor?.updateSoftwareLoudness(enabled, clamped)
            enabled
        }
        _state.update {
            it.copy(
                isLoudnessEnabled = actualEnabled,
                loudnessGainMb = clamped
            )
        }
    }

    fun setBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        audioProcessor?.balance = clamped
        _state.update { it.copy(balance = clamped) }
    }

    fun toSavedPrefs(): SavedEqualizerPrefs {
        val s = _state.value
        return SavedEqualizerPrefs(
            enabled = s.isEqualizerEnabled,
            preset = s.activePreset,
            bandLevels = s.bands.map { it.levelMb },
            bassBoostEnabled = s.isBassBoostEnabled,
            bassBoostStrength = s.bassBoostStrength,
            virtualizerEnabled = s.isVirtualizerEnabled,
            virtualizerStrength = s.virtualizerStrength,
            loudnessEnabled = s.isLoudnessEnabled,
            loudnessGainMb = s.loudnessGainMb,
            balance = s.balance
        )
    }

    private fun releaseHardwareEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        hwEqSupported = false
        hwBassSupported = false
        hwVirtSupported = false
        hwLoudSupported = false
        currentSessionId = 0
    }

    fun release() {
        releaseHardwareEffects()
        audioProcessor?.updateSoftwareEq(false, emptyList())
        audioProcessor?.updateSoftwareBass(false, 0)
        audioProcessor?.updateSoftwareVirtualizer(false, 0)
        audioProcessor?.updateSoftwareLoudness(false, 0)
    }

    private fun formatFreq(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000f
            if (khz % 1f == 0f) "${khz.toInt()}kHz" else String.format(Locale.US, "%.1fkHz", khz)
        } else {
            "${hz}Hz"
        }
    }
}
