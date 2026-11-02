package ir.hoboho.shooting.domain.report

/**
 * Persian report wording. The app ships a single locale, so keeping the PDF's
 * fixed labels here (rather than reaching for Android resources) keeps the
 * report builders pure and independently testable.
 */
object ReportText {
    const val BRAND = "شوتینگ"
    const val BRAND_LATIN = "SHOOTING"
    const val SUBTITLE = "ثبت و گزارش عملیات شوتینگ فیبر نوری"
    const val FOOTER = "تهیهشده با شوتینگ"

    const val TITLE_DAILY = "گزارش روزانه شوتینگ"
    const val TITLE_PERIOD = "گزارش دورهای شوتینگ"
    const val TITLE_EXPENSES = "گزارش هزینهها و تسویهها"

    const val PROJECT = "پروژه"
    const val DATE = "تاریخ"
    const val WORK_AREA = "محدوده کار"
    const val EMPLOYER = "کارفرما"
    const val SUPERVISOR = "سرپرست"
    const val WORKER_COUNT = "تعداد کارگران"
    const val NOTES = "یادداشتها"
    const val DAILY_TOTAL = "جمع طول شوتینگ روز"
    const val PERIOD_TOTAL = "مجموع طول شوتینگ دوره"
    const val RANGE = "بازه گزارش"
    const val OPERATIONS = "عملیات شوتینگ"
    const val DAILY_BREAKDOWN = "تفکیک روزانه"
    const val PROJECT_BREAKDOWN = "تفکیک پروژه"
    const val EXPENSE_SUMMARY = "هزینهها و تسویهها (تومان)"
    const val TOTAL_EXPENSES = "جمع هزینهها"
    const val TOTAL_SETTLED = "جمع دریافتی"
    const val TOTAL_OUTSTANDING = "مانده مطالبات"
    const val EXPENSE_LIST = "فهرست هزینهها"
    const val SETTLEMENT_LIST = "جزئیات تسویهها"
    const val LENGTH_NOTE = "طولها بر حسب متر و مبالغ بر حسب تومان هستند."
    const val SETTLED = "تسویهشده"
    const val PARTIAL = "تسویه جزئی"
    const val UNSETTLED = "تسویهنشده"
    const val DESCRIPTION = "شرح"
    const val CATEGORY = "دستهبندی"
    const val RECEIPT = "مرجع رسید"
    const val AMOUNT = "مبلغ"
    const val REIMBURSED = "دریافتشده"
    const val OUTSTANDING = "مانده"
    const val STATUS = "وضعیت"
    const val ROW = "ردیف"
    const val START_POINT = "نقطه شروع"
    const val START_CODE = "کد شروع"
    const val START_METER = "کنتور شروع"
    const val END_POINT = "نقطه پایان"
    const val END_CODE = "کد پایان"
    const val END_METER = "کنتور پایان"
    const val CABLE = "نوع کابل"
    const val LENGTH = "طول (متر)"
}
