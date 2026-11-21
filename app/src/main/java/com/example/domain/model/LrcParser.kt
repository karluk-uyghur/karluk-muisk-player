package com.example.domain.model

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(songId: Long, rawContent: String): LyricsData {
        if (rawContent.isBlank()) {
            return LyricsData(songId = songId, rawContent = "", lines = emptyList(), isSynced = false)
        }

        val timedLines = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<LyricLine>()

        rawContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lyricText = TIMESTAMP_REGEX.replace(trimmed, "").trim()
                if (lyricText.isNotEmpty()) {
                    matches.forEach { match ->
                        val min = match.groupValues[1].toLongOrNull() ?: 0L
                        val sec = match.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = match.groupValues[3]
                        val ms = when (fracStr.length) {
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            3 -> fracStr.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val totalMs = min * 60_000L + sec * 1_000L + ms
                        timedLines.add(LyricLine(timestampMs = totalMs, text = lyricText))
                    }
                }
            } else if (!trimmed.startsWith("[ar:") &&
                !trimmed.startsWith("[ti:") &&
                !trimmed.startsWith("[al:") &&
                !trimmed.startsWith("[by:") &&
                !trimmed.startsWith("[offset:")
            ) {
                plainLines.add(LyricLine(timestampMs = -1L, text = trimmed))
            }
        }

        return if (timedLines.isNotEmpty()) {
            LyricsData(
                songId = songId,
                rawContent = rawContent,
                lines = timedLines.sortedBy { it.timestampMs },
                isSynced = true
            )
        } else {
            LyricsData(
                songId = songId,
                rawContent = rawContent,
                lines = plainLines,
                isSynced = false
            )
        }
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs in 0..positionMs) {
                active = i
            } else if (lines[i].timestampMs > positionMs) {
                break
            }
        }
        return active
    }
}
