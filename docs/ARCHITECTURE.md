# معماری

## اصول

| اصل | پیادهسازی |
| --- | --- |
| جداسازی لایه ها | `core` (ابزار های بدون وابستگی پلتفرم)، `domain` (منطق خالص Kotlin)، `data` (Room + نگاشت + مخازن)، `ui`/`feature` (Compose) |
| منبع حقیقت | Room روی دستگاه؛ هیچ کش ابری/همگامسازی وجود ندارد |
| یکطرفگی وابستگی | `feature` → `domain.repository` (interface)؛ `data` پیادهسازی می کند؛ `di` آن ها را با Hilt میبندد |
| آفلاین مطلق | مانیفست فاقد `INTERNET`؛ `FileProvider` فقط برای اشتراک گذاری فایل محلی |
| قابل آزمون بودن | محاسبات، اعتبارسنجی، تجمیع، نگاشت پشتیبان و ساخت مشخصات PDF هیچ وابست های به Android ندارند |
| عدم از دست رفتن داده | ذخیرهٔ نامعتبر ممنوع (`ValidationException`)، مهاجرت مخرب ممنوع، حذف هزینهٔ دارای تسویه ممنوع، بازیابی همراه نسخهٔ ایمنی |

## نمودار جریان داده

```
Compose Screen ──(intent)──> ViewModel ──> Repository interface
                                   ▲              │
                              StateFlow      Room DAO (Flow / suspend)
                                   │              │
                                   └──────── SQLite (shooting.db، نسخه ۱)
```

* مشاهده: DAOها `Flow` میدهند، مخازن به مدل دامنه نگاشت می کنند، ViewModel آن ها را
 در `StateFlow` ادغام می کند (`combine`) و اسکرین با
 `collectAsStateWithLifecycle` رندر می شود.
* نوشتن: ViewModel متد `suspend` مخزن را در `viewModelScope` صدا میزند. مخزن داخل
 `RoomDatabase.withTransaction` اعتبارسنجی، محاسبهٔ مجدد طول ها و نوشتن را انجام
 می دهد؛ خطا به پیام فارسی تبدیل می شود.
* ورودی عددی: `ShootingNumberField` ارقام فارسی/عربی و جداکننده ها را نرمال می کند؛
 کد نقطه از `ShootingTextField` با `KeyboardType.Number` استفاده می کند.
* خروجی فایل: `PublicDownloadSaver` نسخهٔ قابل مشاهدهٔ PDF/پشتیبان را در
 `Downloads/Shooting` می نویسد؛ `rememberFileSaveAction` فقط روی Android 8/9
 مجوز ذخیره سازی قدیمی را می گیرد.
 `NumberFormatter` ورودی منفی، اعشاری یا غیرعددی را **رد** می کند (حذف خاموش
 کاراکتر ها انجام نمی شود).

## لایه ها

### `core`
* `date/` — `JalaliCalendar` با جدول ۱۰۰۲ مقداری نوروز سال های ۱۰۰۰…۲۰۰۱ (تولیدشده
 از `jdatetime`)، تبدیل دوسویه دقیق، طول ماه/سال کبیسه، شنبه=۰، `DateRange` بازهٔ
 شمول دو سر. ذخیره فقط `epochDay` گرگوری؛ رشتهٔ فارسی هرگز در DB نیست.
* `format/` — `PersianDigits`, `NumberFormatter` (گروهبندی خالص Kotlin، بدون
 `DecimalFormat`), `DateFormatter`.
* `pdf/` — `PdfModel.kt` مدل مستقل از اندروید (بلوکها، ستونها، سبک ها) و
 `PdfReportEngine.kt` تن ها جایی که `android.graphics.pdf.PdfDocument`،
 `StaticLayout` و `BidiFormatter`/`FIRSTSTRONG_RTL` را میشناسد.
* `backup/` — `SimpleJson` (نویسنده/خوانندهٔ قطعی)، DTOها، `BackupCodec`
 (MAGIC/نسخه/SHA-256)، `BackupMapper` (uuid پایدار)، `AppInfo`,
 `BackupManager` (پیش نمایش، نسخهٔ ایمنی، اعمال راهبرد).
* `common/` — `Clock` تزریقپذیر، خطاها، `StringProvider` (نگاشت پیام اعتبارسنجی
 به منابع فارسی).
* `share/` — `ReportFileWriter` (نوشتن در `filesDir/exports`) و `FileSharer`
 (اشتراک از طریق `FileProvider`).

### `domain`
مدل ها (`Models.kt`)، `ShootingCalculator`, `ExpenseCalculator`, `Validator`,
`PeriodAggregator`, `ReportSpecBuilder`, `ReportText`, `RestoreModels`، و رابط های
مخازن. هیچ `import android.*` در این بسته وجود ندارد.

### `data`
هشت موجودیت Room، هشت DAO، `ShootingDatabase` (v1، `exportSchema=true`)،
نگاشت ها و شش پیادهسازی مخزن. طول عملیات و جمع روزانه **همیشه** دوباره محاسبه
می شود؛ `Workday.totalLengthMeters` فقط کشِ مشتقشده است و با
`WorkdayDao.recomputeTotal` همگام نگه داشته می شود.

### `di`
`AppModule` پایگاه داده، `Clock` و DAOها را فراهم می کند؛ `SeedCallback` در اولین
ایجاد، کور ها و دسته های پیش فرض را وارد می کند؛ `RepositoryModule` رابط ها را به
پیادهسازی ها میبندد؛ `Migrations.ALL` خالی است (v1) و سیاست مهاجرت افزودنی در
کامنت و [DATA_MODEL](DATA_MODEL.md) ثبت شده است.

### `ui` / `feature`
`ShootingTheme` دقیقاً توکن های Metra را بازتولید می کند: سبز `#176B5B`،
`primaryContainer #D8F2E9`، ثانویه `#2E7D8A`، پسزمینه `#F7F9F7`، سطح
`#FFFFFEFC`، فونت Vazirmatn، مقیاس تایپوگرافی و شکل های `10/14/20/26/32dp`،
همراه پالت تاریک. `CompositionLocalProvider` جهت را به `LayoutDirection.Rtl`
پین می کند. کامپوننت های مشترک (`ShootingButton` با پنج سطح، فیلدها،
`ChoiceChips`, `SectionCard`, `StatRow`, `TagChip`, `ShootingDropdown`,
`JalaliDatePickerDialog`, `DateRangeFields`) در **همه** صفحات استفاده می شوند؛
هیچ صفح های استایل جداگان های تعریف نکرده است.

ناوبری `NavHost` تایپسیف با مسیر های رشت های و شش مقصد نوار پایین
(داشبورد، پروژهها، گزارش های روزانه، هزینهها، گزارشها، تنظیمات) بهعلاوهٔ
صفحات ویرایش/جزئیات/پشتیبان/داده های مرجع.

## تصمیم های مهم

* **گزارش تکراری ممنوع است، جریان چندگزارش های عمداً طراحی نشده**: ایندکس
 یکتای `(projectId, dateEpochDay)` در DB + بررسی `Validator.isDuplicateWorkday`
 + بررسی در `WorkdayRepositoryImpl.save`.
* **جهت قرقره مهم نیست**: `abs` در `ShootingCalculator` و `Validator` اختلاف
 منفی raw را خطا نمیدانند.
* **طول و پول جدا هستند**: `PeriodReport` دو گروه فیلد مجزا دارد؛
 `ReportSpecBuilder` جدول طول و بلوک مبالغ را جدا میسازد؛
 `StatRow`ها هیچ جمعی از دو کمیت نمیسازند.
* **هزینه = مطالبه از شرکت**؛ ماژول حقوق/مالیات/بودجه وجود ندارد.
* **داده های مرجع ردیفاند نه enum**: افزودن کور/دسته بدون مهاجرت.
