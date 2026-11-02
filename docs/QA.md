# وضعیت کنترل کیفیت

این سند صرفاً وضعیت **واقعی اجرا** را ثبت میکند. عبور یک مرحله فقط زمانی علامت
مییابد که فرمان مربوطه اجرا شده و خروجی موفق آن دیده شده باشد. موفقیت ساخت و
آزمونهای خودکار، تأیید کیفیت بصری یا آمادگی انتشار نیست.

## اجرا شده در محیط توسعهٔ محلی (بدون Android SDK)

| مورد | فرمان | نتیجه |
| --- | --- | --- |
| کامپایل منابع خالص Kotlin (۲۵ فایل) + ۱۳ فایل آزمون | `KOTLINC=... bash scripts/run-offline-tests.sh` | `exit 0` |
| اجرای ۶۲ آزمون منطق JVM | همان اسکریپت (`TestRunnerKt`) | `62 passed, 0 failed`، `exit 0` |
| بررسی ایستای منابع/importها/ناوبری/قواعد حیاتی | `python3 scripts/check_sources.py` | `0 error(s), 0 warning(s)` |
| کامپایل ۴۲ فایل `core/domain/data/di` (kotlinc + استاب API) | `bash scripts/offline-compile-check/run.sh` | `exit 0` |
| بررسی صحت تقویم جلالی در برابر `jdatetime` | مقایسهٔ جدول نوروز و سالهای کبیسه | لنگرها و کبیسهها مطابق |

این ابزارها شامل کامپایل اندروید، JUnit/Gradle، Android lint، APK، آزمونهای
instrumentation یا کامپایل صفحات Compose نمیشوند. در این محیط
`services.gradle.org`، `dl.google.com` و `repo.maven.apache.org` پاسخ نمیدهند و
Android SDK/Gradle/AGP نصب نیست؛ بنابراین `./gradlew` بهصورت محلی قابل اجرا نیست.

## اجرا شده در GitHub Actions (runner دارای Android SDK)

آخرین اجرای کامل:
[run 41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)
روی شاخهٔ `arena/674b93c5-shouting`. نتیجهٔ هر سه job و همهٔ stepها `success`
است.

| Job | شناسه | فرمانها / بررسیها | نتیجه |
| --- | --- | --- | --- |
| Offline source checks | 125112199066 | `python3 scripts/check_sources.py` | `success` |
| JVM tests, lint & debug APK | 125112285994 | `./gradlew :app:testDebugUnitTest`، `:app:lintDebug`، `:app:assembleDebug`، `:app:kspDebugKotlin` | `success`؛ APK، گزارش JUnit/lint و JSON اسکیمای Room بارگذاری شدند |
| Room/repository tests (API 34) | 125112804915 | `./gradlew :app:connectedDebugAndroidTest` روی امولاتور API 34 | `success`؛ خروجی XML: `tests="9" failures="0" errors="0" skipped="0"` |

artifactهای همان اجرا: `shooting-debug-apk`، `jvm-unit-test-report`،
`lint-report`، `room-schema` و `instrumented-test-report`. اسکیمای نسخهٔ ۱ توسط
Room/KSP تولید شده و همراه artifact است؛ فایل JSON هنوز به مخزن کامیت نشده است و
پیش از نخستین تغییر اسکیما باید اضافه شود (به [`app/schemas/README.md`](../app/schemas/README.md)
مراجعه کنید). نسخهٔ فعلی پایگاه داده v1 و `Migrations.ALL` خالی است، بنابراین
فعلاً آزمون `MigrationTestHelper` بین دو نسخه موضوعیت ندارد.

اعلانهای runner دربارهٔ منسوخشدن Node.js 20 برای برخی actionها،
`actions/setup-java@v4` و مهاجرت آیندهٔ برچسب `ubuntu-latest` هشدار نسخهٔ
زیرساخت CI هستند، نه خطای کامپایل، لینت یا آزمون اپلیکیشن.

بر این اساس مراحل خودکار زیر انجام شدهاند:

- [x] `./gradlew :app:testDebugUnitTest` (JUnit واقعی روی runner)
- [x] کامپایل همهٔ منابع Kotlin اندروید (Compose/Room/Hilt/KSP) و رفع خطاها
- [x] `./gradlew :app:lintDebug` و عبور تحلیل ایستا
- [x] `./gradlew :app:assembleDebug` و تولید APK دیباگ
- [x] `./gradlew :app:connectedDebugAndroidTest` (دو فایل آزمون Room/اسکیما، ۹ آزمون)
- [x] تولید اسکیمای JSON نسخهٔ ۱ و بررسی شکل v1 روی SQLite واقعی
- [ ] نصب و اجرای APK دیباگ روی دستگاه فیزیکی و بررسی رفتار واقعی اپلیکیشن

## انجامنشده / نیازمند بررسی انسانی

این موارد با عبور CI تأیید نمیشوند و هنوز اجرا نشدهاند:

- [ ] بازبینی بصری تکتک صفحات: داشبورد، پروژهها، ویرایش پروژه، فهرست/ویرایش/جزئیات
  گزارش روزانه، هزینهها، ویرایش هزینه و تسویهها، گزارشها، تنظیمات، دادههای مرجع،
  پشتیبانگیری
- [ ] جریان کامل روزانه: پروژه → گزارش → افزودن/حذف عملیات → مشاهدهٔ طول خودکار
  (شامل قرقرهٔ معکوس) → ذخیره → جلوگیری از تکرار همان تاریخ → بازگشایی و ویرایش
- [ ] جریان هزینه و تسویه: ثبت هزینه، چند تسویهٔ جزئی/کامل، جلوگیری از بیشبود،
  حذف، صحت مانده و خلاصهٔ داشبورد
- [ ] تولید و **بازبینی بصری** هر سه PDF (روزانه، دورهای، هزینهها) از نظر RTL،
  شکلگیری حروف، ارقام فارسی، تکرار سرستونها و صفحهبندی
- [ ] جریان پشتیبان/بازیابی: ساخت فایل، بررسی checksum، پیشنمایش، سه راهبرد،
  نسخهٔ ایمنی، عدم از دست رفتن داده
- [ ] بررسی حالت تاریک، چرخش صفحه، اندازههای مختلف و دسترسپذیری
- [ ] پس از نخستین تغییر اسکیما: کامیت JSON نسخهٔ قدیم/جدید و اجرای آزمون
  `MigrationTestHelper` با migration افزودنی صریح

## یادآوری

یک ساخت APK موفق یا عبور کامل CI بهتنهایی اثبات کیفیت نهایی نیست. تا زمان تیک
خوردن موارد انسانی بالا، نمیتوان اپلیکیشن را «آمادهٔ انتشار» اعلام کرد. README،
[`TESTING.md`](TESTING.md) و [`BUILD.md`](BUILD.md) نیز همین وضعیت را منعکس
میکنند؛ هر تغییر بعدی در وضعیت اجراها باید همراه آنها بهروز شود.
