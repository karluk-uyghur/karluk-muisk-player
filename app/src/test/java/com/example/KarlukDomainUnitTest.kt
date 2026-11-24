package com.example

import com.example.core.common.FormatUtils
import com.example.domain.model.AppLanguage
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.LrcParser
import com.example.domain.model.RepeatModeOption
import com.example.domain.model.TextColorOption
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

    @Test
    fun `repeat mode option resolves persisted names safely`() {
        assertEquals(RepeatModeOption.OFF, RepeatModeOption.fromName("OFF"))
        assertEquals(RepeatModeOption.ALL, RepeatModeOption.fromName("ALL"))
        assertEquals(RepeatModeOption.ONE, RepeatModeOption.fromName("ONE"))
        assertEquals(RepeatModeOption.OFF, RepeatModeOption.fromName("INVALID"))
    }

    @Test
    fun `text color option resolves persisted ids cleanly`() {
        assertEquals(TextColorOption.AUTO, TextColorOption.fromId("auto"))
        assertEquals(TextColorOption.PURE_WHITE, TextColorOption.fromId("pure_white"))
        assertEquals(TextColorOption.SKY_CYAN, TextColorOption.fromId("sky_cyan"))
        assertEquals(TextColorOption.GOLD_AMBER, TextColorOption.fromId("gold_amber"))
        assertEquals(TextColorOption.EMERALD_MINT, TextColorOption.fromId("emerald_mint"))
        assertEquals(TextColorOption.ROSE_CORAL, TextColorOption.fromId("rose_coral"))
        assertEquals(TextColorOption.OBSIDIAN_BLACK, TextColorOption.fromId("obsidian_black"))
        assertEquals(TextColorOption.AUTO, TextColorOption.fromId("unknown_color"))
    }

    @Test
    fun `app update checker compares semantic versions accurately`() {
        assertTrue(com.example.data.update.AppUpdateChecker.compareVersions("1.2.1", "1.2.0") > 0)
        assertTrue(com.example.data.update.AppUpdateChecker.compareVersions("v2.0.0", "1.9.9") > 0)
        assertEquals(0, com.example.data.update.AppUpdateChecker.compareVersions("1.2.1", "v1.2.1"))
        assertTrue(com.example.data.update.AppUpdateChecker.compareVersions("1.2.0", "1.2.1") < 0)
    }

    @Test
    fun `app update checker parses newer release from telegram html`() {
        val html = """
            <div class="tgme_widget_message_text js-message_text" dir="auto">
                Karluk MP3 Pro v1.3.0 يېڭى نەشرى چىقتى<br/>ئەخلەت چېلىكى ۋە قولدا رەڭ تاللاش قوشۇلدى
            </div>
        """.trimIndent()
        val info = com.example.data.update.AppUpdateChecker.parseChannelHtml(html, currentVersion = "1.2.1")
        assertTrue(info.isUpdateAvailable)
        assertEquals("1.3.0", info.latestVersion)
        assertEquals("https://t.me/aplirim2026", info.downloadUrl)
    }

    @Test
    fun `audio content filter rejects dj remixes instrumentals and music while keeping nasheeds and vocal melodies`() {
        val filter = com.example.data.media.AudioContentFilter

        // Reject DJ / Remix / Club / EDM / Instrumental / Instrument solos across EN, UG, AR, TR
        assertTrue(filter.isExcludedMusicTrack(title = "Summer Club Mix DJ", artist = "DJ Tiesto"))
        assertTrue(filter.isExcludedMusicTrack(title = "ئۇسسۇل رېمىكىس دىجى 2026", displayName = "dj_mix.mp3"))
        assertTrue(filter.isExcludedMusicTrack(title = "Sad Piano Solo Instrumental", genre = "Instrumental"))
        assertTrue(filter.isExcludedMusicTrack(title = "دۇتتار چالغۇ مۇزىكىسى", artist = "چالغۇ"))
        assertTrue(filter.isExcludedMusicTrack(title = "موسيقى هادئة بالعود والكمان", artist = "عزف"))
        assertTrue(filter.isExcludedMusicTrack(title = "Hareketli Oyun Havası Bağlama", genre = "Pop"))

        // Preserve Nasheeds, Quran, Ilahi, vocal-only and melodies (including inside /Music/)
        assertFalse(
            filter.isExcludedMusicTrack(
                title = "گۈزەل نەشىد - تىنىچ ئاھاڭ",
                artist = "قارىم",
                album = "تۈركۈم 1",
                folderPath = "/storage/emulated/0/Music/Nasheeds"
            )
        )
        assertFalse(
            filter.isExcludedMusicTrack(
                title = "نەشىد مۇزىكىسىز پەقەت ئاۋاز",
                artist = "ئەلفۇرقان",
                displayName = "nasheed_no_music.mp3"
            )
        )
        assertFalse(
            filter.isExcludedMusicTrack(
                title = "نشيد جميل بدون موسيقى",
                artist = "مشاري",
                genre = "Nasheed"
            )
        )
        assertFalse(
            filter.isExcludedMusicTrack(
                title = "Güzel İlahi - Rapsodi Değil",
                artist = "Abdurrahman Önül",
                genre = "İlahi"
            )
        )
    }

    @Test
    fun `audio processor handles incomplete trailing pcm frame safely without buffer underflow`() {
        val processor = KarlukAudioProcessor()
        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        processor.flush()

        // 2 bytes is less than 1 full stereo 16-bit frame (4 bytes)
        val tinyBuf = java.nio.ByteBuffer.allocateDirect(2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        tinyBuf.putShort(1234.toShort())
        tinyBuf.flip()

        processor.queueInput(tinyBuf)
        val out = processor.output
        assertEquals(2, out.remaining())
    }
}
