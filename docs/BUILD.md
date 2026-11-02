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
* `app/build/reports/androidTests/` و `app/build/outputs/androidTest-results/`

اسکیمای JSON توسط Room/KSP تولید میشود و در مخزن فقط
[`app/schemas/README.md`](../app/schemas/README.md) کامیت شده است؛ فایل `1.json`
در [اجرای CI 41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)
بهصورت artifact `room-schema` موجود است. پیش از نخستین تغییر اسکیما، همان فایل
تولیدشده را کامیت کنید تا مبنای `MigrationTestHelper` باشد.

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

## وضعیت اجرا

در محیط توسعهٔ محلی، `services.gradle.org`، `dl.google.com` و
`repo.maven.apache.org` در دسترس نبودند و Android SDK/Gradle نصب نیست؛ بنابراین
`./gradlew` در آن محیط قابل اجرا نیست. ابزارهای `scripts/run-offline-tests.sh`،
`scripts/check_sources.py` و `scripts/offline-compile-check/` فقط بررسی کمکیاند.

دستورات بالا روی runner دارای Android SDK در
[GitHub Actions run 41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)
اجرا و همگی با موفقیت کامل شدند: `testDebugUnitTest`، `lintDebug`،
`assembleDebug`، `kspDebugKotlin` و `connectedDebugAndroidTest` (API 34، ۹ آزمون
بدون شکست). artifactها شامل APK دیباگ، گزارش JUnit/lint/instrumentation و
اسکیمای Roomاند. جزئیات دقیق و موارد باقیماندهٔ بررسی انسانی در
[`QA.md`](QA.md) و [`TESTING.md`](TESTING.md) ثبت شده است؛ عبور CI بهمعنای تأیید
بصری صفحات/PDF یا آمادگی انتشار نیست.
