package dev.ricky.galaxywatchtweaker.update

object VersionTags {
    private val PATTERN = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")

    /** Mirrors the release workflow: major * 10000 + minor * 100 + patch. */
    fun code(tag: String?): Int? {
        val match = PATTERN.matchEntire(tag?.trim().orEmpty()) ?: return null
        val (major, minor, patch) = match.destructured
        return major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
    }
}
