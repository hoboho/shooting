# ساخت

## نیازمندی ها

| مورد | نسخه |
| --- | --- |
| JDK | 21 (CI: Temurin 21) |
| Gradle | 9.6.0 (از طریق `gradlew` و `gradle-wrapper.jar` موجود) |
| Android SDK | platform 37 (`compileSdk`), build-tools، platform-tools |
| Kotlin | 2.3.21 (پلاگین Compose)، KSP 2.3.12، AGP 9.4.0 |
| minSdk / targetSdk | 26 / 36 |

نسخه ها در `gradle/libs.versions.toml` متمرکزاند. AGP 9 پشتیبانی Kotlin را
داخلی دارد؛ بنابراین پلاگین `org.jetbrains.kotlin.android` اعمال **نمی شود**
(اعمال همزمان خطا می دهد). تن ها پلاگین ها:
`com.android.application`, `org.jetbrains.kotlin.plugin.compose`,
`com.google.devtools.ksp`, `com.google.dagger.hilt.android`.

منابع مورد نیاز Gradle از `google()` و `mavenCentral()` و پلاگین ها از
`gradlePluginPortal()` میآیند (`settings.gradle.kts`).

## دستورات

```bash
chmod +x gradlew                              # در صورت نیاز
./gradlew :app:kspDebugKotlin                # تولید کد Room/Hilt + JSON اسکیما
./gradlew :app:testDebugUnitTest             # ۱۳ فایل آزمون JUnit روی JVM
./gradlew :app:lintDebug                     # تحلیل ایستای Android lint
./gradlew :app:assembleDebug                 # APK دیباگ
./gradlew :app:connectedDebugAndroidTest     # آزمونهای Room/اسکیما و جریانهای کاری روی امولاتور
```

خروجی ها:

* `app/build/outputs/apk/debug/app-debug.apk`
* `app/build/reports/tests/testDebugUnitTest/`
* `app/build/reports/lint-results-debug.html`
* `app/schemas/ir.hoboho.shooting.data.local.ShootingDatabase/1.json`
* `app/build/reports/androidTests/` و `app/build/outputs/androidTest-results/`
* artifact CI `qa-pdf-samples/` (سه PDF نمونهٔ تولیدشده توسط `WorkflowIntegrationTest`)

اسکیمای JSON توسط Room/KSP تولید می شود و در مخزن فقط
[`app/schemas/README.md`](../app/schemas/README.md) کامیت شده است؛ فایل `1.json`
در اجرا های [PR 41480196402](https://github.com/hoboho/shooting/actions/runs/41480196402)
و [push 41480188364](https://github.com/hoboho/shooting/actions/runs/41480188364)
بهصورت artifact `room-schema` موجود است. پیش از نخستین تغییر اسکیما، همان فایل
تولیدشده را کامیت کنید تا مبنای `MigrationTestHelper` باشد.

## نسخه گذاری

همهٔ مقادیر نسخه فقط در `gradle.properties` تعریف می شوند:

```properties
shooting.versionBase=1.0
shooting.versionCodeBase=1000
shooting.versionBuild=1
```

`app/build.gradle.kts` از روی آن ها حساب می کند:

* `versionName = <shooting.versionBase>.<shooting.versionBuild>`
* `versionCode = <shooting.versionCodeBase> + <shooting.versionBuild>`

همان دو مقدار از طریق فیلد های تولید شدهٔ `BuildConfig` به `AppInfo` می رسند؛
صفحهٔ تنظیمات و metadata فایل پشتیبان دقیقاً نسخهٔ همان APK را نشان می دهند و
دیگر نیازی به هماهنگ کردن دستی چند فایل نیست. مقادیر پیش فرض فیلد های نسخه در
`BackupPayload` فقط مربوط به فایل های قدیمی/ناقص اند و نسخهٔ جاری برنامه
نیستند. نسخهٔ قالب پشتیبان (`BackupCodec.FORMAT_VERSION`) و نسخهٔ پایگاه داده
(`ShootingDatabase` v1) اعداد جداگانه ای هستند و ربطی به نسخهٔ APK ندارند.

گردشکار CI پارامتر `-Pshooting.versionBuild=<github.run_number>` را به تمام
دستورات Gradle می دهد؛ بنابراین هر ساخت، نام نسخهٔ تازه و `versionCode` ای
بزرگ تر از ساخت قبل دارد. ساخت محلی بدون آن پارامتر `1.0.1 (1001)` می سازد.
برای تغییر خط نسخه (مثلاً به `1.1`) فقط `shooting.versionBase` را عوض کنید.

هر پوش به `main`، افزون بر `debug-latest`، یک release نسخه دار با برچسب
`v<versionName>` شامل APK و ZIP همان اجرا منتشر می کند. این ساخت دیباگ امضا
شده است، نه نسخهٔ انتشار امضا شده برای استور.

## لینک های دانلود پایدار

گردشکار CI در هر پوش به `main` (و نیز در اجرای دستی یا کامیتی با برچسب
`[publish-apk]`) دو فایل پیش انتشار `debug-latest` را بهروز می کند:

* APK: <https://github.com/hoboho/shooting/releases/download/debug-latest/Shooting-debug.apk>
* ZIP کامل همان اجرا (همهٔ artifact ها به همراه `BUILD-INFO.txt`):
  <https://github.com/hoboho/shooting/releases/download/debug-latest/Shooting-latest.zip>

برگهٔ پیش انتشار: <https://github.com/hoboho/shooting/releases/tag/debug-latest>.
هر دو ساخت دیباگ اند و برای نصب آزمایشی اند؛ نسخهٔ انتشار امضا شده محسوب
نمی شوند.

آخرین ساخت `main` همچنین به صورت release نسخه دار منتشر می شود:
<https://github.com/hoboho/shooting/releases>. برچسب آن `v<versionName>` است و
یادداشت release کامیت و شمارهٔ اجرای مربوطه را مشخص می کند.

## نسخهٔ ریلیز

```bash
./gradlew :app:assembleRelease
```

هیچ کلید امضایی در مخزن نیست. `release` با `proguard-android-optimize.txt` و
`app/proguard-rules.pro` کوچک/فشرده می شود و APK امضانشده تحویل می دهد؛ امضا را
بهصورت محلی با `apksigner` یا Play App Signing انجام دهید. قوانین نگهداری
Room/Hilt/Compose در `app/proguard-rules.pro` آمده است.

## پیکربندی های قابل توجه

* `androidResources { localeFilters += listOf("fa") }` — فقط فارسی ارسال می شود؛
 `MissingTranslation` در `lint.xml` خاموش است، چون فارسی زبان پایه است.
* `testOptions.unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true }`
* `ksp { arg("room.schemaLocation", "$projectDir/schemas"); arg("room.generateKotlin", "true") }`
* مانیفست: **بدون `INTERNET`**؛ `FileProvider` با authorities
 `${applicationId}.fileprovider` و `res/xml/file_paths.xml` فقط `filesDir` را
 در معرض اشتراک میگذارد.
* فونت های Vazirmatn (Regular/Medium/Bold) در `app/src/main/res/font` و
 `assets/fonts` کپی شدهاند و هم در تم Compose و هم در موتور PDF استفاده می شوند.

## عیبیابی

* **خطای KSP/Room دربارهٔ اسکیما**: `app/schemas` را پاک نکنید؛ فایل JSON نسخهٔ
 قدیم مبنای آزمون مهاجرتهاست.
* **`Duplicate class` / نسخه های ناسازگار Compose**: از `platform(libs.androidx.compose.bom)`
 استفاده شده است؛ نسخهٔ کتابخانه های Compose را جدا تنظیم نکنید.
* **خطای Hilt**: `@HiltAndroidApp` روی `ShootingApplication`،
 `@AndroidEntryPoint` روی `MainActivity` و `@HiltViewModel` روی ViewModelها لازم
 است.

## وضعیت اجرا

> **توجه:** وضعیت زیر مربوط به کامیت پیش از تغییرات اخیر (کیبورد عددی، ذخیرهٔ
> فایل ها، کور های استاندارد، تفکیک نوع کابل و اصلاح نگارشی) است؛ اجرا های تازهٔ
> این شاخه در برگهٔ Actions مخزن قابل بررسی اند.

در محیط توسعهٔ محلی، `services.gradle.org`، `dl.google.com` و
`repo.maven.apache.org` در دسترس نبودند و Android SDK/Gradle نصب نیست؛ بنابراین
`./gradlew` در آن محیط قابل اجرا نیست. ابزار های `scripts/run-offline-tests.sh`،
`scripts/check_sources.py` و `scripts/offline-compile-check/` فقط بررسی کمکیاند.

دستورات بالا روی runner دارای Android SDK در
[PR run 41480196402](https://github.com/hoboho/shooting/actions/runs/41480196402)
و [push run 41480188364](https://github.com/hoboho/shooting/actions/runs/41480188364)
اجرا و همگی با موفقیت کامل شدند: `testDebugUnitTest`، `lintDebug`,
`assembleDebug`، `kspDebugKotlin` و `connectedDebugAndroidTest` (API 34، ۱۲ آزمون
بدون شکست). artifactها شامل APK دیباگ، گزارش JUnit/lint/instrumentation،
اسکیمای Room و سه نمونهٔ PDFاند. اجرای قدیمی
[41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)
نخستین ساخت کامل را ثبت می کند. جزئیات دقیق و موارد باقیماندهٔ بررسی انسانی در
[`QA.md`](QA.md) و [`TESTING.md`](TESTING.md) ثبت شده است؛ عبور CI بهمعنای تأیید
بصری صفحات/PDF یا آمادگی انتشار نیست.
