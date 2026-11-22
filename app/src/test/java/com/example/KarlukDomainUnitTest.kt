package com.example

import com.example.core.common.FormatUtils
import com.example.domain.model.AppLanguage
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.LrcParser
import com.example.playback.audio.KarlukAudioProcessor
import com.example.playback.effects.AudioEffectsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarlukDomainUnitTest {

    @Test
    fun `lrc parser parses synced timestamps and finds active line`() {
        val raw = """
            [ar:Karluk]
            [00:05.00]گۈزەل ئاۋاز
            [00:12.50]ئاۋاز ھاياتنىڭ رەڭگى
            [00:25.00]ھەر كۈنى گۈزەل بولىدۇ
        """.trimIndent()

        val parsed = LrcParser.parse(songId = 42L, rawContent = raw)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 2_000L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 6_000L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 13_000L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, 30_000L))
    }

    @Test
    fun `lrc parser handles plain lyrics fallback`() {
        val raw = "Line one\nLine two"
        val parsed = LrcParser.parse(songId = 7L, rawContent = raw)
        assertFalse(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals("Line one", parsed.lines[0].text)
    }

    @Test
    fun `format utils formats duration and file size cleanly`() {
        assertEquals("04:15", FormatUtils.formatDuration(255_000L))
        assertEquals("1:01:05", FormatUtils.formatDuration(3_665_000L))
        assertEquals("1.5 MB", FormatUtils.formatFileSize(1_572_864L))
    }

    @Test
    fun `app language resolves codes accurately`() {
        assertEquals(AppLanguage.UYGHUR, AppLanguage.fromCode("ug"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromCode("tr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
    }

    @Test
    fun `audio effects manager provides software dsp bands and effects without hardware session`() {
        val processor = KarlukAudioProcessor()
        val manager = AudioEffectsManager(processor)
        val initial = manager.state.value

        assertTrue(initial.isEqualizerSupported)
        assertTrue(initial.isBassBoostSupported)
        assertTrue(initial.isVirtualizerSupported)
        assertTrue(initial.isLoudnessSupported)
        assertEquals(5, initial.bands.size)

        manager.setEqualizerEnabled(true)
        manager.applyPreset(EqualizerPreset.ROCK)
        manager.setBassBoost(true, 650)
        manager.setVirtualizer(true, 500)
        manager.setLoudnessEnhancer(true, 800)
        manager.setBalance(-0.25f)

        val updated = manager.state.value
        assertTrue(updated.isEqualizerEnabled)
        assertEquals(EqualizerPreset.ROCK, updated.activePreset)
        assertTrue(processor.softwareEqEnabled)
        assertTrue(processor.softwareBassEnabled)
        assertEquals(650, processor.softwareBassStrength)
        assertTrue(processor.softwareVirtualizerEnabled)
        assertTrue(processor.softwareLoudnessEnabled)
        assertEquals(-0.25f, processor.balance, 0.001f)
    }

    @Test
    fun `audio processor drives all 16 spectrum bands on stereo pcm audio`() {
        val processor = KarlukAudioProcessor()
        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        processor.flush()

        // Generate 512 stereo frames containing low + mid + high tones
        val frameCount = 512
        val buf = java.nio.ByteBuffer.allocateDirect(frameCount * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount) {
            val t = i / 44100.0
            val sig = 0.4 * kotlin.math.sin(2.0 * Math.PI * 180.0 * t) +
                0.35 * kotlin.math.sin(2.0 * Math.PI * 1800.0 * t) +
                0.3 * kotlin.math.sin(2.0 * Math.PI * 9000.0 * t)
            val s = (sig.coerceIn(-0.99, 0.99) * 32767.0).toInt().toShort()
            buf.putShort(s)
            buf.putShort(s)
        }
        buf.flip()
        processor.queueInput(buf)

        val bands = processor.spectrumFlow.value
        assertEquals(KarlukAudioProcessor.BAND_COUNT, bands.size)
        for (b in bands.indices) {
            assertTrue("Expected band $b to be active (> 0.05), got ${bands[b]}", bands[b] > 0.05f)
        }
    }
}
