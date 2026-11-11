package com.waha.app.util

private val DIACRITICS = Regex("[ؐ-ًؚ-ٰٟۖ-ۭـ]")

/** تطبيع للبحث: إزالة التشكيل وتوحيد الألف والياء والتاء المربوطة (مطابق لسكربت بناء القاعدة). */
fun normalizeArabic(s: String): String {
    var r = DIACRITICS.replace(s, "")
    for (a in "أإآٱ") r = r.replace(a, 'ا')
    return r.replace('ى', 'ي').replace('ة', 'ه')
}
