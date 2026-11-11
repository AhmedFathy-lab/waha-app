package com.waha.app

import com.waha.app.data.prayer.Cities
import com.waha.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PrayerCalcTest {
    private val cairo = Cities.all.first { it.name == "القاهرة" }.let { SavedLocation(it.name, it.lat, it.lon, it.tz) }

    @Test fun orderIsChronologicalForEveryCityAndMethod() {
        val date = LocalDate.of(2026, 11, 10)
        for (c in Cities.all) for (m in Methods.labels.keys) {
            val t = PrayerCalc.day(SavedLocation(c.name, c.lat, c.lon, c.tz), m, "SHAFI", date).map { it.second }
            assertEquals("${c.name}/$m", t.sorted(), t)
        }
    }
    @Test fun hanafiAsrIsLaterThanShafi() {
        val d = LocalDate.of(2026, 6, 21)
        val a = PrayerCalc.day(cairo, "EGYPTIAN", "SHAFI", d).first { it.first == Prayer.ASR }.second
        val h = PrayerCalc.day(cairo, "EGYPTIAN", "HANAFI", d).first { it.first == Prayer.ASR }.second
        assertTrue(h > a)
    }
    @Test fun nextIsAfterNowAndSkipsSunrise() {
        val now = System.currentTimeMillis()
        val n = PrayerCalc.next(cairo, "EGYPTIAN", "SHAFI", now)!!
        assertTrue(n.second > now); assertTrue(n.first.isPrayer)
        assertTrue(n.second - now < 26 * 3600_000L)
    }
    @Test fun allowedFilterIsRespected() {
        val n = PrayerCalc.next(cairo, "EGYPTIAN", "SHAFI", System.currentTimeMillis(), setOf("fajr"))!!
        assertEquals(Prayer.FAJR, n.first)
    }
    @Test fun everyCityHasValidTimezoneAndKnownMethod() {
        for (c in Cities.all) { java.time.ZoneId.of(c.tz); assertTrue(c.name, c.method in Methods.labels) }
    }
}
