package com.foxdog.strucalendar.data.recurrence

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class RecurrenceType {
    NONE,
    INTERVAL_DAYS,
    MONTHLY_NTH_WEEKDAY,
    WEEKLY_ON_DAYS
}

/**
 * 繰り返しルールの入力値をまとめたデータクラス。
 * weekday は java.time.DayOfWeek.value（月=1 〜 日=7）で統一する。
 */
data class RecurrenceRule(
    val type: RecurrenceType,
    val intervalDays: Int? = null,
    val nth: Int? = null,
    val weekday: Int? = null,
    val weekdays: Set<Int> = emptySet(),
    val endDate: LocalDate
)

object RecurrenceCalculator {

    private const val MAX_OCCURRENCES = 366


    fun defaultEndDate(startDate: LocalDate): LocalDate = startDate.plusYears(1)

    fun generateOccurrences(startDate: LocalDate, rule: RecurrenceRule): List<LocalDate> {
        return when (rule.type) {
            RecurrenceType.NONE -> listOf(startDate)
            RecurrenceType.INTERVAL_DAYS -> generateIntervalDays(startDate, rule)
            RecurrenceType.MONTHLY_NTH_WEEKDAY -> generateMonthlyNthWeekday(startDate, rule)
            RecurrenceType.WEEKLY_ON_DAYS -> generateWeeklyOnDays(startDate, rule)
        }
    }

    private fun generateWeeklyOnDays(startDate: LocalDate, rule: RecurrenceRule): List<LocalDate> {
        val weekdays = rule.weekdays.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }.toSet()
        if (weekdays.isEmpty()) return listOf(startDate)

        val result = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(rule.endDate) && result.size < MAX_OCCURRENCES) {
            if (current.dayOfWeek in weekdays) {
                result.add(current)
            }
            current = current.plusDays(1)
        }
        return result
    }

    private fun generateIntervalDays(startDate: LocalDate, rule: RecurrenceRule): List<LocalDate> {
        val interval = (rule.intervalDays ?: 1).coerceAtLeast(1)
        val result = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(rule.endDate) && result.size < MAX_OCCURRENCES) {
            result.add(current)
            current = current.plusDays(interval.toLong())
        }
        return result
    }

    private fun generateMonthlyNthWeekday(startDate: LocalDate, rule: RecurrenceRule): List<LocalDate> {
        val nth = rule.nth ?: 1
        val weekday = rule.weekday?.let { DayOfWeek.of(it) } ?: startDate.dayOfWeek

        val result = mutableListOf<LocalDate>()
        var yearMonth = YearMonth.from(startDate)

        while (result.size < MAX_OCCURRENCES) {
            val occurrence = nthWeekdayOfMonth(yearMonth, weekday, nth)
            if (occurrence != null && !occurrence.isBefore(startDate)) {
                if (occurrence.isAfter(rule.endDate)) break
                result.add(occurrence)
            }
            if (yearMonth.atEndOfMonth().isAfter(rule.endDate) && occurrence != null && occurrence.isAfter(rule.endDate)) break
            yearMonth = yearMonth.plusMonths(1)
            if (yearMonth.atDay(1).isAfter(rule.endDate)) break
        }

        return result
    }

    /**
     * 指定した年月の「第nth ○曜日」を返す。nth=5は「その月の最終○曜日」として扱う。
     * 該当日が存在しない場合（例：5週目が無い月でnth=4指定など）はnullを返す。
     */
    private fun nthWeekdayOfMonth(yearMonth: YearMonth, weekday: DayOfWeek, nth: Int): LocalDate? {
        return if (nth in 1..4) {
            var date = yearMonth.atDay(1)
            while (date.dayOfWeek != weekday) date = date.plusDays(1)
            val candidate = date.plusWeeks((nth - 1).toLong())
            if (candidate.month == yearMonth.month) candidate else null
        } else {
            // nth == 5（またはそれ以外の値）→ 最終週として扱う
            var date = yearMonth.atEndOfMonth()
            while (date.dayOfWeek != weekday) date = date.minusDays(1)
            date
        }
    }
}