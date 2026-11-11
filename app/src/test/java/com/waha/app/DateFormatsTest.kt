package com.waha.app

import com.waha.app.util.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateFormatsTest {
    @Test fun arabicDigits() = assertEquals("٢٠٢٦", 2026.arabic())
    @Test fun clock12h() { assertEquals("١٢:٠٥", clockText(0, 5)); assertEquals("٣:٢١", clockText(15, 21)) }
    @Test fun ampm() { assertEquals("ص", ampmText(9)); assertEquals("م", ampmText(15)) }
    @Test fun gregorian() = assertEquals("١٠ نوفمبر ٢٠٢٦ م", gregorianText(LocalDate.of(2026, 11, 10)))
}
