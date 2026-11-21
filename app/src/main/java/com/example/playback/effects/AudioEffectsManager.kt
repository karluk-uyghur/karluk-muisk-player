package com.example.playback.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.data.datastore.SavedEqualizerPrefs
import com.example.domain.model.AudioBand
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class AudioEffectsManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentSessionId: Int = 0
    private var lastSavedPrefs: SavedEqualizerPrefs = SavedEqualizerPrefs()

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    fun attachToSession(audioSessionId: Int, savedPrefs: SavedEqualizerPrefs = lastSavedPrefs) {
        lastSavedPrefs = savedPrefs
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null) {
            applySavedPrefs(savedPrefs)
            return
        }

        release()
        currentSessionId = audioSessionId

        var eqSupported = false
        val bands = mutableListOf<AudioBand>()
        var minMb: Short = -1500
        var maxMb: Short = 1500

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            eqSupported = true
            val range = eq.bandLevelRange
            if (range != null && range.size >= 2) {
                minMb = range[0]
                maxMb = range[1]
            }
            val count = eq.numberOfBands.toInt()
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
        } catch (_: Exception) {
            eqSupported = false
        }

        val bassSupported = try {
            val bb = BassBoost(0, audioSessionId)
            bassBoost = bb
            bb.strengthSupported
        } catch (_: Exception) {
            false
        }

        val virtSupported = try {
            val virt = Virtualizer(0, audioSessionId)
            virtualizer = virt
            virt.strengthSupported
        } catch (_: Exception) {
            false
        }

        val loudSupported = try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            true
        } catch (_: Exception) {
            false
        }

        _state.update {
            it.copy(
                isEqualizerSupported = eqSupported,
                bands = bands,
                minLevelMb = minMb,
                maxLevelMb = maxMb,
                isBassBoostSupported = bassSupported,
                isVirtualizerSupported = virtSupported,
                isLoudnessSupported = loudSupported
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
        _state.update { it.copy(activePreset = prefs.preset, balance = prefs.balance) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val eq = equalizer
        val actuallyEnabled = if (eq != null) {
            try {
                eq.enabled = enabled
                eq.enabled
            } catch (_: Exception) {
                false
            }
        } else false

        _state.update { it.copy(isEqualizerEnabled = actuallyEnabled) }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Short, markCustom: Boolean = true) {
        val eq = equalizer
        val min = _state.value.minLevelMb
        val max = _state.value.maxLevelMb
        val clamped = levelMb.coerceIn(min, max)

        if (eq != null && bandIndex < _state.value.bands.size) {
            try {
                eq.setBandLevel(bandIndex.toShort(), clamped)
            } catch (_: Exception) {
            }
        }

        _state.update { current ->
            val updatedBands = current.bands.map { band ->
                if (band.index == bandIndex) band.copy(levelMb = clamped) else band
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
        var actualEnabled = false
        if (bb != null && _state.value.isBassBoostSupported) {
            try {
                bb.setStrength(clamped.toShort())
                bb.enabled = enabled
                actualEnabled = bb.enabled
            } catch (_: Exception) {
            }
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
        var actualEnabled = false
        if (virt != null && _state.value.isVirtualizerSupported) {
            try {
                virt.setStrength(clamped.toShort())
                virt.enabled = enabled
                actualEnabled = virt.enabled
            } catch (_: Exception) {
            }
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
        var actualEnabled = false
        if (loud != null && _state.value.isLoudnessSupported) {
            try {
                loud.setTargetGain(clamped)
                loud.enabled = enabled
                actualEnabled = loud.enabled
            } catch (_: Exception) {
            }
        }
        _state.update {
            it.copy(
                isLoudnessEnabled = actualEnabled,
                loudnessGainMb = clamped
            )
        }
    }

    fun setBalance(balance: Float) {
        _state.update { it.copy(balance = balance.coerceIn(-1f, 1f)) }
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

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentSessionId = 0
    }

    private fun formatFreq(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000f
            if (khz % 1f == 0f) "${khz.toInt()}kHz" else String.format("%.1fkHz", khz)
        } else {
            "${hz}Hz"
        }
    }
}
