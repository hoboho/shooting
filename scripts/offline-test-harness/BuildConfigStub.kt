package ir.hoboho.shooting

/**
 * Stand-in for the AGP-generated BuildConfig when the pure-Kotlin tests are run
 * without Gradle. The real values come from gradle.properties via BuildConfig.
 */
object BuildConfig {
    const val APP_VERSION_NAME: String = "1.0.1"
    const val APP_VERSION_CODE: Int = 1001
}
