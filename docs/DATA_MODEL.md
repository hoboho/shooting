# مدل داده

## نگاشت نگهداری

| مقدار | نوع ذخیره | توضیح |
| --- | --- | --- |
| تاریخ | `Long` = Gregorian `epochDay` | تبدیل به جلالی فقط هنگام نمایش/گزارش |
| مبلغ | `Long` = تومانِ عدد صحیح | بدون `Float`/`Double`، بدون ریال، بدون تبدیل خاموش |
| طول | `Int` متر | `L = abs(endMeter − startMeter)`، جمع با حساب `Long` و بررسی سرریز |
| هویت پایدار | `String uuid` | همهٔ موجودیتها بهجز تنظیمات؛ مبنای تطبیق در بازیابی |
| وضعیت | مشتقشده | `SettlementStatus` و مبالغ تسویهشده از روی ردیفها محاسبه میشوند، کش نمیشوند |

## جداول (Room v1، `shooting.db`)

| جدول | ستونهای کلیدی | محدودیتها |
| --- | --- | --- |
| `projects` | `id`, `uuid`, `name`, `workArea`, `employer`, `supervisor`, `description`, `active`, `createdAt`, `updatedAt` | ایندکس یکتا روی `uuid` و `name` |
| `workdays` | `id`, `uuid`, `projectId`, `dateEpochDay`, `workArea`, `employer`, `supervisor`, `workerCount`, `notes`, `totalLengthMeters`, timestamps | FK→projects `CASCADE`؛ **ایندکس یکتا `(projectId, dateEpochDay)`** |
| `cable_types` | `id`, `coreCount`, `label`, `active`, `sortOrder` | ایندکس یکتا `coreCount` |
| `operations` | `id`, `uuid`, `workdayId`, `startType`, `startCode`, `startMeter`, `endType`, `endCode`, `endMeter`, `cableTypeId`, `lengthMeters`, timestamps | FK→workdays `CASCADE`، FK→cable_types `RESTRICT` |
| `expense_categories` | `id`, `name`, `active`, `sortOrder` | ایندکس یکتا `name` |
| `expenses` | `id`, `uuid`, `dateEpochDay`, `categoryId`, `amountToman`, `description`, `receiptReference`, `projectId?`, `workdayId?`, timestamps | FK→categories `RESTRICT`، →projects/workdays `SET_NULL` |
| `settlements` | `id`, `uuid`, `expenseId`, `amountToman`, `dateEpochDay`, `note`, `createdAt` | FK→expenses `CASCADE` |
| `app_settings` | `key` (PK), `value` | `theme_mode`, `default_project_id`, `last_backup_epoch_milli` |

مقادیر اولیه: کورهای `۶/۱۲/۲۴/۴۸/۹۶/۱۴۴ Core` و هفت دستهٔ
`سوخت خودرو`, `سوخت دستگاه`, `مخارج خودرو`, `مخارج دستگاه`, `اقلام مصرفی`,
`خورد و خوراک`, `هزینههای بدون فاکتور` (از `ReferenceData` + `SeedCallback`).

انواع نقطه: `ODC`, `FAT`, `HANDHOLE(حوضچه)`, `MANHOLE(منهول)` — نام enum در DB
ذخیره میشود و برچسب فارسی فقط نمایشی است.

## قواعد محاسباتی

```
L(operation)          = abs(endMeter - startMeter)      // اختلاف منفی مجاز
DailyTotal(workday)   = Σ L(operation)
PeriodTotal           = Σ DailyTotal(workday در بازه/فیلتر پروژه)
TotalSettlements      = Σ settlement.amountToman
Outstanding(expense)  = expense.amountToman - TotalSettlements(expense)   // >= 0
PeriodOutstanding     = Σ expense.amountToman - Σ settlements
```

* اعتبارسنجی هزینه/تسویه: مبلغ > ۰، دسته معتبر، تسویه ≤ مانده.
* حذف هزینهای که تسویه دارد با `EXPENSE_HAS_SETTLEMENTS` رد میشود.
* هزینهٔ حذفشدهٔ مرتبط با گزارش روزانه: FK `SET_NULL` تاریخچه را نگه میدارد.
* `Workday.operations` از `WorkdayDetailRow` (`@Relation`) خوانده میشود؛
  `WorkdayRow` برای لیستها فقط نام پروژه را join میکند.
* `ExpenseRow` مبالغ تسویهشده و تعداد تسویهها را با subquery محاسبه میکند
  (ستون کششدهٔ مستعد ناهماهنگی وجود ندارد).

> پشتیبانگیری خودکار ابری سیستمعامل غیرفعال است (`allowBackup=false`)؛ صادرات
> داده فقط از طریق فایل نسخهدار درون برنامهای و با اقدام کاربر انجام میشود.

## مهاجرتها

نسخهٔ فعلی **۱** است؛ `Migrations.ALL` خالی و `fallbackToDestructiveMigration()`
هرگز فراخوانی نشده است. اسکیمای JSON با `exportSchema=true` و
`room.schemaLocation=$projectDir/schemas` در هر اجرای KSP تولید میشود (شامل
`identityHash` محاسبهشدهٔ Room، بنابراین دستینویس نمیشود)؛ CI آن را بهعنوان
artifact بارگذاری میکند. فایل نسخهٔ ۱ هنوز در مخزن کامیت نشده است و پیش از
نخستین تغییر اسکیما باید همان خروجی تولیدشده اضافه شود
([`app/schemas/README.md`](../app/schemas/README.md)).

سیاست برای نسخههای بعدی:

1. فقط تغییر **افزودنی** (`ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...`
   یا جدول جدید).
2. افزودن `Migration(from, to)` صریح به `Migrations.ALL`؛ حذف/بازسازی جدول ممنوع.
3. کامیت فایل JSON نسخهٔ قدیم و جدید + آزمون `MigrationTestHelper` که از JSON
   نسخهٔ قدیم شروع میکند و صحت دادهها را بعد از مهاجرت بررسی میکند.
4. `DatabaseSchemaTest` شکل نسخهٔ ۱ (جداول/ستونها/ایندکس یکتا/FK) را روی SQLite
   واقعی بررسی میکند.
