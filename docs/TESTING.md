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

## آزمونهای on-device (نوشتهشده، در انتظار اجرا)

`ShootingDatabaseTest` — seed دادههای مرجع و افزودن کور جدید بدون مهاجرت،
ذخیرهٔ گزارش و محاسبهٔ مجدد طولها/جمع، جلوگیری از تکرار پروژه+تاریخ، جلوگیری از
تسویهٔ بیش از مانده و حذف هزینهٔ دارای تسویه، بازیابی `ADD_ONLY` بدون تکرار/حذف.
`DatabaseSchemaTest` — هشت جدول، ستونهای عددی تاریخ/مبلغ، ایندکس یکتا و اعمال FK
روی SQLite واقعی.

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

**نتیجهٔ واقعی آخرین اجرا (محیط فعلی):** کامپایل `exit 0`،
`62 passed, 0 failed`، `exit 0`؛ بررسیهای ایستا `0 error, 0 warning`؛ کامپایل
استابی ۴۲ فایل `exit 0`. هیچکدام اجرای JUnit/Gradle یا کامپایل اندروید **نیست**
و نباید بهعنوان چنین گزارش شود.

## چکلیست پیش از اعلام آمادگی

[docs/QA.md](QA.md) فهرست دقیق مواردی است که باید اجرا و بررسی شوند (کامپایل،
APK، لینت، مهاجرتها، بازبینی صفحات، جریان روزانه، جریان هزینه/تسویه، تولید و
**بازبینی بصری** PDF، و پشتیبان/بازیابی). تا زمان انجام آنها، کیفیت نهایی تأیید
نشده است.
