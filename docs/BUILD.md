# ساخت

## نیازمندیها

| مورد | نسخه |
| --- | --- |
| JDK | 21 (CI: Temurin 21) |
| Gradle | 9.6.0 (از طریق `gradlew` و `gradle-wrapper.jar` موجود) |
| Android SDK | platform 37 (`compileSdk`), build-tools، platform-tools |
| Kotlin | 2.3.21 (پلاگین Compose)، KSP 2.3.12، AGP 9.4.0 |
| minSdk / targetSdk | 26 / 36 |

نسخهها در `gradle/libs.versions.toml` متمرکزاند. AGP 9 پشتیبانی Kotlin را
داخلی دارد؛ بنابراین پلاگین `org.jetbrains.kotlin.android` اعمال **نمیشود**
(اعمال همزمان خطا میدهد). تنها پلاگینها:
`com.android.application`, `org.jetbrains.kotlin.plugin.compose`,
`com.google.devtools.ksp`, `com.google.dagger.hilt.android`.

منابع مورد نیاز Gradle از `google()` و `mavenCentral()` و پلاگینها از
`gradlePluginPortal()` میآیند (`settings.gradle.kts`).

## دستورات

```bash
chmod +x gradlew                              # در صورت نیاز
./gradlew :app:kspDebugKotlin                # تولید کد Room/Hilt + JSON اسکیما
./gradlew :app:testDebugUnitTest             # ۱۳ فایل آزمون JUnit روی JVM
./gradlew :app:lintDebug                     # تحلیل ایستای Android lint
./gradlew :app:assembleDebug                 # APK دیباگ
./gradlew :app:connectedDebugAndroidTest     # آزمونهای Room/اسکیما روی امولاتور
```

خروجیها:

* `app/build/outputs/apk/debug/app-debug.apk`
* `app/build/reports/tests/testDebugUnitTest/`
* `app/build/reports/lint-results-debug.html`
* `app/schemas/ir.hoboho.shooting.data.local.ShootingDatabase/1.json`
* `app/build/reports/androidTests/`

## نسخهٔ ریلیز

```bash
./gradlew :app:assembleRelease
```

هیچ کلید امضایی در مخزن نیست. `release` با `proguard-android-optimize.txt` و
`app/proguard-rules.pro` کوچک/فشرده میشود و APK امضانشده تحویل میدهد؛ امضا را
بهصورت محلی با `apksigner` یا Play App Signing انجام دهید. قوانین نگهداری
Room/Hilt/Compose در `app/proguard-rules.pro` آمده است.

## پیکربندیهای قابل توجه

* `androidResources { localeFilters += listOf("fa") }` — فقط فارسی ارسال میشود؛
  `MissingTranslation` در `lint.xml` خاموش است، چون فارسی زبان پایه است.
* `testOptions.unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true }`
* `ksp { arg("room.schemaLocation", "$projectDir/schemas"); arg("room.generateKotlin", "true") }`
* مانیفست: **بدون `INTERNET`**؛ `FileProvider` با authorities
  `${applicationId}.fileprovider` و `res/xml/file_paths.xml` فقط `filesDir` را
  در معرض اشتراک میگذارد.
* فونتهای Vazirmatn (Regular/Medium/Bold) در `app/src/main/res/font` و
  `assets/fonts` کپی شدهاند و هم در تم Compose و هم در موتور PDF استفاده میشوند.

## عیبیابی

* **خطای KSP/Room دربارهٔ اسکیما**: `app/schemas` را پاک نکنید؛ فایل JSON نسخهٔ
  قدیم مبنای آزمون مهاجرتهاست.
* **`Duplicate class` / نسخههای ناسازگار Compose**: از `platform(libs.androidx.compose.bom)`
  استفاده شده است؛ نسخهٔ کتابخانههای Compose را جدا تنظیم نکنید.
* **خطای Hilt**: `@HiltAndroidApp` روی `ShootingApplication`،
  `@AndroidEntryPoint` روی `MainActivity` و `@HiltViewModel` روی ViewModelها لازم
  است.

> وضعیت فعلی: در محیط توسعهای که این مخزن نوشته شد،
> `services.gradle.org`, `dl.google.com` و `repo.maven.apache.org` در دسترس
> نبودند؛ بنابراین دستورات بالا در آن محیط **اجرا نشدهاند**. گردشکار
> `.github/workflows/android.yml` آنها را روی runners دارای SDK اجرا میکند.
