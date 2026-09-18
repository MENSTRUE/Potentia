package com.potentia.reminder

import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object WeeklyReminderPolicy {
    const val HOUR = 19
    const val MINUTE = 0
    val dayOfWeek: DayOfWeek = DayOfWeek.SUNDAY

    fun nextTrigger(now: ZonedDateTime): ZonedDateTime {
        var candidate = now
            .with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .withHour(HOUR)
            .withMinute(MINUTE)
            .withSecond(0)
            .withNano(0)

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1)
        }

        return candidate
    }

    fun initialDelayMillis(now: ZonedDateTime = ZonedDateTime.now()): Long =
        Duration.between(now, nextTrigger(now)).toMillis().coerceAtLeast(0L)

    const val DISPLAY_LABEL = "Setiap Minggu sekitar pukul 19.00"
}
