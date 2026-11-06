# امضای نسخهٔ انتشار (Release Signing)

## کلید امضای نسخهٔ 2.0.0

برای نسخهٔ 2.0.0 یک کلید جدید دائمی ساخته شده است؛ کلید قبلی در دسترس نبود. این کلید فقط برای همین برنامه و نسخه‌های بعدی آن استفاده می‌شود. تعویض یا گم‌کردن این کلید در آینده باعث می‌شود نسخه‌های بعدی روی نصب‌های فعلی قابل به‌روزرسانی مستقیم نباشند.

مشخصات کلید:

- Keystore: `shooting-release.jks` (JKS)
- Alias: `shooting-release`
- الگوریتم: RSA 3072-bit، امضا SHA256withRSA
- اعتبار: ۱۰۹۵۰ روز
- فایل‌های محرمانه در بستهٔ محلی `shooting-2.0.0-signing-credentials.zip` تحویل داده می‌شوند و نباید وارد مخزن Git شوند.

## تنظیم GitHub Actions Secrets

در صفحهٔ [Settings → Secrets and variables → Actions](https://github.com/hoboho/shooting/settings/secrets/actions) این چهار Secret را بسازید:

| Secret | مقدار |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | تمام محتوای تک‌خطی فایل `keystore.base64` |
| `RELEASE_KEYSTORE_PASSWORD` | مقدار هم‌نام در `credentials.env` |
| `RELEASE_KEY_ALIAS` | `shooting-release` |
| `RELEASE_KEY_PASSWORD` | مقدار هم‌نام در `credentials.env` |

مقادیر محرمانه را در گفتگو، Issue یا فایل‌های مخزن قرار ندهید. کلید خصوصی و رمزها باید در نسخهٔ پشتیبان رمزگذاری‌شده و آفلاین نگهداری شوند.

## گردش ساخت و انتشار

1. پس از ثبت هر چهار Secret، اجرای workflow روی `main` باید job با نام **Signed release APK** را اجرا کند.
2. این job کلید را فقط در فضای موقت runner رمزگشایی می‌کند، نسخهٔ Release را می‌سازد و با `apksigner verify` امضا را بررسی می‌کند.
3. اگر هر Secret موجود نباشد، job با خطا متوقف می‌شود؛ هیچ APK بدون امضا یا Debug به‌عنوان Release منتشر نخواهد شد.
4. انتشار در صفحهٔ GitHub Releases فقط با اجرای دستی workflow و تأیید محیط `release-approval` انجام می‌شود.

نام نسخهٔ انتشار `v2.0.0-release` و نام فایل `Shooting-release.apk` خواهد بود.

## هشدار سازگاری نصب

به‌دلیل تغییر کلید امضا، Android نصب 2.0.0 را به‌عنوان به‌روزرسانی روی نسخه‌ای که با کلید قبلی امضا شده قبول نمی‌کند. پیش از حذف نسخهٔ قبلی، از داخل برنامه فایل پشتیبان تهیه و آن را خارج از گوشی نگهداری کنید؛ سپس نسخهٔ قبلی را حذف و 2.0.0 را نصب کنید.
