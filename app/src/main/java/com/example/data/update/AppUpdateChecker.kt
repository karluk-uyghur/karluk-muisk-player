package com.example.data.update

import com.example.BuildConfig
import com.example.domain.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {
    const val TELEGRAM_CHANNEL_URL = "https://t.me/aplirim2026"
    private const val TELEGRAM_PUBLIC_WEB_URL = "https://t.me/s/aplirim2026"

    private val versionRegex = Regex("""(?i)(?:v|version|نەشرى|نەشىرى|karluk[^0-9]{0,16})?\s*\b(\d+\.\d+(?:\.\d+)?)\b""")

    suspend fun checkTelegramChannelForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(TELEGRAM_PUBLIC_WEB_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 KarlukMP3Pro/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }
            val code = conn.responseCode
            val html = try {
                if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw IllegalStateException("HTTP $code")
                }
            } finally {
                conn.disconnect()
            }

            parseChannelHtml(html, currentVersion)
        }
    }

    fun parseChannelHtml(
        html: String,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): AppUpdateInfo {
        if (html.isBlank()) {
            return AppUpdateInfo(
                latestVersion = currentVersion,
                currentVersion = currentVersion,
                releaseNotes = "",
                downloadUrl = TELEGRAM_CHANNEL_URL,
                isUpdateAvailable = false
            )
        }

        // Extract Telegram message texts, document titles, and channel description
        val textRegex = Regex(
            """<div class="tgme_widget_message_(?:text|document_title)[^"]*"[^>]*>(.*?)</div>|<div class="tgme_channel_info_description"[^>]*>(.*?)</div>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val postBlocks = textRegex
            .findAll(html)
            .map { match ->
                val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
                raw
                    .replace(Regex("""<br\s*/?>"""), "\n")
                    .replace(Regex("""<[^>]+>"""), " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace(Regex("""[ \t]+"""), " ")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .toList()

        var bestVersion = currentVersion
        var bestNotes = ""
        for (post in postBlocks) {
            val explicitMatches = Regex("""(?i)\bv?(\d+\.\d+\.\d+)\b|\bv(\d+\.\d+)\b""").findAll(post)
            for (m in explicitMatches) {
                val candidate = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
                if (compareVersions(candidate, bestVersion) > 0) {
                    bestVersion = candidate
                    bestNotes = post.take(280)
                } else if (bestNotes.isBlank() && compareVersions(candidate, currentVersion) == 0) {
                    bestNotes = post.take(280)
                }
            }
        }

        val hasUpdate = compareVersions(bestVersion, currentVersion) > 0
        return AppUpdateInfo(
            latestVersion = bestVersion,
            currentVersion = currentVersion,
            releaseNotes = bestNotes,
            downloadUrl = TELEGRAM_CHANNEL_URL,
            isUpdateAvailable = hasUpdate
        )
    }

    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.trimStart('v', 'V').split('.').mapNotNull { it.toIntOrNull() }
        val parts2 = v2.trimStart('v', 'V').split('.').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
