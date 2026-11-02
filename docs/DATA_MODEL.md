# مدل داده

## نگاشت نگهداری

| مقدار | نوع ذخیره | توضیح |
| --- | --- | --- |
| تاریخ | `Long` = Gregorian `epochDay` | تبدیل به جلالی فقط هنگام نمایش/گزارش |
| مبلغ | `Long` = تومانِ عدد صحیح | بدون `Float`/`Double`، بدون ریال، بدون تبدیل خاموش |
| طول | `Int` متر | `L = abs(endMeter − startMeter)`، جمع با حساب `Long` و بررسی سرریز |
| هویت پایدار | `String uuid` | همهٔ موجودیت ها بهجز تنظیمات؛ مبنای تطبیق در بازیابی |
| وضعیت | مشتقشده | `SettlementStatus` و مبالغ تسویه شده از روی ردیف ها محاسبه می شوند، کش نمی شوند |

## جداول (Room v1، `shooting.db`)

| جدول | ستون های کلیدی | محدودیت ها |
| --- | --- | --- |
| `projects` | `id`, `uuid`, `name`, `workArea`, `employer`, `supervisor`, `description`, `active`, `createdAt`, `updatedAt` | ایندکس یکتا روی `uuid` و `name` |
| `workdays` | `id`, `uuid`, `projectId`, `dateEpochDay`, `workArea`, `employer`, `supervisor`, `workerCount`, `notes`, `totalLengthMeters`, timestamps | FK→projects `CASCADE`؛ **ایندکس یکتا `(projectId, dateEpochDay)`** |
| `cable_types` | `id`, `coreCount`, `label`, `active`, `sortOrder` | ایندکس یکتا `coreCount` |
| `operations` | `id`, `uuid`, `workdayId`, `startType`, `startCode`, `startMeter`, `endType`, `endCode`, `endMeter`, `cableTypeId`, `lengthMeters`, timestamps | FK→workdays `CASCADE`، FK→cable_types `RESTRICT` |
| `expense_categories` | `id`, `name`, `active`, `sortOrder` | ایندکس یکتا `name` |
| `expenses` | `id`, `uuid`, `dateEpochDay`, `categoryId`, `amountToman`, `description`, `receiptReference`, `projectId?`, `workdayId?`, timestamps | FK→categories `RESTRICT`، →projects/workdays `SET_NULL` |
| `settlements` | `id`, `uuid`, `expenseId`, `amountToman`, `dateEpochDay`, `note`, `createdAt` | FK→expenses `CASCADE` |
| `app_settings` | `key` (PK), `value` | `theme_mode`, `default_project_id`, `last_backup_epoch_milli` |

مقادیر اولیه: کور های استاندارد `۲/۴/۶/۱۲/۲۴/۳۶/۴۸/۷۲/۹۶/۱۴۴/۲۸۸` و هفت دستهٔ
`سوخت خودرو`, `سوخت دستگاه`, `مخارج خودرو`, `مخارج دستگاه`, `اقلام مصرفی`,
`خورد و خوراک`, `هزینه های بدون فاکتور` (از `ReferenceData` + `SeedCallback`).
فهرست کور ها بر اساس مقادیر رایج تجاری و استاندارد TIA انتخاب شده است
([استاندارد TIA](https://www.csemag.com/articles/fiber-optics-a-backbone-for-advanced-building-design/),
[راهنمای تعداد کور](https://www.cobtel.com/info/how-many-core-in-fiber-optic-cable-the-comple-103687610.html)).

انواع نقطه: `ODC`, `FAT`, `HANDHOLE(حوضچه)`, `MANHOLE(منهول)` — نام enum در DB
ذخیره می شود و برچسب فارسی فقط نمایشی است.

## قواعد محاسباتی

```
L(operation)          = abs(endMeter - startMeter)      // اختلاف منفی مجاز
DailyTotal(workday)   = Σ L(operation)
PeriodTotal           = Σ DailyTotal(workday در بازه/فیلتر پروژه)
TotalSettlements      = Σ settlement.amountToman
Outstanding(expense)  = expense.amountToman - TotalSettlements(expense)   // >= 0
PeriodOutstanding     = Σ expense.amountToman - Σ settlements
```

* `CableLengthRow` طول و تعداد عملیات ها را به تفکیک نوع کابل نگه می دارد؛
 `PeriodReport.cableBreakdown` و جدول PDF از آن ساخته می شوند.
* اعتبارسنجی هزینه/تسویه: مبلغ > ۰، دسته معتبر، تسویه ≤ مانده.
* حذف هزینه ای که تسویه دارد با `EXPENSE_HAS_SETTLEMENTS` رد می شود.
* هزینهٔ حذفشدهٔ مرتبط با گزارش روزانه: FK `SET_NULL` تاریخچه را نگه میدارد.
* `Workday.operations` از `WorkdayDetailRow` (`@Relation`) خوانده می شود؛
 `WorkdayRow` برای لیست ها فقط نام پروژه را join می کند.
* `ExpenseRow` مبالغ تسویه شده و تعداد تسویه ها را با subquery محاسبه می کند
 (ستون کششدهٔ مستعد ناهماهنگی وجود ندارد).

> پشتیبان گیری خودکار ابری سیستمعامل غیر فعال است (`allowBackup=false`)؛ صادرات
> داده فقط از طریق فایل نسخهدار درون برنامه ای و با اقدام کاربر انجام می شود.

## مهاجرت ها

نسخهٔ فعلی **۱** است؛ `Migrations.ALL` خالی و `fallbackToDestructiveMigration()`
هرگز فراخوانی نشده است. اسکیمای JSON با `exportSchema=true` و
`room.schemaLocation=$projectDir/schemas` در هر اجرای KSP تولید می شود (شامل
`identityHash` محاسبه شدهٔ Room، بنابراین دستینویس نمی شود)؛ CI آن را بهعنوان
artifact بارگذاری می کند. فایل نسخهٔ ۱ هنوز در مخزن کامیت نشده است و پیش از
نخستین تغییر اسکیما باید همان خروجی تولیدشده اضافه شود
([`app/schemas/README.md`](../app/schemas/README.md)).

سیاست برای نسخه های بعدی:

1. فقط تغییر **افزودنی** (`ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...`
 یا جدول جدید).
2. افزودن `Migration(from, to)` صریح به `Migrations.ALL`؛ حذف/بازسازی جدول ممنوع.
3. کامیت فایل JSON نسخهٔ قدیم و جدید + آزمون `MigrationTestHelper` که از JSON
 نسخهٔ قدیم شروع می کند و صحت داده ها را بعد از مهاجرت بررسی می کند.
4. `DatabaseSchemaTest` شکل نسخهٔ ۱ (جداول/ستون ها/ایندکس یکتا/FK) را روی SQLite
 واقعی بررسی می کند؛ `WorkflowIntegrationTest` نیز نگاشت پشتیبان/بازیابی، نسخه،
 پیش نمایش، نسخهٔ ایمنی و رد checksum دستکاریشده را در همان اجرای on-device
 تأیید مینماید.
