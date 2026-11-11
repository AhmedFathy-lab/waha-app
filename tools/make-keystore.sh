#!/usr/bin/env bash
# ينشئ مفتاح توقيع لواحة. احتفظ بالملف وكلمات المرور في مكان آمن: فقدانها يمنع تحديث التطبيق لاحقاً.
set -euo pipefail
keytool -genkeypair -v -keystore waha-release.jks -alias waha -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Waha, O=Waha, C=EG"
echo "تم إنشاء waha-release.jks"
echo "للاستخدام المحلي:"
echo "  export WAHA_KEYSTORE=\$PWD/waha-release.jks WAHA_KS_PASS=... WAHA_KEY_ALIAS=waha WAHA_KEY_PASS=..."
echo "  ./gradlew :app:assembleRelease"
echo "لـ GitHub Actions: base64 -w0 waha-release.jks   ثم ضعه في سر WAHA_KEYSTORE_B64"
