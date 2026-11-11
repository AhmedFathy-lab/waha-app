package com.waha.app

import com.waha.app.util.normalizeArabic
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicTextTest {
    @Test fun stripsDiacritics() = assertEquals("بسم الله الرحمن الرحيم", normalizeArabic("بِسْمِ اللّهِ الرَّحْمـَنِ الرَّحِيمِ"))
    @Test fun unifiesLetters() = assertEquals("اذا ايه رحمه موسي", normalizeArabic("إذا آية رحمة موسى"))
}
