# آزمونها

## لایهها

| لایه | مسیر | اجرا |
| --- | --- | --- |
| منطق خالص JVM | `app/src/test` (۱۳ فایل، ۶۲ آزمون) | `./gradlew :app:testDebugUnitTest` |
| منابع/ناوبری/قواعد ایستا | `scripts/check_sources.py` | `python3 scripts/check_sources.py` |
| کامپایل منطق/داده با استاب | `scripts/offline-compile-check/` | `bash scripts/offline-compile-check/run.sh` |
| Android lint | قواعد `app/lint.xml` | `./gradlew :app:lintDebug` |
| Room/مخازن/اسکیما روی SQLite | `app/src/androidTest` (۲ فایل) | `./gradlew :app:connectedDebugAndroidTest` |
| ساخت | — | `./gradlew :app:assembleDebug` |

## پوشش آزمونهای JVM

| فایل | موضوع |
| --- | --- |
| `JalaliCalendarTest`, `DateRangeExtraTest` | جدول نوروز/سالهای کبیسه، لنگرهای ۱۴۰۳/۱۴۰۴/۱۴۰۵، تبدیل دوسویه، مرز ماه/سال/هفته، شمول بازه، `fromEpochMilli` |
| `FormatterTest`, `NumberFormatterExtraTest` | ارقام فارسی/عربی، جداکنندهها، گروهبندی، واحد متر/تومان، رد صریح منفی/اعشار/ورودی غیرعددی، عدم پنهانسازی ورودی نامعتبر |
| `ShootingCalculatorTest` | `abs(end−start)`، اختلاف منفی مجاز، بهروزرسانی طول با تغییر هر کنتور، عدم سرریز، رد پیشنویس ناقص، `DailyTotal = ΣL` |
| `ValidatorTest` | نام پروژه/تکراری، جلوگیری از گزارش تکراری همان پروژه+تاریخ، الزام پروژه/تاریخ/عملیات، اعتبار سطر به سطر، پیشفرض ۰ کارگر |
| `ExpenseCalculatorTest` | تسویهنشده/جزئی/کامل، مانده، جلوگیری از تسویهٔ بیش از مانده، تجمع صحیح تومان |
| `PeriodAggregatorTest` | فیلتر بازه و پروژه، تفکیک روزانه/پروژه، **عدم ترکیب طول و پول** |
| `ReportSpecBuilderTest` | گزارش روزانه فقط جمع طول (بدون تعداد عملیات/تفکیک کابل)، تفکیک بلوک طول و مبالغ در گزارش دورهای، فهرست تسویهها و تومان صحیح |
| `SimpleJsonTest`, `BackupCodecTest`, `BackupMapperTest`, `RestoreModelsTest` | JSON قطعی، MAGIC/نسخه/SHA-256/دستکاری checksum، نگاشت رفتوبرگشت همهٔ موجودیتها، پرچمهای صریح راهبرد |

## آزمونهای on-device (اجراشده در CI)

`ShootingDatabaseTest` — seed دادههای مرجع و افزودن کور جدید بدون مهاجرت،
ذخیرهٔ گزارش و محاسبهٔ مجدد طولها/جمع، جلوگیری از تکرار پروژه+تاریخ، جلوگیری از
تسویهٔ بیش از مانده و حذف هزینهٔ دارای تسویه، بازیابی `ADD_ONLY` بدون تکرار/حذف.
`DatabaseSchemaTest` — هشت جدول، ستونهای عددی تاریخ/مبلغ، ایندکس یکتا و اعمال FK
روی SQLite واقعی.

در [run 41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)،
job سوم `connectedDebugAndroidTest` را روی امولاتور API 34 اجرا کرد؛ خروجی XML
چاپشده `tests="9" failures="0" errors="0" skipped="0"` است.

## اجرای آفلاین (بدون Gradle/Android SDK)

```bash
KOTLINC=/path/to/kotlinc bash scripts/run-offline-tests.sh
python3 scripts/check_sources.py
KOTLINC=/path/to/kotlinc COROUTINES_JAR=/path/to/kotlinx-coroutines-core-jvm.jar \
  bash scripts/offline-compile-check/run.sh
```

اسکریپت اول فایلهای خالص `core/` + `domain/` بههمراه ۱۳ فایل آزمون و دو فایل
`scripts/offline-test-harness/` را با `kotlinc` کامپایل میکند و با یک
اجراکنندهٔ بازتابی کوچک اجرا مینماید. آزمونها خودشان از API استاندارد
`org.junit.Test` / `org.junit.Assert` استفاده میکنند، بنابراین در Gradle بدون
هیچ تغییری با JUnit واقعی اجرا میشوند.

اسکریپت سوم ۴۲ فایل `core/`, `domain/`, `data/`, `di/` را با `kotlinc` واقعی،
`kotlinx-coroutines-core` واقعی و استابهای کوچک Android/Room/Hilt کامپایل
میکند؛ خطاهای نوع/`suspend`/ارجاع در لایهٔ داده را میگیرد ولی **کامپایل
اندروید، KSP، Hilt codegen یا Compose نیست** (جزئیات در
`scripts/offline-compile-check/README.md`).

**نتیجهٔ واقعی ابزارهای آفلاین (محیط محلی):** کامپایل `exit 0`،
`62 passed, 0 failed`، `exit 0`؛ بررسیهای ایستا `0 error, 0 warning`؛ کامپایل
استابی ۴۲ فایل `exit 0`. هیچکدام اجرای JUnit/Gradle یا کامپایل اندروید **نیست**
و نباید بهعنوان چنین گزارش شود.

## اجرای واقعی روی GitHub Actions

گردشکار [`.github/workflows/android.yml`](../.github/workflows/android.yml) روی
runner دارای Android SDK اجرا شد؛
[run 41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)
نتیجهٔ `success` برای هر سه job دارد:

1. **Offline source checks**: `check_sources.py`.
2. **JVM tests, lint & debug APK**: `testDebugUnitTest` با JUnit واقعی،
   `lintDebug`، `assembleDebug` و `kspDebugKotlin` برای تولید اسکیمای Room.
3. **Room/repository tests (API 34)**: `connectedDebugAndroidTest` و چاپ XML
   نتایج؛ ۹ آزمون، بدون شکست/خطا/ردشده.

گزارش JUnit، lint، instrumentation، اسکیمای Room و APK دیباگ بهعنوان artifact
همان اجرا موجوداند. این اجراها جایگزین ابزارهای آفلاین نیستند؛ ابزارهای آفلاین
همچنان برای محیط بدون SDK مفیدند، ولی مرجع تأیید ساخت اندروید، CI است. جزئیات
وضعیت در [QA.md](QA.md).

## چکلیست پیش از اعلام آمادگی

مراحل خودکار بالا (کامپایل، JUnit، لینت، APK، آزمون Room/اسکیما) اجرا و عبور
کردهاند. [docs/QA.md](QA.md) همچنان فهرست موارد **انسانی** انجامنشده را نگه
میدارد: نصب/بازبینی بصری صفحات، جریان روزانه، جریان هزینه/تسویه، تولید و
**بازبینی بصری** PDF، پشتیبان/بازیابی، حالت تاریک/چرخش و آزمون مهاجرت پس از
نخستین تغییر اسکیما. تا زمان انجام آنها، کیفیت نهایی و آمادگی انتشار تأیید
نشده است.
