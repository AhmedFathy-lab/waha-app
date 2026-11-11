# واحة | WAHA

تطبيق إسلامي لأندرويد: القرآن الكريم، الأذكار، الأدعية، المسبحة، مواقيت الصلاة. Kotlin + Jetpack Compose + Material 3.

## حالة المشروع (يرجى القراءة)
كُتب هذا المشروع في بيئة بلا Android SDK، لذلك **لم يُجمَّع كود Compose/Android ولم يُجرَّب على جهاز بعد**. اختُبر فعلياً: محلل القرآن (مطابقة 6236 آية كلمة بكلمة)، وبيانات الأذكار، واختبارات التواريخ والتطبيع. توقّع إصلاح أخطاء تجميع صغيرة عند أول بناء، خصوصاً: أسماء حزم مكتبة `adhan2` وبعض أسماء الأيقونات.

## البناء
### الطريقة 1: Android Studio (الأسهل)
افتح المجلد في Android Studio (Koala أو أحدث، JDK 17) ← انتظر مزامنة Gradle ← Run ▶ على جهاز/محاكي. للـ APK: Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s).

### الطريقة 2: سطر الأوامر
```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk (قابل للتثبيت)
```
### Release موقّع
```
tools/make-keystore.sh
export WAHA_KEYSTORE=$PWD/waha-release.jks WAHA_KS_PASS=... WAHA_KEY_ALIAS=waha WAHA_KEY_PASS=...
./gradlew :app:assembleRelease    # app/build/outputs/apk/release/app-release.apk
```
### الطريقة 3: GitHub Actions
ارفع المشروع إلى مستودع GitHub، ثم Actions ▸ Build WAHA APK ▸ Run workflow، ونزّل الملف من Artifacts. لنسخة Release أضف الأسرار: `WAHA_KEYSTORE_B64` و`WAHA_KS_PASS` و`WAHA_KEY_ALIAS` و`WAHA_KEY_PASS`.

## البيانات
| الملف | المصدر | الأداة |
|---|---|---|
| `app/src/main/assets/quran.db` | ARQURAN.TXT | `tools/build_quran.py` ثم `tools/verify_quran.py` |
| `app/src/main/assets/adhkar.db` | hesn-utf8.txt (حصن المسلم) | `tools/build_adhkar.py` |

إعادة البناء: `python3 tools/build_quran.py ARQURAN.TXT app/src/main/assets/quran.db` (ويُنصح بتشغيل verify بعدها).

## ملاحظات
- لا توجد علامات وقف في مصدر القرآن، ولا بيانات أحزاب؛ يُعرض الجزء والصفحة فقط.
- الأدعية مأخوذة حالياً من أبواب الدعاء في حصن المسلم. لاعتماد كتاب آخر أعد بناء قاعدة البيانات من نص رقمي نظيف.
- تنبيهات الصلاة إشعارات نصية بلا صوت أذان.
- مخطط الحزم في `PLAN.md`.
