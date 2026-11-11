# واحة | WAHA — خطة المشروع

## الطبقات (MVVM)
- data: مصادر البيانات (SQLite للقراءة فقط لقاعدة القرآن والأذكار والأدعية، DataStore للإعدادات والمفضلة والتقدم والسجل)
- domain: النماذج وحالات الاستخدام (حساب المواقيت، التاريخ الهجري)
- ui: Compose + ViewModel لكل شاشة، تنقل، سمة
- notifications: AlarmManager + قناة إشعارات + استقبال إعادة التشغيل

## الحزم (com.waha.app)
data/{quran,adhkar,duas,prayer,settings,db} · domain · ui/{theme,home,quran,adhkar,duas,tasbih,prayer,settings,navigation} · notifications · util

## خط البيانات
tools/*.py تحوّل ARQURAN.TXT و hesn-utf8.txt وكتاب الأدعية إلى waha.db داخل assets (مع اختبارات تحقق: 6236 آية، مطابقة النص).

## البناء
minSdk 26 · target 34 · Release موقّع عبر متغيرات WAHA_KEYSTORE… · .github/workflows/build-apk.yml
