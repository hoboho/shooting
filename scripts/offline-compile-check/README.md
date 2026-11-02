# بررسی کامپایل آفلاین Kotlin

`run.sh` چهلودو فایل `core/`, `domain/`, `data/` و `di/` را با `kotlinc` واقعی،
کتابخانهٔ واقعی `kotlinx-coroutines-core` و **استابهای کوچک** APIهای
Android/Room/Hilt/Dagger کامپایل میکند. هدف، گرفتن خطاهای نحوی، نوعی،
`suspend` و ارجاعهای حلنشده در لایهٔ داده/منطق در محیطی است که Gradle و Maven
در دسترس نیستند.

```bash
KOTLINC=/path/to/kotlinc \
COROUTINES_JAR=/path/to/kotlinx-coroutines-core-jvm.jar \
  bash scripts/offline-compile-check/run.sh
```

اگر `COROUTINES_JAR` تنظیم نشود، اسکریپت آن را در `$KOTLIN_HOME/lib` یا کنار
`kotlinc` جستجو میکند. `R.kt` بهصورت پویا از `res/values/strings.xml` ساخته
میشود تا با منابع هماهنگ بماند.

**محدودیتها (مهم):** استابها فقط امضای تقریبی APIها هستند؛ این بررسی جایگزین
کامپایل اندروید، پردازش انوتیشن Room/KSP/Hilt، تولید `R` واقعی، لینت، ساخت APK
یا کامپایل صفحات Compose نیست. همچنین `app/src/main/java/.../ui` و `feature`
در آن شرکت نمیکنند؛ آنها توسط `scripts/check_sources.py` و در نهایت
`./gradlew :app:assembleDebug` بررسی میشوند.
