package dev.ricky.galaxywatchtweaker.update

import org.json.JSONObject

data class ReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val title: String,
    val releaseUrl: String,
    val apkUrl: String?,
    val publishedAt: String?,
) {
    companion object {
        fun parse(json: String): ReleaseInfo {
            val root = JSONObject(json)
            val tag = root.optString("tag_name").trim()
            val code = VersionTags.code(tag)
                ?: throw IllegalArgumentException("Unsupported release tag: $tag")
            val name = root.optString("name").ifBlank { tag.removePrefix("v") }
            val htmlUrl = root.optString("html_url")
            val publishedAt = root.optString("published_at").ifBlank { null }
            val apkUrl = root.optJSONArray("assets")?.let { assets ->
                (0 until assets.length())
                    .asSequence()
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?.optString("browser_download_url")
                    ?.ifBlank { null }
            }
            return ReleaseInfo(
                versionName = tag.removePrefix("v"),
                versionCode = code,
                title = name,
                releaseUrl = htmlUrl,
                apkUrl = apkUrl,
                publishedAt = publishedAt,
            )
        }
    }
}
