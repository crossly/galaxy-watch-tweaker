package dev.ricky.galaxywatchtweaker.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReleaseInfoTest {
    @Test
    fun versionCodeMatchesReleaseWorkflowFormula() {
        assertEquals(104, VersionTags.code("v0.1.4"))
        assertEquals(104, VersionTags.code("0.1.4"))
        assertEquals(101000, VersionTags.code("v10.10.0"))
        assertNull(VersionTags.code("release"))
        assertNull(VersionTags.code(null))
        assertNull(VersionTags.code("v0.1"))
    }

    @Test
    fun parsesLatestReleaseWithApkAsset() {
        val json = """
            {
              "tag_name": "v0.1.5",
              "name": "v0.1.5 — fixes",
              "html_url": "https://github.com/crossly/galaxy-watch-tweaker/releases/tag/v0.1.5",
              "published_at": "2026-11-01T00:00:00Z",
              "assets": [
                {"name": "source.zip", "browser_download_url": "https://example.com/source.zip"},
                {"name": "GalaxyWatchTweaker-0.1.5-release-signed.apk", "browser_download_url": "https://example.com/app.apk"}
              ]
            }
        """.trimIndent()
        val release = ReleaseInfo.parse(json)
        assertEquals("0.1.5", release.versionName)
        assertEquals(105, release.versionCode)
        assertEquals("v0.1.5 — fixes", release.title)
        assertEquals("https://example.com/app.apk", release.apkUrl)
        assertEquals("https://github.com/crossly/galaxy-watch-tweaker/releases/tag/v0.1.5", release.releaseUrl)
        assertEquals("2026-11-01T00:00:00Z", release.publishedAt)
    }

    @Test
    fun fallsBackToReleaseUrlWhenNoApkAsset() {
        val json = """
            {"tag_name":"v0.1.5","name":"","html_url":"https://example.com/release","published_at":"","assets":[]}
        """.trimIndent()
        val release = ReleaseInfo.parse(json)
        assertEquals("0.1.5", release.title)
        assertNull(release.apkUrl)
        assertNull(release.publishedAt)
    }

    @Test
    fun rejectsReleasesWithoutAVersionTag() {
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseInfo.parse("""{"tag_name":"latest","html_url":"https://example.com"}""")
        }
    }
}
