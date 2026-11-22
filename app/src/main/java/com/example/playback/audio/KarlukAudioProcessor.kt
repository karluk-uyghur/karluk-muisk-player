package com.example.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Real-time PCM AudioProcessor attached directly to ExoPlayer's audio pipeline.
 * - Supports both PCM 16-bit and PCM Float streams.
 * - Computes genuine 16-band DFT spectral energy + RMS amplitude across all 16 bands
 *   so every visualizer bar reacts dynamically to audio volume and timbre.
 * - Applies software 5-band Biquad Peaking EQ + Low-Shelf Bass Boost + 3D Stereo Widener + Loudness Gain
 *   when hardware AudioEffects are unavailable, plus Left/Right stereo balance on all devices.
 */
@OptIn(UnstableApi::class)
class KarlukAudioProcessor : BaseAudioProcessor() {

    companion object {
        const val BAND_COUNT = 16
        val DEFAULT_EQ_FREQUENCIES_HZ = intArrayOf(60, 230, 910, 3600, 14000)
        private val ZERO_BANDS = FloatArray(BAND_COUNT) { 0f }
        private const val MAX_CHANNELS = 2
        private const val NUM_EQ_BANDS = 5
        private const val FFT_SAMPLES = 64
        // Bin indices mapped across 1..30 (Nyquist/2 region) with smooth spread so all 16 bars dance vividly
        private val DFT_BIN_MAP = intArrayOf(1, 2, 3, 4, 5, 6, 7, 9, 11, 13, 15, 18, 21, 24, 27, 30)
    }

    private val _spectrumFlow = MutableStateFlow(ZERO_BANDS)
    val spectrumFlow: StateFlow<FloatArray> = _spectrumFlow.asStateFlow()

    @Volatile
    var balance: Float = 0f // -1.0 (Left only) .. 0.0 (Center) .. +1.0 (Right only)

    @Volatile
    var softwareEqEnabled: Boolean = false

    @Volatile
    var softwareBandLevelsMb: IntArray = IntArray(NUM_EQ_BANDS) { 0 }

    @Volatile
    var softwareBassEnabled: Boolean = false

    @Volatile
    var softwareBassStrength: Int = 0 // 0..1000

    @Volatile
    var softwareVirtualizerEnabled: Boolean = false

    @Volatile
    var softwareVirtualizerStrength: Int = 0 // 0..1000

    @Volatile
    var softwareLoudnessEnabled: Boolean = false

    @Volatile
    var softwareLoudnessGainMb: Int = 0 // 0..1500

    private val smoothed = FloatArray(BAND_COUNT) { 0f }
    private val monoWindow = FloatArray(FFT_SAMPLES) { 0f }

    // Precomputed Hann window + cosine/sine DFT basis tables for zero-allocation audio thread analysis
    private val hannWindow = FloatArray(FFT_SAMPLES) { n ->
        (0.5 - 0.5 * cos(2.0 * PI * n / (FFT_SAMPLES - 1))).toFloat()
    }
    private val cosTable = Array(BAND_COUNT) { b ->
        val k = DFT_BIN_MAP[b]
        FloatArray(FFT_SAMPLES) { n ->
            cos(2.0 * PI * k * n / FFT_SAMPLES).toFloat()
        }
    }
    private val sinTable = Array(BAND_COUNT) { b ->
        val k = DFT_BIN_MAP[b]
        FloatArray(FFT_SAMPLES) { n ->
            sin(2.0 * PI * k * n / FFT_SAMPLES).toFloat()
        }
    }

    // Per-filter coefficients (b0, b1, b2, a1, a2 normalized by a0) for 5 EQ bands + 1 Bass shelf
    private class BiquadCoeff {
        @Volatile var b0 = 1f
        @Volatile var b1 = 0f
        @Volatile var b2 = 0f
        @Volatile var a1 = 0f
        @Volatile var a2 = 0f

        fun setIdentity() {
            b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
        }

        fun setPeaking(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f || freqHz >= sampleRate * 0.48f) {
                setIdentity()
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2f * q)

            val a0 = 1f + alpha / a
            b0 = (1f + alpha * a) / a0
            b1 = (-2f * cosW0) / a0
            b2 = (1f - alpha * a) / a0
            a1 = (-2f * cosW0) / a0
            a2 = (1f - alpha / a) / a0
        }

        fun setLowShelf(sampleRate: Float, freqHz: Float, gainDb: Float) {
            if (gainDb < 0.1f) {
                setIdentity()
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2f * sqrt(2f)
            val twoSqrtAAlpha = 2f * sqrt(a) * alpha

            val a0 = (a + 1f) + (a - 1f) * cosW0 + twoSqrtAAlpha
            b0 = (a * ((a + 1f) - (a - 1f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (2f * a * ((a - 1f) - (a + 1f) * cosW0)) / a0
            b2 = (a * ((a + 1f) - (a - 1f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (-2f * ((a - 1f) + (a + 1f) * cosW0)) / a0
            a2 = ((a + 1f) + (a - 1f) * cosW0 - twoSqrtAAlpha) / a0
        }
    }

    private class BiquadState {
        var x1 = 0f
        var x2 = 0f
        var y1 = 0f
        var y2 = 0f

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }

        inline fun process(sample: Float, c: BiquadCoeff): Float {
            val y = c.b0 * sample + c.b1 * x1 + c.b2 * x2 - c.a1 * y1 - c.a2 * y2
            x2 = x1
            x1 = sample
            y2 = y1
            y1 = y
            return y
        }
    }

    private val eqCoeffs = Array(NUM_EQ_BANDS) { BiquadCoeff() }
    private val bassCoeff = BiquadCoeff()
    private val eqStates = Array(MAX_CHANNELS) { Array(NUM_EQ_BANDS) { BiquadState() } }
    private val bassStates = Array(MAX_CHANNELS) { BiquadState() }

    @Volatile
    private var currentSampleRate = 44100f

    @Volatile
    private var coeffsDirty = true

    fun updateSoftwareEq(enabled: Boolean, bandLevelsMb: List<Short>) {
        softwareEqEnabled = enabled
        val copy = IntArray(NUM_EQ_BANDS) { idx ->
            bandLevelsMb.getOrNull(idx)?.toInt() ?: 0
        }
        softwareBandLevelsMb = copy
        coeffsDirty = true
    }

    fun updateSoftwareBass(enabled: Boolean, strength: Int) {
        softwareBassEnabled = enabled
        softwareBassStrength = strength.coerceIn(0, 1000)
        coeffsDirty = true
    }

    fun updateSoftwareVirtualizer(enabled: Boolean, strength: Int) {
        softwareVirtualizerEnabled = enabled
        softwareVirtualizerStrength = strength.coerceIn(0, 1000)
    }

    fun updateSoftwareLoudness(enabled: Boolean, gainMb: Int) {
        softwareLoudnessEnabled = enabled
        softwareLoudnessGainMb = gainMb.coerceIn(0, 1500)
    }

    private fun refreshCoefficientsIfNeeded() {
        if (!coeffsDirty) return
        coeffsDirty = false
        val sr = currentSampleRate.coerceAtLeast(8000f)

        val eqOn = softwareEqEnabled
        val levels = softwareBandLevelsMb
        for (b in 0 until NUM_EQ_BANDS) {
            val db = if (eqOn) (levels.getOrElse(b) { 0 } / 100f) else 0f
            eqCoeffs[b].setPeaking(sr, DEFAULT_EQ_FREQUENCIES_HZ[b].toFloat(), 1.25f, db)
        }

        val bassDb = if (softwareBassEnabled) (softwareBassStrength / 1000f) * 9.5f else 0f
        bassCoeff.setLowShelf(sr, 105f, bassDb)
    }

    fun resetSpectrum() {
        smoothed.fill(0f)
        _spectrumFlow.value = ZERO_BANDS
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (
            inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        ) {
            currentSampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
            coeffsDirty = true
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        refreshCoefficientsIfNeeded()

        val outputBuffer = replaceOutputBuffer(remaining)
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val isFloatPcm = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerSample = if (isFloatPcm) 4 else 2
        val totalSamples = remaining / bytesPerSample
        val frameCount = (totalSamples / channels).coerceAtLeast(1)

        val currentBalance = balance.coerceIn(-1f, 1f)
        val leftGain = if (currentBalance > 0f) (1f - currentBalance) else 1f
        val rightGain = if (currentBalance < 0f) (1f + currentBalance) else 1f

        val applyEq = softwareEqEnabled
        val applyBass = softwareBassEnabled && softwareBassStrength > 0
        val applyVirt = channels == 2 && softwareVirtualizerEnabled && softwareVirtualizerStrength > 0
        val virtWidth = 1f + (softwareVirtualizerStrength / 1000f) * 0.85f
        val applyLoudness = softwareLoudnessEnabled && softwareLoudnessGainMb > 0
        val loudnessLinear = if (applyLoudness) 10.0.pow(softwareLoudnessGainMb / 2000.0).toFloat() else 1f
        val needsSoftLimiter = applyEq || applyBass || applyVirt || applyLoudness

        val readBuffer = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        monoWindow.fill(0f)
        var energySumSq = 0f
        val sampleStep = (frameCount.toFloat() / FFT_SAMPLES).coerceAtLeast(1f)
        var nextCaptureFrame = 0f
        var capturedCount = 0

        if (channels == 2) {
            for (frameIdx in 0 until frameCount) {
                var left = if (isFloatPcm) readBuffer.float else (readBuffer.short / 32768f)
                var right = if (isFloatPcm) readBuffer.float else (readBuffer.short / 32768f)

                if (applyEq) {
                    val lStates = eqStates[0]
                    val rStates = eqStates[1]
                    for (b in 0 until NUM_EQ_BANDS) {
                        left = lStates[b].process(left, eqCoeffs[b])
                        right = rStates[b].process(right, eqCoeffs[b])
                    }
                }

                if (applyBass) {
                    left = bassStates[0].process(left, bassCoeff)
                    right = bassStates[1].process(right, bassCoeff)
                }

                if (applyVirt) {
                    val mid = 0.5f * (left + right)
                    val side = 0.5f * (left - right)
                    left = mid + side * virtWidth
                    right = mid - side * virtWidth
                }

                if (applyLoudness) {
                    left *= loudnessLinear
                    right *= loudnessLinear
                }

                if (currentBalance != 0f) {
                    left *= leftGain
                    right *= rightGain
                }

                if (needsSoftLimiter) {
                    if (abs(left) > 0.92f) left = tanh(left.toDouble()).toFloat()
                    if (abs(right) > 0.92f) right = tanh(right.toDouble()).toFloat()
                }

                if (isFloatPcm) {
                    outputBuffer.putFloat(left.coerceIn(-1f, 1f))
                    outputBuffer.putFloat(right.coerceIn(-1f, 1f))
                } else {
                    val outL = (left * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    val outR = (right * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    outputBuffer.putShort(outL.toShort())
                    outputBuffer.putShort(outR.toShort())
                }

                val mono = 0.5f * (left + right)
                energySumSq += mono * mono
                if (capturedCount < FFT_SAMPLES && frameIdx >= nextCaptureFrame.toInt()) {
                    monoWindow[capturedCount++] = mono
                    nextCaptureFrame += sampleStep
                }
            }
        } else {
            for (i in 0 until totalSamples) {
                var sample = if (isFloatPcm) readBuffer.float else (readBuffer.short / 32768f)
                val ch = i % channels
                if (ch < MAX_CHANNELS) {
                    if (applyEq) {
                        val states = eqStates[ch]
                        for (b in 0 until NUM_EQ_BANDS) {
                            sample = states[b].process(sample, eqCoeffs[b])
                        }
                    }
                    if (applyBass) {
                        sample = bassStates[ch].process(sample, bassCoeff)
                    }
                }
                if (applyLoudness) {
                    sample *= loudnessLinear
                }
                if (needsSoftLimiter && abs(sample) > 0.92f) {
                    sample = tanh(sample.toDouble()).toFloat()
                }

                if (isFloatPcm) {
                    outputBuffer.putFloat(sample.coerceIn(-1f, 1f))
                } else {
                    val outSample = (sample * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    outputBuffer.putShort(outSample.toShort())
                }

                if (ch == 0) {
                    val frameIdx = i / channels
                    energySumSq += sample * sample
                    if (capturedCount < FFT_SAMPLES && frameIdx >= nextCaptureFrame.toInt()) {
                        monoWindow[capturedCount++] = sample
                        nextCaptureFrame += sampleStep
                    }
                }
            }
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()

        // Compute real 16-bin DFT energy blended with buffer RMS loudness so all 16 bars react together
        val bufferRms = sqrt(energySumSq / frameCount).coerceIn(0f, 1f)
        val volumeFloor = (bufferRms * 1.45f).coerceIn(0f, 0.72f)
        val activeLen = capturedCount.coerceAtLeast(1)

        var updated = false
        val next = FloatArray(BAND_COUNT)
        for (b in 0 until BAND_COUNT) {
            var re = 0f
            var im = 0f
            val cosRow = cosTable[b]
            val sinRow = sinTable[b]
            for (n in 0 until activeLen) {
                val wSample = monoWindow[n] * hannWindow[n]
                re += wSample * cosRow[n]
                im -= wSample * sinRow[n]
            }
            val mag = sqrt(re * re + im * im) / (activeLen * 0.25f)
            // High frequencies naturally carry less energy in speech/nasheeds; apply gentle tilt compensation
            val tiltGain = 1.0f + (b / (BAND_COUNT - 1f)) * 1.15f
            val target = (mag * tiltGain * 1.35f + volumeFloor * 0.42f).coerceIn(0f, 1f)

            // Fast attack on vocal/beat spikes, smooth liquid decay
            val alpha = if (target > smoothed[b]) 0.62f else 0.26f
            smoothed[b] = smoothed[b] * (1f - alpha) + target * alpha
            next[b] = smoothed[b]
            if (next[b] > 0.006f) updated = true
        }
        _spectrumFlow.value = if (updated) next else ZERO_BANDS
    }

    override fun onFlush() {
        for (ch in 0 until MAX_CHANNELS) {
            for (b in 0 until NUM_EQ_BANDS) eqStates[ch][b].reset()
            bassStates[ch].reset()
        }
    }

    override fun onReset() {
        onFlush()
        resetSpectrum()
    }
}
