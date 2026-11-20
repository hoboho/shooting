package ir.hoboho.shooting.feature.settings

/** Ignore malformed tags rather than letting one release break all update checks. */
internal fun latestReleaseTag(tags: List<String>): String? = tags
    .filter { tag ->
        tag.endsWith("-release") && runCatching {
            val version = tag.removeSuffix("-release")
            compareAppVersions(version, version)
        }.isSuccess
    }
    .maxWithOrNull { a, b ->
        compareAppVersions(a.removeSuffix("-release"), b.removeSuffix("-release"))
    }

internal fun compareAppVersions(left: String, right: String): Int {
    fun parts(value: String): List<Int> {
        val input = value.trim().removePrefix("v")
        require(input.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) { "شمارهٔ نسخه معتبر نیست: $value" }
        val parsed = input.split(".").map { it.toIntOrNull() }
        require(parsed.size == 3 && parsed.all { it != null }) {
            "شمارهٔ نسخه معتبر نیست: $value"
        }
        return parsed.filterNotNull()
    }
    val a = parts(left)
    val b = parts(right)
    for (index in 0..2) {
        val compared = a[index].compareTo(b[index])
        if (compared != 0) return compared
    }
    return 0
}
