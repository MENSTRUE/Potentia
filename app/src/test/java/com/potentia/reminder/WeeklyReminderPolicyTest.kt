package com.potentia.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklyReminderPolicyTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun nextTrigger_onSundayBeforeSeven_isSameSundayAtSeven() {
        val now = ZonedDateTime.of(2026, 9, 20, 10, 0, 0, 0, zone)
        val next = WeeklyReminderPolicy.nextTrigger(now)

        assertEquals(2026, next.year)
        assertEquals(9, next.monthValue)
        assertEquals(20, next.dayOfMonth)
        assertEquals(19, next.hour)
        assertEquals(0, next.minute)
    }

    @Test
    fun nextTrigger_onSundayAfterSeven_movesToNextWeek() {
        val now = ZonedDateTime.of(2026, 9, 20, 20, 0, 0, 0, zone)
        val next = WeeklyReminderPolicy.nextTrigger(now)

        assertEquals(27, next.dayOfMonth)
        assertEquals(19, next.hour)
        assertTrue(next.isAfter(now))
    }
}
