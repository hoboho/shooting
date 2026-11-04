plugins {
    // AGP 9 provides Kotlin support out of the box; applying
    // org.jetbrains.kotlin.android on top of it is an error.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// All version values come from gradle.properties, so the APK, the UI and the
// backup metadata can never disagree. CI passes -Pshooting.versionBuild=<run>.
val versionBase = findProperty("shooting.versionBase")?.toString() ?: "1.0"
val versionCodeBase = findProperty("shooting.versionCodeBase")?.toString()?.toIntOrNull() ?: 1000
val versionBuild = findProperty("shooting.versionBuild")?.toString()?.toIntOrNull() ?: 1
val shootingVersionName = "$versionBase.$versionBuild"
val shootingVersionCode = versionCodeBase + versionBuild

// Release signing material is NEVER stored in this repository. All four values
// come from the environment (GitHub Actions Secrets in CI, an exported shell
// locally). When any of them is absent the release build stays unsigned, and
// the workflow refuses to publish it instead of silently using a debug APK.
val releaseKeystoreFile = System.getenv("RELEASE_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
val releaseSigningReady =
    listOf(releaseKeystoreFile, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword)
        .all { !it.isNullOrBlank() } &&
        releaseKeystoreFile?.let { file(it).isFile } == true

android {
    namespace = "ir.hoboho.shooting"
    compileSdk = 37

    defaultConfig {
        applicationId = "ir.hoboho.shooting"
        minSdk = 26
        targetSdk = 36
        versionCode = shootingVersionCode
        versionName = shootingVersionName

        // Exposes the same version to Kotlin (settings screen and backup files).
        buildConfigField("String", "APP_VERSION_NAME", "\"$shootingVersionName\"")
        buildConfigField("int", "APP_VERSION_CODE", "$shootingVersionCode")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    // No signing keys are committed. Without the four environment values the
    // release variant stays unsigned and CI must not publish it
    // (see docs/RELEASE_SIGNING.md).
    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources { localeFilters += listOf("fa") }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "META-INF/*.kotlin_module",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all { it.useJUnit() }
        }
    }

    lint {
        // Persian is the only shipped locale; the default resource catalogue is
        // the Persian one, not a translation target.
        disable += "MissingTranslation"
        warningsAsErrors = false
        abortOnError = false
        lintConfig = file("lint.xml")
    }
}

// Room schema JSON is committed so migration tests can validate every version.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    // ------------------------------------------------------------------ tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
