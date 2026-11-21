package com.example

import com.example.core.common.FormatUtils
import com.example.domain.model.AppLanguage
import com.example.domain.model.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarlukDomainUnitTest {

    @Test
    fun `lrc parser parses synced timestamps and finds active line`() {
        val raw = """
            [ar:Karluk]
            [00:05.00]گۈزەل مۇزىكا
            [00:12.50]مۇزىكا ھاياتنىڭ رەڭگى
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
}
