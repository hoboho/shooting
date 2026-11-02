# آزمون ها

> **توجه:** وضعیت زیر مربوط به کامیت پیش از تغییرات اخیر (کیبورد عددی، ذخیرهٔ
> فایل ها، کور های استاندارد، تفکیک نوع کابل و اصلاح نگارشی) است؛ اجرا های تازهٔ
> این شاخه در برگهٔ Actions مخزن قابل بررسی اند.

## لایه ها

| لایه | مسیر | اجرا |
| --- | --- | --- |
| منطق خالص JVM | `app/src/test` (۱۳ فایل، ۶۲ آزمون) | `./gradlew :app:testDebugUnitTest` |
| منابع/ناوبری/قواعد ایستا | `scripts/check_sources.py` | `python3 scripts/check_sources.py` |
| کامپایل منطق/داده با استاب | `scripts/offline-compile-check/` | `bash scripts/offline-compile-check/run.sh` |
| Android lint | قواعد `app/lint.xml` | `./gradlew :app:lintDebug` |
| Room/مخازن/اسکیما و جریان های کاری روی SQLite | `app/src/androidTest` (۳ فایل، ۱۲ آزمون) | `./gradlew :app:connectedDebugAndroidTest` |
| ساخت | — | `./gradlew :app:assembleDebug` |

## پوشش آزمون های JVM

| فایل | موضوع |
| --- | --- |
| `JalaliCalendarTest`, `DateRangeExtraTest` | جدول نوروز/سال های کبیسه، لنگر های ۱۴۰۳/۱۴۰۴/۱۴۰۵، تبدیل دوسویه، مرز ماه/سال/هفته، شمول بازه، `fromEpochMilli` |
| `FormatterTest`, `NumberFormatterExtraTest` | ارقام فارسی/عربی، جداکنندهها، گروهبندی، واحد متر/تومان، رد صریح منفی/اعشار/ورودی غیرعددی، عدم پنهانسازی ورودی نامعتبر |
| `ShootingCalculatorTest` | `abs(end−start)`، اختلاف منفی مجاز، به روز رسانی طول با تغییر هر کنتور، عدم سرریز، رد پیشنویس ناقص، `DailyTotal = ΣL` |
| `ValidatorTest` | نام پروژه/تکراری، جلوگیری از گزارش تکراری همان پروژه+تاریخ، الزام پروژه/تاریخ/عملیات، اعتبار سطر به سطر، پیش فرض ۰ کارگر |
| `ExpenseCalculatorTest` | تسویه نشده/جزئی/کامل، مانده، جلوگیری از تسویهٔ بیش از مانده، تجمع صحیح تومان |
| `PeriodAggregatorTest` | فیلتر بازه و پروژه، تفکیک روزانه/پروژه/**نوع کابل**، **عدم ترکیب طول و پول** |
| `ReportSpecBuilderTest` | جمع طول روزانه + جدول جداگانه تفکیک نوع کابل، تفکیک بلوک طول و مبالغ در گزارش دوره ای، فهرست تسویه ها و تومان صحیح |
| `SimpleJsonTest`, `BackupCodecTest`, `BackupMapperTest`, `RestoreModelsTest` | JSON قطعی، MAGIC/نسخه/SHA-256/دستکاری checksum، نگاشت رفتوبرگشت همهٔ موجودیتها، پرچم های صریح راهبرد |

## آزمون های on-device (اجراشده در CI)

`ShootingDatabaseTest` — seed داده های مرجع و افزودن کور جدید بدون مهاجرت،
ذخیرهٔ گزارش و محاسبهٔ مجدد طول ها/جمع، جلوگیری از تکرار پروژه+تاریخ، جلوگیری از
تسویهٔ بیش از مانده و حذف هزینهٔ دارای تسویه، بازیابی `ADD_ONLY` بدون تکرار/حذف.
`DatabaseSchemaTest` — هشت جدول، ستون های عددی تاریخ/مبلغ، ایندکس یکتا و اعمال FK
روی SQLite واقعی. `WorkflowIntegrationTest` — گردش روزانه با قرقرهٔ معکوس،
جلوگیری از گزارش تکراری و PDF روزانه؛ تسویهٔ جزئی/کامل، جلوگیری از بیشبود،
گزارش دوره ای و PDF هزینهها؛ و پیش نمایش پشتیبان، نسخهٔ ایمنی، راهبرد تراکنشی و
رد دستکاری checksum. هر PDF با `PdfRenderer` بررسی و از طریق
`MediaStore.Downloads` به artifact `qa-pdf-samples` منتقل می شود؛ این بررسی
قابلبازشدن و وجود پیکسل متن است، نه بازبینی بصری انسانی.

در اجرا های مرجع [PR 41480196402](https://github.com/hoboho/shooting/actions/runs/41480196402)
و [push 41480188364](https://github.com/hoboho/shooting/actions/runs/41480188364)،
job سوم `connectedDebugAndroidTest` را روی امولاتور API 34 اجرا کرد؛ خروجی XML
چاپشده `tests="12" failures="0" errors="0" skipped="0"` است. اجرای قدیمی
[41394294285](https://github.com/hoboho/shooting/actions/runs/41394294285)
نخستین ساخت کامل با ۹ آزمون on-device بود.

## اجرای آفلاین (بدون Gradle/Android SDK)

```bash
KOTLINC=/path/to/kotlinc bash scripts/run-offline-tests.sh
python3 scripts/check_sources.py
KOTLINC=/path/to/kotlinc COROUTINES_JAR=/path/to/kotlinx-coroutines-core-jvm.jar \
  bash scripts/offline-compile-check/run.sh
```

اسکریپت اول فایل های خالص `core/` + `domain/` بههمراه ۱۳ فایل آزمون و دو فایل
`scripts/offline-test-harness/` را با `kotlinc` کامپایل می کند و با یک
اجراکنندهٔ بازتابی کوچک اجرا مینماید. آزمون ها خودشان از API استاندارد
`org.junit.Test` / `org.junit.Assert` استفاده می کنند، بنابراین در Gradle بدون
هیچ تغییری با JUnit واقعی اجرا می شوند.

اسکریپت سوم ۴۲ فایل `core/`, `domain/`, `data/`, `di/` را با `kotlinc` واقعی،
`kotlinx-coroutines-core` واقعی و استاب های کوچک Android/Room/Hilt کامپایل
می کند؛ خطا های نوع/`suspend`/ارجاع در لایهٔ داده را میگیرد ولی **کامپایل
اندروید، KSP، Hilt codegen یا Compose نیست** (جزئیات در
`scripts/offline-compile-check/README.md`).

**نتیجهٔ واقعی ابزار های آفلاین (محیط محلی):** کامپایل `exit 0`،
`62 passed, 0 failed`، `exit 0`؛ بررسی های ایستا `0 error, 0 warning`؛ کامپایل
استابی ۴۲ فایل `exit 0`. هیچکدام اجرای JUnit/Gradle یا کامپایل اندروید **نیست**
و نباید بهعنوان چنین گزارش شود.

## اجرای واقعی روی GitHub Actions

گردشکار [`.github/workflows/android.yml`](../.github/workflows/android.yml) روی
runner دارای Android SDK اجرا شد؛
[PR run 41480196402](https://github.com/hoboho/shooting/actions/runs/41480196402)
و [push run 41480188364](https://github.com/hoboho/shooting/actions/runs/41480188364)
نتیجهٔ `success` برای هر سه job دارند:

1. **Offline source checks**: `check_sources.py`.
2. **JVM tests, lint & debug APK**: `testDebugUnitTest` با JUnit واقعی،
 `lintDebug`، `assembleDebug` و `kspDebugKotlin` برای تولید اسکیمای Room.
3. **Room/repository tests (API 34)**: `connectedDebugAndroidTest` و چاپ XML
 نتایج؛ ۱۲ آزمون، بدون شکست/خطا/ردشده.

گزارش JUnit، lint، instrumentation، اسکیمای Room، APK دیباگ و سه نمونهٔ PDF
(`qa-pdf-samples`) بهعنوان artifact همان اجرا ها موجوداند. این اجرا ها جایگزین ابزار های آفلاین نیستند؛ ابزار های آفلاین
همچنان برای محیط بدون SDK مفیدند، ولی مرجع تأیید ساخت اندروید، CI است. جزئیات
وضعیت در [QA.md](QA.md).

## چکلیست پیش از اعلام آمادگی

مراحل خودکار بالا (کامپایل، JUnit، لینت، APK، آزمون Room/اسکیما و سه آزمون
جریان کاری شامل رندر PDF) اجرا و عبور کردهاند. [docs/QA.md](QA.md) همچنان فهرست موارد **انسانی** انجامنشده را نگه
میدارد: نصب/بازبینی بصری صفحات، جریان روزانه، جریان هزینه/تسویه، تولید و
**بازبینی بصری** PDF، پشتیبان/بازیابی، حالت تاریک/چرخش و آزمون مهاجرت پس از
نخستین تغییر اسکیما. تا زمان انجام آنها، کیفیت نهایی و آمادگی انتشار تأیید
نشده است.
