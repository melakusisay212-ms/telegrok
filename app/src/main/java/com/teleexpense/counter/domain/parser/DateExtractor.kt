package com.teleexpense.counter.domain.parser

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * Extracts explicit transaction dates from SMS bodies.
 * Prefer explicit date over SMS received timestamp.
 * Never invents a date.
 */
object DateExtractor {

    private val PATTERNS = listOf(
        // 11/09/2026 15:52:15
        Pattern.compile("""(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})"""),
        // 17/09/2026 08:38:04
        Pattern.compile("""(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})"""),
        // 30/09/26
        Pattern.compile("""(\d{1,2})/(\d{1,2})/(\d{2})(?:\s+(\d{1,2}):(\d{2}):(\d{2}))?"""),
        // Sep 17, 2026 8:38:04 AM
        Pattern.compile("""(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{1,2}),?\s+(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})\s*(AM|PM)?""", Pattern.CASE_INSENSITIVE),
        // 2026-09-17
        Pattern.compile("""(\d{4})-(\d{1,2})-(\d{1,2})(?:\s+(\d{1,2}):(\d{2}):(\d{2}))?"""),
        // on 11/09/2026 15:52:15
        Pattern.compile("""on\s+(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})""", Pattern.CASE_INSENSITIVE)
    )

    private val MONTH_MAP = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
        "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    /**
     * Returns epoch millis of the extracted transaction datetime, or null if none found.
     */
    fun extract(text: String, defaultTimeZone: TimeZone = TimeZone.getDefault()): Long? {
        for (pattern in PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                try {
                    return parseMatch(matcher, pattern, defaultTimeZone)
                } catch (_: Exception) {
                    // try next pattern
                }
            }
        }
        return null
    }

    private fun parseMatch(
        matcher: java.util.regex.Matcher,
        pattern: Pattern,
        tz: TimeZone
    ): Long? {
        val groupCount = matcher.groupCount()
        val g1 = matcher.group(1) ?: return null

        // Month-name style: Sep 17, 2026 ...
        if (g1.matches(Regex("[A-Za-z]+"))) {
            val month = MONTH_MAP[g1.lowercase().take(3)] ?: return null
            val day = matcher.group(2)?.toIntOrNull() ?: return null
            val year = matcher.group(3)?.toIntOrNull() ?: return null
            var hour = matcher.group(4)?.toIntOrNull() ?: 0
            val minute = matcher.group(5)?.toIntOrNull() ?: 0
            val second = matcher.group(6)?.toIntOrNull() ?: 0
            val ampm = matcher.group(7)?.uppercase()
            if (ampm == "PM" && hour < 12) hour += 12
            if (ampm == "AM" && hour == 12) hour = 0
            return toMillis(year, month, day, hour, minute, second, tz)
        }

        // ISO-like: 2026-09-17
        if (g1.length == 4) {
            val year = g1.toIntOrNull() ?: return null
            val month = matcher.group(2)?.toIntOrNull() ?: return null
            val day = matcher.group(3)?.toIntOrNull() ?: return null
            val hour = matcher.group(4)?.toIntOrNull() ?: 0
            val minute = matcher.group(5)?.toIntOrNull() ?: 0
            val second = matcher.group(6)?.toIntOrNull() ?: 0
            return toMillis(year, month, day, hour, minute, second, tz)
        }

        // dd/MM/yyyy or dd/MM/yy
        val day = g1.toIntOrNull() ?: return null
        val month = matcher.group(2)?.toIntOrNull() ?: return null
        var year = matcher.group(3)?.toIntOrNull() ?: return null
        if (year < 100) year += 2000
        val hour = if (groupCount >= 4) matcher.group(4)?.toIntOrNull() ?: 0 else 0
        val minute = if (groupCount >= 5) matcher.group(5)?.toIntOrNull() ?: 0 else 0
        val second = if (groupCount >= 6) matcher.group(6)?.toIntOrNull() ?: 0 else 0
        return toMillis(year, month, day, hour, minute, second, tz)
    }

    private fun toMillis(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int, second: Int,
        tz: TimeZone
    ): Long {
        val cal = java.util.GregorianCalendar(tz)
        cal.set(year, month - 1, day, hour, minute, second)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
