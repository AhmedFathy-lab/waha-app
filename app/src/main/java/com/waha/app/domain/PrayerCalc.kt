package com.waha.app.domain

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.LocalDate
import java.time.ZoneId

enum class Prayer(val key: String, val title: String) {
    FAJR("fajr", "الفجر"), SUNRISE("sunrise", "الشروق"), DHUHR("dhuhr", "الظهر"),
    ASR("asr", "العصر"), MAGHRIB("maghrib", "المغرب"), ISHA("isha", "العشاء");
    val isPrayer get() = this != SUNRISE
}

data class SavedLocation(val name: String, val lat: Double, val lon: Double, val tz: String, val gps: Boolean = false)

object Methods {
    /** المفتاح ← الاسم العربي للعرض. الحساب نفسه يتم داخل مكتبة Adhan. */
    val labels = linkedMapOf(
        "MUSLIM_WORLD_LEAGUE" to "رابطة العالم الإسلامي",
        "EGYPTIAN" to "الهيئة المصرية العامة للمساحة",
        "UMM_AL_QURA" to "أم القرى (مكة المكرمة)",
        "KARACHI" to "جامعة العلوم الإسلامية بكراتشي",
        "NORTH_AMERICA" to "الجمعية الإسلامية لأمريكا الشمالية",
        "DUBAI" to "دبي",
        "QATAR" to "قطر",
        "KUWAIT" to "الكويت",
        "TURKEY" to "رئاسة الشؤون الدينية التركية",
        "SINGAPORE" to "سنغافورة",
        "MOON_SIGHTING_COMMITTEE" to "لجنة رؤية الهلال",
    )
    fun of(key: String): CalculationMethod = when (key) {
        "EGYPTIAN" -> CalculationMethod.EGYPTIAN
        "UMM_AL_QURA" -> CalculationMethod.UMM_AL_QURA
        "KARACHI" -> CalculationMethod.KARACHI
        "NORTH_AMERICA" -> CalculationMethod.NORTH_AMERICA
        "DUBAI" -> CalculationMethod.DUBAI
        "QATAR" -> CalculationMethod.QATAR
        "KUWAIT" -> CalculationMethod.KUWAIT
        "TURKEY" -> CalculationMethod.TURKEY
        "SINGAPORE" -> CalculationMethod.SINGAPORE
        "MOON_SIGHTING_COMMITTEE" -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
    }
}

object PrayerCalc {
    /** أوقات يوم كامل (بالميلي ثانية UTC) لتاريخ محلي في منطقة الموقع الزمنية. */
    fun day(loc: SavedLocation, methodKey: String, madhabKey: String, date: LocalDate): List<Pair<Prayer, Long>> {
        val params = Methods.of(methodKey).parameters.copy(madhab = if (madhabKey == "HANAFI") Madhab.HANAFI else Madhab.SHAFI)
        val t = PrayerTimes(Coordinates(loc.lat, loc.lon), DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
        return listOf(
            Prayer.FAJR to t.fajr.toEpochMilliseconds(), Prayer.SUNRISE to t.sunrise.toEpochMilliseconds(),
            Prayer.DHUHR to t.dhuhr.toEpochMilliseconds(), Prayer.ASR to t.asr.toEpochMilliseconds(),
            Prayer.MAGHRIB to t.maghrib.toEpochMilliseconds(), Prayer.ISHA to t.isha.toEpochMilliseconds(),
        )
    }

    fun today(loc: SavedLocation, methodKey: String, madhabKey: String, nowMs: Long = System.currentTimeMillis()): List<Pair<Prayer, Long>> =
        day(loc, methodKey, madhabKey, LocalDate.now(ZoneId.of(loc.tz)))

    /** الصلاة القادمة (من الخمس فقط، دون الشروق) بين المفاتيح المسموحة، مع البحث في اليوم التالي ثم الذي بعده. */
    fun next(loc: SavedLocation, methodKey: String, madhabKey: String, nowMs: Long, allowed: Set<String> = Prayer.entries.filter { it.isPrayer }.map { it.key }.toSet()): Pair<Prayer, Long>? {
        val start = LocalDate.now(ZoneId.of(loc.tz)).minusDays(1)
        for (d in 0..3) {
            val hit = day(loc, methodKey, madhabKey, start.plusDays(d.toLong()))
                .firstOrNull { (p, t) -> p.isPrayer && p.key in allowed && t > nowMs }
            if (hit != null) return hit
        }
        return null
    }
}
