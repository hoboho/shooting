# معماری

## اصول

| اصل | پیادهسازی |
| --- | --- |
| جداسازی لایهها | `core` (ابزارهای بدون وابستگی پلتفرم)، `domain` (منطق خالص Kotlin)، `data` (Room + نگاشت + مخازن)، `ui`/`feature` (Compose) |
| منبع حقیقت | Room روی دستگاه؛ هیچ کش ابری/همگامسازی وجود ندارد |
| یکطرفگی وابستگی | `feature` → `domain.repository` (interface)؛ `data` پیادهسازی میکند؛ `di` آنها را با Hilt میبندد |
| آفلاین مطلق | مانیفست فاقد `INTERNET`؛ `FileProvider` فقط برای اشتراکگذاری فایل محلی |
| قابل آزمون بودن | محاسبات، اعتبارسنجی، تجمیع، نگاشت پشتیبان و ساخت مشخصات PDF هیچ وابستهای به Android ندارند |
| عدم از دست رفتن داده | ذخیرهٔ نامعتبر ممنوع (`ValidationException`)، مهاجرت مخرب ممنوع، حذف هزینهٔ دارای تسویه ممنوع، بازیابی همراه نسخهٔ ایمنی |

## نمودار جریان داده

```
Compose Screen ──(intent)──> ViewModel ──> Repository interface
                                   ▲              │
                              StateFlow      Room DAO (Flow / suspend)
                                   │              │
                                   └──────── SQLite (shooting.db، نسخه ۱)
```

* مشاهده: DAOها `Flow` میدهند، مخازن به مدل دامنه نگاشت میکنند، ViewModel آنها را
  در `StateFlow` ادغام میکند (`combine`) و اسکرین با
  `collectAsStateWithLifecycle` رندر میشود.
* نوشتن: ViewModel متد `suspend` مخزن را در `viewModelScope` صدا میزند. مخزن داخل
  `RoomDatabase.withTransaction` اعتبارسنجی، محاسبهٔ مجدد طولها و نوشتن را انجام
  میدهد؛ خطا به پیام فارسی تبدیل میشود.
* ورودی عددی: `ShootingNumberField` ارقام فارسی/عربی و جداکنندهها را نرمال میکند؛
  `NumberFormatter` ورودی منفی، اعشاری یا غیرعددی را **رد** میکند (حذف خاموش
  کاراکترها انجام نمیشود).

## لایهها

### `core`
* `date/` — `JalaliCalendar` با جدول ۱۰۰۲ مقداری نوروز سالهای ۱۰۰۰…۲۰۰۱ (تولیدشده
  از `jdatetime`)، تبدیل دوسویه دقیق، طول ماه/سال کبیسه، شنبه=۰، `DateRange` بازهٔ
  شمول دو سر. ذخیره فقط `epochDay` گرگوری؛ رشتهٔ فارسی هرگز در DB نیست.
* `format/` — `PersianDigits`, `NumberFormatter` (گروهبندی خالص Kotlin، بدون
  `DecimalFormat`), `DateFormatter`.
* `pdf/` — `PdfModel.kt` مدل مستقل از اندروید (بلوکها، ستونها، سبکها) و
  `PdfReportEngine.kt` تنها جایی که `android.graphics.pdf.PdfDocument`،
  `StaticLayout` و `BidiFormatter`/`FIRSTSTRONG_RTL` را میشناسد.
* `backup/` — `SimpleJson` (نویسنده/خوانندهٔ قطعی)، DTOها، `BackupCodec`
  (MAGIC/نسخه/SHA-256)، `BackupMapper` (uuid پایدار)، `AppInfo`,
  `BackupManager` (پیشنمایش، نسخهٔ ایمنی، اعمال راهبرد).
* `common/` — `Clock` تزریقپذیر، خطاها، `StringProvider` (نگاشت پیام اعتبارسنجی
  به منابع فارسی).
* `share/` — `ReportFileWriter` (نوشتن در `filesDir/exports`) و `FileSharer`
  (اشتراک از طریق `FileProvider`).

### `domain`
مدلها (`Models.kt`)، `ShootingCalculator`, `ExpenseCalculator`, `Validator`,
`PeriodAggregator`, `ReportSpecBuilder`, `ReportText`, `RestoreModels`، و رابطهای
مخازن. هیچ `import android.*` در این بسته وجود ندارد.

### `data`
هشت موجودیت Room، هشت DAO، `ShootingDatabase` (v1، `exportSchema=true`)،
نگاشتها و شش پیادهسازی مخزن. طول عملیات و جمع روزانه **همیشه** دوباره محاسبه
میشود؛ `Workday.totalLengthMeters` فقط کشِ مشتقشده است و با
`WorkdayDao.recomputeTotal` همگام نگه داشته میشود.

### `di`
`AppModule` پایگاه داده، `Clock` و DAOها را فراهم میکند؛ `SeedCallback` در اولین
ایجاد، کورها و دستههای پیشفرض را وارد میکند؛ `RepositoryModule` رابطها را به
پیادهسازیها میبندد؛ `Migrations.ALL` خالی است (v1) و سیاست مهاجرت افزودنی در
کامنت و [DATA_MODEL](DATA_MODEL.md) ثبت شده است.

### `ui` / `feature`
`ShootingTheme` دقیقاً توکنهای Metra را بازتولید میکند: سبز `#176B5B`،
`primaryContainer #D8F2E9`، ثانویه `#2E7D8A`، پسزمینه `#F7F9F7`، سطح
`#FFFFFEFC`، فونت Vazirmatn، مقیاس تایپوگرافی و شکلهای `10/14/20/26/32dp`،
همراه پالت تاریک. `CompositionLocalProvider` جهت را به `LayoutDirection.Rtl`
پین میکند. کامپوننتهای مشترک (`ShootingButton` با پنج سطح، فیلدها،
`ChoiceChips`, `SectionCard`, `StatRow`, `TagChip`, `ShootingDropdown`,
`JalaliDatePickerDialog`, `DateRangeFields`) در **همه** صفحات استفاده میشوند؛
هیچ صفحهای استایل جداگانهای تعریف نکرده است.

ناوبری `NavHost` تایپسیف با مسیرهای رشتهای و شش مقصد نوار پایین
(داشبورد، پروژهها، گزارشهای روزانه، هزینهها، گزارشها، تنظیمات) بهعلاوهٔ
صفحات ویرایش/جزئیات/پشتیبان/دادههای مرجع.

## تصمیمهای مهم

* **گزارش تکراری ممنوع است، جریان چندگزارشهای عمداً طراحی نشده**: ایندکس
  یکتای `(projectId, dateEpochDay)` در DB + بررسی `Validator.isDuplicateWorkday`
  + بررسی در `WorkdayRepositoryImpl.save`.
* **جهت قرقره مهم نیست**: `abs` در `ShootingCalculator` و `Validator` اختلاف
  منفی raw را خطا نمیدانند.
* **طول و پول جدا هستند**: `PeriodReport` دو گروه فیلد مجزا دارد؛
  `ReportSpecBuilder` جدول طول و بلوک مبالغ را جدا میسازد؛
  `StatRow`ها هیچ جمعی از دو کمیت نمیسازند.
* **هزینه = مطالبه از شرکت**؛ ماژول حقوق/مالیات/بودجه وجود ندارد.
* **دادههای مرجع ردیفاند نه enum**: افزودن کور/دسته بدون مهاجرت.
