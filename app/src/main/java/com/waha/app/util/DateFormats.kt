package com.waha.app.util

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val AR = Locale("ar")
private val HIJRI_MONTHS = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
)
private val GREG_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

/** تحويل الأرقام اللاتينية إلى هندية (٠-٩). */
fun Int.arabic(): String = toString().map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")
fun String.arabicDigits(): String = map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

fun hijriText(date: LocalDate): String {
    val h = HijrahDate.from(HijrahChronology.INSTANCE.date(date))
    return "${h.get(java.time.temporal.ChronoField.DAY_OF_MONTH).arabic()} " +
        "${HIJRI_MONTHS[h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) - 1]} " +
        "${h.get(java.time.temporal.ChronoField.YEAR).arabic()} هـ"
}

fun gregorianText(date: LocalDate): String =
    "${date.dayOfMonth.arabic()} ${GREG_MONTHS[date.monthValue - 1]} ${date.year.arabic()} م"

fun weekdayText(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, AR)

fun clockText(h: Int, m: Int): String {
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "${h12.arabic()}:${m.toString().padStart(2, '0').arabicDigits()}"
}
fun ampmText(h: Int) = if (h < 12) "ص" else "م"
