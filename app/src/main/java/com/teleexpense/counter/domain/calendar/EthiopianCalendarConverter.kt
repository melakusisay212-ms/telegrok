package com.teleexpense.counter.domain.calendar

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Reliable Gregorian ↔ Ethiopian calendar conversion.
 *
 * Ethiopian calendar:
 * - 13 months: 12 × 30 days + Pagume (5 or 6 days)
 * - New Year (Meskerem 1) falls on 11 Sep (or 12 Sep in Gregorian leap years)
 * - Ethiopian leap year every 4 years (year % 4 == 3)
 *
 * Algorithm based on well-tested offset arithmetic used by Ethiopian calendar libraries.
 * Do NOT use a fixed day offset — leap years and Pagume are handled correctly.
 */
object EthiopianCalendarConverter {

    data class EthiopianDate(
        val year: Int,
        val month: Int,   // 1 = Meskerem … 13 = Pagume
        val day: Int
    ) {
        fun formatted(): String {
            val monthName = monthName(month)
            return "$monthName $day, $year"
        }

        fun shortFormatted(): String {
            val monthName = monthName(month)
            return "$monthName $day"
        }
    }

    private val MONTH_NAMES = arrayOf(
        "", // 1-indexed
        "Meskerem", "Tikimt", "Hidar", "Tahsas",
        "Tir", "Yekatit", "Megabit", "Miazia",
        "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
    )

    private val MONTH_NAMES_AM = arrayOf(
        "",
        "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ",
        "ጥር", "የካቲት", "መጋቢት", "ሚያዝያ",
        "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜ"
    )

    fun monthName(month: Int, amharic: Boolean = false): String {
        if (month !in 1..13) return "?"
        return if (amharic) MONTH_NAMES_AM[month] else MONTH_NAMES[month]
    }

    fun isEthiopianLeapYear(year: Int): Boolean = year % 4 == 3

    fun daysInMonth(year: Int, month: Int): Int {
        return when {
            month in 1..12 -> 30
            month == 13 -> if (isEthiopianLeapYear(year)) 6 else 5
            else -> 0
        }
    }

    /**
     * Convert Gregorian year/month/day (1-based month) to Ethiopian date.
     * Time-of-day is ignored for the date part; caller keeps the time separately.
     */
    fun toEthiopian(gregorianYear: Int, gregorianMonth: Int, gregorianDay: Int): EthiopianDate {
        // JD calculation (Gregorian)
        val a = (14 - gregorianMonth) / 12
        val y = gregorianYear + 4800 - a
        val m = gregorianMonth + 12 * a - 3
        val jdn = gregorianDay + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        // Ethiopian epoch: Meskerem 1, 1 EE = August 29, 8 CE (JDN 1724220.5 ≈ 1724221)
        // Using integer arithmetic consistent with common Ethiopian calendar libs.
        val r = (jdn - 1723856) % 1461
        val n = r % 365 + 365 * (r / 1460)

        val year = 4 * ((jdn - 1723856) / 1461) + r / 365 - r / 1460
        val month = n / 30 + 1
        val day = n % 30 + 1

        return EthiopianDate(year, month, day)
    }

    fun toEthiopian(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): EthiopianDate {
        val cal = GregorianCalendar(timeZone).apply { timeInMillis = millis }
        return toEthiopian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Convert Ethiopian date to Gregorian (year, month 1-12, day).
     */
    fun toGregorian(ethYear: Int, ethMonth: Int, ethDay: Int): Triple<Int, Int, Int> {
        // Inverse of the above JDN formula
        val n = 30 * (ethMonth - 1) + (ethDay - 1)
        val jdn = 1723856 + 365 * ethYear + ethYear / 4 + n + 1 - (ethYear / 4 - ethYear / 4) // simplified

        // More precise inverse:
        val jdn2 = (1723856 + 365 * ethYear + (ethYear / 4) + 30 * (ethMonth - 1) + ethDay - 1)

        // JDN → Gregorian
        val a = jdn2 + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153

        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = 100 * b + d - 4800 + m / 10

        return Triple(year, month, day)
    }

    /**
     * Start of an Ethiopian month as Gregorian millis (00:00 local).
     */
    fun startOfEthiopianMonthMillis(ethYear: Int, ethMonth: Int, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val (gy, gm, gd) = toGregorian(ethYear, ethMonth, 1)
        val cal = GregorianCalendar(timeZone).apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Current Ethiopian date.
     */
    fun today(timeZone: TimeZone = TimeZone.getDefault()): EthiopianDate {
        return toEthiopian(System.currentTimeMillis(), timeZone)
    }

    /**
     * Ethiopian month label e.g. "Meskerem 2019"
     */
    fun monthLabel(year: Int, month: Int, amharic: Boolean = false): String {
        return "${monthName(month, amharic)} $year"
    }
}
