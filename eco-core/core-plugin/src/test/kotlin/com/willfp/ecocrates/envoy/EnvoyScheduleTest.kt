package com.willfp.ecocrates.envoy

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvoyScheduleTest {
    @Test
    fun `no times and no interval is disabled`() {
        val schedule = EnvoySchedule(emptyList(), 0)
        assertFalse(schedule.isEnabled)
        assertFalse(schedule.isDueAt(LocalTime.of(12, 0), null, 100_000))
    }

    @Test
    fun `time based start fires on the matching minute`() {
        val schedule = EnvoySchedule(listOf("12:00", "18:30"), 0)
        assertTrue(schedule.isDueAt(LocalTime.of(12, 0, 41), null, 0))
        assertTrue(schedule.isDueAt(LocalTime.of(18, 30, 0), null, 0))
    }

    @Test
    fun `time based start does not fire on other minutes`() {
        val schedule = EnvoySchedule(listOf("12:00"), 0)
        assertFalse(schedule.isDueAt(LocalTime.of(12, 1), null, 0))
        assertFalse(schedule.isDueAt(LocalTime.of(11, 59, 59), null, 0))
    }

    @Test
    fun `time based start does not fire twice in the same minute`() {
        val schedule = EnvoySchedule(listOf("12:00"), 0)
        val lastStart = LocalTime.of(12, 0, 3)
        assertFalse(schedule.isDueAt(LocalTime.of(12, 0, 44), lastStart, 820))
    }

    @Test
    fun `malformed times are ignored, not crashed on`() {
        val schedule = EnvoySchedule(listOf("banana", "25:99", "12:00"), 0)
        assertTrue(schedule.isEnabled)
        assertTrue(schedule.isDueAt(LocalTime.of(12, 0), null, 0))
    }

    @Test
    fun `times take priority over interval`() {
        val schedule = EnvoySchedule(listOf("12:00"), 20)
        // Interval would be long overdue, but times win, so nothing fires at 13:00.
        assertFalse(schedule.isDueAt(LocalTime.of(13, 0), null, 999_999))
    }

    @Test
    fun `interval fires once the tick count is reached`() {
        val schedule = EnvoySchedule(emptyList(), 1200)
        assertFalse(schedule.isDueAt(LocalTime.of(9, 0), null, 1199))
        assertTrue(schedule.isDueAt(LocalTime.of(9, 0), null, 1200))
        assertTrue(schedule.isDueAt(LocalTime.of(9, 0), null, 5000))
    }
}
