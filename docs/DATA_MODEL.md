# مدل داده

## نگاشت نگهداری

| مقدار | نوع ذخیره | توضیح |
| --- | --- | --- |
| تاریخ | `Long` = Gregorian `epochDay` | تبدیل به جلالی فقط هنگام نمایش/گزارش |
| مبلغ | `Long` = تومانِ عدد صحیح | بدون `Float`/`Double`، بدون ریال، بدون تبدیل خاموش |
| طول | `Int` متر | `L = abs(endMeter − startMeter)`، جمع با حساب `Long` و بررسی سرریز |
| هویت پایدار | `String uuid` | پروژه، گزارش، عملیات، هزینه، تسویه و دریافتی؛ داده‌های مرجع با کلید طبیعی تطبیق می‌یابند |
| وضعیت | مشتقشده | `SettlementStatus` و مبالغ تسویه شده از روی ردیف ها محاسبه می شوند، کش نمی شوند |

## جداول (Room v6، `shooting.db`)

| جدول | ستون های کلیدی | محدودیت ها |
| --- | --- | --- |
| `projects` | `id`, `uuid`, `name`, `workArea`, `employer`, `supervisor`, `description`, `active`, `createdAt`, `updatedAt` | ایندکس یکتا روی `uuid` و `name` |
| `workdays` | `id`, `uuid`, `projectId`, `dateEpochDay`, `workArea`, `employer`, `supervisor`, `workerCount`, `notes`, `totalLengthMeters`, timestamps | FK→projects `CASCADE`؛ چند گزارش برای یک پروژه و تاریخ مجاز است |
| `cable_types` | `id`, `coreCount`, `label`, `active`, `sortOrder` | ایندکس یکتا `coreCount` |
| `operations` | `id`, `uuid`, `workdayId`, `startType`, `startCode`, `startMeter`, `endType`, `endCode`, `endMeter`, `cableTypeId`, `lengthMeters`, timestamps | FK→workdays `CASCADE`، FK→cable_types `RESTRICT` |
| `expense_categories` | `id`, `name`, `active`, `sortOrder` | ایندکس یکتا `name` |
| `expenses` | `id`, `uuid`, `dateEpochDay`, `categoryId`, `amountToman`, `description`, `receiptReference`, `projectId?`, `workdayId?`, `companyApproved`, timestamps | FK→categories `RESTRICT`، →projects/workdays `SET_NULL` |
| `settlements` | `id`, `uuid`, `expenseId`, `amountToman`, `dateEpochDay`, `note`, `createdAt` | FK→expenses `CASCADE` |
| `company_payments` | `id`, `uuid`, `amountToman`, `dateEpochDay`, `receiptReference`, `note`, `createdAt` | ایندکس یکتا روی `uuid` |
| `cash_ledger` | `id`, `uuid`, `idempotencyKey`, `originType`, `originId`, `amountToman`, `dateEpochDay`, `description`, `createdAt`, `reversalOfEntryId?` | ایندکس یکتا روی `idempotencyKey`؛ گردش مثبت یا منفی نقدی |
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
Outstanding(expense)  = amountToman - TotalSettlements(expense) برای هزینهٔ تأییدشده؛ وگرنه ۰
PeriodOutstanding     = Σ Outstanding(expense) در بازه و فیلتر
```

* `CableLengthRow` طول و تعداد عملیات ها را به تفکیک نوع کابل نگه می دارد؛
 `PeriodReport.cableBreakdown` و جدول PDF از آن ساخته می شوند.
* اعتبارسنجی هزینه/تسویه: مبلغ > ۰، دسته معتبر، تسویه ≤ مانده.
* حذف هزینه ای که تسویه دارد با `EXPENSE_HAS_SETTLEMENTS` رد می شود.
* با حذف گزارش یا پروژه، FK مربوط در هزینه `SET_NULL` می‌شود و هزینه باقی می‌ماند.
* `Workday.operations` از `WorkdayDetailRow` (`@Relation`) خوانده می شود؛
 `WorkdayRow` برای لیست ها فقط نام پروژه را join می کند.
* `ExpenseRow` مبالغ تسویه شده و تعداد تسویه ها را با subquery محاسبه می کند
 (ستون کششدهٔ مستعد ناهماهنگی وجود ندارد).

> پشتیبان گیری خودکار ابری سیستمعامل غیر فعال است (`allowBackup=false`)؛ صادرات
> داده فقط از طریق فایل نسخهدار درون برنامه ای و با اقدام کاربر انجام می شود.

## مهاجرت‌ها

نسخهٔ فعلی پایگاه داده **۶** است. `Migrations.ALL` مسیرهای صریح ۱→۲، ۲→۳،
۳→۴، ۴→۵ و ۵→۶ را ثبت می‌کند و fallback مخرب استفاده نمی‌شود. مهاجرت ۴→۵
ایندکس یکتای پروژه/تاریخ را حذف می‌کند و ۵→۶ جدول دفتر نقدی را ایجاد و از
دریافتی‌ها و هزینه‌های تأییدشده مقداردهی می‌کند.

خروجی KSP فعلی اسکیمای نسخهٔ ۶ را تولید می‌کند؛ JSON هیچ نسخه‌ای در مخزن ثبت
نشده است. بازیابی اسکیماهای تاریخی معتبر و افزودن آزمون `MigrationTestHelper`
برای مسیرهای واقعی ارتقا هنوز لازم است. `DatabaseSchemaTest` پایگاه جدید v6 را
بررسی می‌کند و `LegacyFatCodeMigrationTest` و `CashLedgerMigrationTest` فقط SQL
مهاجرت‌های ۳→۴ و ۵→۶ را روی جداول حداقلی اجرا می‌کنند؛ هیچ‌یک آزمون کامل ارتقا نیست. روند پیشنهادی و جزئیات در
[`app/schemas/README.md`](../app/schemas/README.md) آمده است.
