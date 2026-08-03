package com.nudge.android.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val version: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String?,
)

sealed interface UpdateCheckResult {
    data class Available(val release: GitHubRelease) : UpdateCheckResult
    data class Current(val latestVersion: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object GitHubUpdateChecker {
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/YumiNoona/Nudge/releases/latest"

    suspend fun check(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "Nudge-Android/$currentVersion")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    val message = if (connection.responseCode == 404) "No GitHub release has been published yet" else "GitHub returned ${connection.responseCode}"
                    return@withContext UpdateCheckResult.Failed(message)
                }
                val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                val tag = json.optString("tag_name").trim().removePrefix("v").removePrefix("V")
                if (tag.isBlank()) return@withContext UpdateCheckResult.Failed("The latest release has no version tag")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").takeIf(String::isNotBlank)
                            break
                        }
                    }
                }
                val release = GitHubRelease(
                    version = tag,
                    title = json.optString("name").takeIf(String::isNotBlank) ?: "Nudge $tag",
                    notes = json.optString("body").trim(),
                    pageUrl = json.optString("html_url", "https://github.com/YumiNoona/Nudge/releases/latest"),
                    apkUrl = apkUrl,
                )
                if (VersionComparator.isNewer(tag, currentVersion)) {
                    UpdateCheckResult.Available(release)
                } else {
                    UpdateCheckResult.Current(tag)
                }
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            UpdateCheckResult.Failed(error.message ?: "Unable to reach GitHub")
        }
    }
}

object VersionComparator {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = numericParts(candidate)
        val currentParts = numericParts(current)
        val size = maxOf(candidateParts.size, currentParts.size)
        for (index in 0 until size) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }
        return false
    }

    private fun numericParts(version: String): List<Int> = version
        .trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
