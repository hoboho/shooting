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
shooting.versionBase=2.0
shooting.versionCodeBase=2000000
shooting.versionBuild=0
```

`app/build.gradle.kts` از روی آن ها حساب می کند:

* `versionName = 2.0.0 (release baseline; CI build number fixed to 0)`
* `versionCode = 2000000`

همان دو مقدار از طریق فیلد های تولید شدهٔ `BuildConfig` به `AppInfo` می رسند؛
صفحهٔ تنظیمات و metadata فایل پشتیبان دقیقاً نسخهٔ همان APK را نشان می دهند و
دیگر نیازی به هماهنگ کردن دستی چند فایل نیست. مقادیر پیش فرض فیلد های نسخه در
`BackupPayload` فقط مربوط به فایل های قدیمی/ناقص اند و نسخهٔ جاری برنامه
نیستند. نسخهٔ قالب پشتیبان (`BackupCodec.FORMAT_VERSION`) و نسخهٔ پایگاه داده
(`ShootingDatabase` v1) اعداد جداگانه ای هستند و ربطی به نسخهٔ APK ندارند.

نسخهٔ جاری برنامه `2.0.0` با `versionCode=2000000` است. گردشکار CI مقدار build را روی صفر ثابت نگه می دارد تا نام نسخه در تمام jobها دقیقاً `2.0.0` بماند. نسخهٔ قالب پشتیبان و نسخهٔ پایگاه داده مستقل از شمارهٔ APK هستند.

پیش انتشار `debug-latest` همواره حاوی APK **دیباگ** است و نباید به عنوان نصب نهایی استفاده شود. نسخهٔ Release امضا
شده مسیر جداگانه ای دارد: job `release` در CI آن را می سازد و با `apksigner`
اعتبارسنجی می کند، و job `publish_release` تنها با تأیید مالک مخزن آن را در
Release نسخهدار `v<versionName>-release` قرار می دهد. جزئیات و نام Secrets در
[`RELEASE_SIGNING.md`](RELEASE_SIGNING.md).

## لینک های دانلود پایدار

گردشکار CI در هر پوش به `main` (و نیز در اجرای دستی یا کامیتی با برچسب
`[publish-apk]`) دو فایل پیش انتشار `debug-latest` را بهروز می کند:

* APK: <https://github.com/hoboho/shooting/releases/download/debug-latest/Shooting-debug.apk>
* ZIP کامل همان اجرا (همهٔ artifact ها به همراه `BUILD-INFO.txt`):
  <https://github.com/hoboho/shooting/releases/download/debug-latest/Shooting-latest.zip>

برگهٔ پیش انتشار: <https://github.com/hoboho/shooting/releases/tag/debug-latest>.
هر دو ساخت دیباگ اند و برای نصب آزمایشی اند؛ نسخهٔ انتشار امضا شده محسوب
نمی شوند.

فهرست Release ها: <https://github.com/hoboho/shooting/releases>. Release نسخهدار
حاوی APK **امضا شده** تنها پس از تأیید مالک ساخته می شود؛ APK دیباگ هرگز به
عنوان Release منتشر نمی شود.

## نسخهٔ ریلیز (امضا شده)

```bash
set -a
source ~/.shooting-signing/credentials.env
set +a
export RELEASE_KEYSTORE_FILE="$HOME/.shooting-signing/shooting-release.jks"
./gradlew :app:assembleRelease
```

`release` با `proguard-android-optimize.txt` و `app/proguard-rules.pro`
کوچک/فشرده می شود. `app/build.gradle.kts` پیکربندی Signing را فقط از چهار
متغیر محیطی `RELEASE_KEYSTORE_FILE`، `RELEASE_KEYSTORE_PASSWORD`،
`RELEASE_KEY_ALIAS` و `RELEASE_KEY_PASSWORD` می خواند؛ هیچ کلید یا رمزی در مخزن
نیست. بدون این متغیرها APK ریلیز بدون امضا می ماند و CI از انتشار آن جلوگیری
می کند. در CI همان مقادیر از Secrets می آیند و APK حتماً با `apksigner verify`
بررسی می شود. قوانین نگهداری Room/Hilt/Compose در `app/proguard-rules.pro` و
همهٔ جزئیات امنیتی در [`RELEASE_SIGNING.md`](RELEASE_SIGNING.md) آمده است.

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
