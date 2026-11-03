# امضای نسخهٔ انتشار (Release Signing)

این سند تنها مرجع امضای APK انتشار است. اصول آن تغییر ناپذیر اند:

- کلید خصوصی، رمزها و فایل Keystore **هرگز** وارد مخزن Git، لاگ CI، artifact عمومی
  یا گفتگو نمی شوند.
- کلید امضا **دائمی** است و برای همهٔ نسخه های آینده همان می ماند. گم شدن یا
  تعویض آن یعنی کاربران نمی توانند نسخهٔ تازه را روی نسخهٔ قبلی نصب کنند و ناچار
  به انتشار با `applicationId` تازه خواهیم بود.
- APK دیباگ فقط در پیش انتشار `debug-latest` قرار می گیرد و **هرگز** به عنوان
  نسخهٔ Release معرفی یا منتشر نمی شود.
- انتشار Release بدون اقدام و تأیید صریح مالک مخزن انجام نمی شود.

## ۱. کلید دائمی

محل پیشنهادی روی رایانهٔ امن مالک: `~/.shooting-signing/` (بیرون از مخزن).
در محیط کاری فعلی، یک کلید واقعی در `/home/user/.shooting-signing/` ساخته شده
است که بیرون از دایرکتوری ریپازیتوری `/home/user/shouting` قرار دارد:

| فایل | توضیح |
| --- | --- |
| `shooting-release.jks` | Keystore نوع JKS، کلید RSA ۳۰۷۲ بیت، `SHA256withRSA`، اعتبار ۱۰۹۵۰ روز، alias `shooting-release` |
| `credentials.env` | تنها سه مقدار `RELEASE_KEYSTORE_PASSWORD`، `RELEASE_KEY_PASSWORD` و `RELEASE_KEY_ALIAS` |
| `keystore.base64` | همان JKS به صورت base64 تک خطی، ویژهٔ Secret |
| `README.txt` | یادداشت محلی بدون هیچ مقدار محرمانه |

سطح دسترسی دایرکتوری `700` و فایلها `600` است. الگوهای `*.jks`، `*.p12`،
`*.base64`، `credentials.env` و `.shooting-signing/` نیز در `.gitignore` مخزن
قرار دارند تا حتی کپی تصادفی داخل ریپو کامیت نشود.

**وظیفهٔ مالک:** از این پوشه یک نسخهٔ پشتیبان رمزگذاری شده در مخزن امن خود
(مدیر رمز عبور / vault آفلاین) بگیرید و همان چهار مقدار زیر را برای همیشه نگه
دارید. Keystore را rotate نکنید و alias را عوض نکنید. چون محیط کاری این نشست
اشتراکی است، پس از انتقال پشتیبان، کپی داخل آن را حذف کنید تا تنها نسخهٔ معتبر
در اختیار شما و چهار Secret گیت هاب بماند.

## ۲. Secrets مورد نیاز (نام های دقیق)

همه در `Settings → Secrets and variables → Actions → Repository secrets`:

| نام Secret | محتوا |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | محتوای `shooting-release.jks` به صورت base64 تک خطی (همان `keystore.base64`) |
| `RELEASE_KEYSTORE_PASSWORD` | رمز Keystore |
| `RELEASE_KEY_ALIAS` | `shooting-release` |
| `RELEASE_KEY_PASSWORD` | رمز کلید خصوصی داخل Keystore |

`GITHUB_TOKEN` به طور خودکار توسط GitHub Actions فراهم می شود و نیازی به تنظیم
ندارد. متغیر `RELEASE_KEYSTORE_FILE` **Secret نیست**: در CI به صورت مسیری موقت در
`RUNNER_TEMP` ساخته می شود و پس از پایان job از بین می رود.

`app/build.gradle.kts` فقط این چهار متغیر محیطی را می خواند. اگر هر کدام خالی
باشد، پیکربندی Signing ساخته نمی شود و variant ریلیز بدون امضا باقی می ماند؛
گردش کار در آن حالت از انتشار جلوگیری می کند.

## ۳. تنظیم Secrets (این نشست دسترسی نداشت)

توکن GitHub App این نشست برای `actions/secrets` با خطای
`403 Resource not accessible by integration` روبه رو شد، بنابراین مقادیر توسط
عامل تنظیم نشدند. مالک مخزن باید یک بار آنها را ثبت کند.

روش پیشنهادی با `gh` (هیچ مقداری در ترمینال چاپ نمی شود):

```bash
cd ~
chmod 700 ~/.shooting-signing
set -a
source ~/.shooting-signing/credentials.env
set +a

gh secret set RELEASE_KEYSTORE_BASE64 -R hoboho/shooting < ~/.shooting-signing/keystore.base64
printf '%s' "$RELEASE_KEYSTORE_PASSWORD" | gh secret set RELEASE_KEYSTORE_PASSWORD -R hoboho/shooting
printf '%s' "$RELEASE_KEY_PASSWORD"       | gh secret set RELEASE_KEY_PASSWORD       -R hoboho/shooting
printf '%s' "$RELEASE_KEY_ALIAS"          | gh secret set RELEASE_KEY_ALIAS          -R hoboho/shooting

gh secret list -R hoboho/shooting   # فقط نامها را نشان می دهد
```

اگر Keystore را خودتان تازه می سازید (فقط یک بار، همان کلید دائمی):

```bash
keytool -genkeypair -alias shooting-release \
  -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10950 \
  -storetype JKS -keystore ~/.shooting-signing/shooting-release.jks \
  -storepass "$RELEASE_KEYSTORE_PASSWORD" -keypass "$RELEASE_KEY_PASSWORD" \
  -dname "CN=Shooting Release, OU=Android, O=Hoboho, C=DE"
base64 -w0 ~/.shooting-signing/shooting-release.jks > ~/.shooting-signing/keystore.base64
```

روش جایگرین: در همان صفحهٔ Secrets، چهار ردیف بالا را با «New repository
secret» وارد کنید (`RELEASE_KEYSTORE_BASE64` را از محتوای `keystore.base64`
کپی کنید).

همچنین محیط `release-approval` را بسازید:
`Settings → Environments → New environment` با نام `release-approval` و
«Required reviewers» را روی خودتان بگذارید. job انتشار به این محیط وابسته است.

## ۴. گردش کار CI

فایل `.github/workflows/android.yml`:

| job | کار |
| --- | --- |
| `static`, `unit`, `instrumented` | بررسی منابع، تست های JVM، `lintDebug`، APK دیباگ، تست های Room روی API ۳۴ |
| `bundle` | فقط به روز کردن **پیش انتشار** `debug-latest` (APK و ZIP دیباگ)؛ هیچ Release نسخهداری از APK دیباگ ساخته نمی شود |
| `release` | `lintRelease`، پیدا کردن `apksigner`، بررسی وجود Secrets، decode موقت Keystore در `RUNNER_TEMP`، `assembleRelease`، **`apksigner verify`**، و بارگذاری `shooting-release-apk` به عنوان artifact |
| `publish_release` | فقط با `workflow_dispatch` + `publish_release=true` + شاخهٔ `main` + محیط `release-approval`؛ ساخت Release نسخهدار `v<versionName>-release` با APK امضا شده |

اگر Secrets تنظیم نشده باشند، job `release` همچنان `lintRelease` و بررسی ابزار
`apksigner` را اجرا می کند، سپس با یک `::warning::` مراحل ساخت و اعتبارسنجی APK
امضا شده را رد می کند. در این حالت هیچ APK ریلیز و هیچ Release ای منتشر نمی شود
و CI قرمز نمی شود.

artifact `shooting-release-apk` فقط APK امضا شده را دارد؛ Keystore و رمزها هرگز
به artifact اضافه نمی شوند (مسیر آنها `RUNNER_TEMP` است و `chmod 600` دارند).

## ۵. اعتبارسنجی با apksigner

در CI، مرحلهٔ `Verify signature with apksigner` اجباری است و در صورت امضا
نبودن APK، شکست می خورد:

```bash
APK="app/build/outputs/apk/release/app-release.apk"
APKSIGNER="$(find "$ANDROID_HOME/build-tools" -type f -name apksigner | sort -V | tail -n 1)"
"$APKSIGNER" verify --verbose --min-sdk-version 26 "$APK"
```

همان کار به صورت محلی:

```bash
set -a
source ~/.shooting-signing/credentials.env
set +a
export RELEASE_KEYSTORE_FILE="$HOME/.shooting-signing/shooting-release.jks"
./gradlew :app:assembleRelease
APKSIGNER="$(find "$ANDROID_HOME/build-tools" -type f -name apksigner | sort -V | tail -n 1)"
"$APKSIGNER" verify --verbose --min-sdk-version 26 \
  app/build/outputs/apk/release/app-release.apk
```

برای بررسی سلامت خود Keystore (بدون چاپ محتوای محرمانه):

```bash
keytool -list -keystore "$RELEASE_KEYSTORE_FILE" \
  -storepass "$RELEASE_KEYSTORE_PASSWORD" -alias "$RELEASE_KEY_ALIAS" > /dev/null
```

## ۶. انتشار تنها با تأیید مالک

هیچ Release ای به طور خودکار منتشر نمی شود. پس از ادغام PR و تنظیم Secrets،
مالک صریحاً اجرا می کند:

```bash
gh workflow run android.yml --ref main -f publish_release=true
```

سپس درخواست «Required reviewers» محیط `release-approval` را تأیید می کند.
بدون این دو اقدام، job `publish_release` اجرا نمی شود. ادغام PR نیز جداگانه و
فقط با تأیید مالک انجام می شود.

## ۷. چک لیست امنیتی

- [ ] Keystore و `credentials.env` بیرون از مخزن، با `chmod 600/700`
- [ ] پشتیبان آفلاین و رمزنگاری شده از Keystore و چهار مقدار
- [ ] چهار Secret با نام های دقیق بالا ثبت شده اند
- [ ] محیط `release-approval` با reviewer تنظیم شده است
- [ ] `git status` هیچ فایل `.jks` / `.base64` / `credentials.env` ای نشان نمی دهد
- [ ] `apksigner verify` در CI روی APK ریلیز موفق شده است
- [ ] هیچ Release ای بدون تأیید مالک ساخته نشده است
