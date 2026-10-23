package dev.ricky.galaxywatchtweaker.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleasesClient(
    private val owner: String = "crossly",
    private val repository: String = "galaxy-watch-tweaker",
) {
    fun fetchLatestRelease(): String {
        val url = URL("https://api.github.com/repos/$owner/$repository/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "GalaxyWatchTweaker")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("GitHub releases request failed: HTTP $status")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
