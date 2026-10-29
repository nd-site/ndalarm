package com.example

import com.example.data.model.AlarmEntity
import com.example.util.LunarCalendarHelper
import com.example.util.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NDAlarmUnitTest {

    @Test
    fun `test SecurityHelper generates 50-character token with high entropy`() {
        val token = SecurityHelper.generateSecretToken50()
        assertEquals(50, token.length)
        assertTrue(SecurityHelper.verifyToken(token, token))
        assertFalse(SecurityHelper.verifyToken(token, "wrong_token"))
    }

    @Test
    fun `test SecurityHelper generates 8-digit emergency PIN`() {
        val pin = SecurityHelper.generateEmergencyPin8()
        assertEquals(8, pin.length)
        assertTrue(pin.all { it.isDigit() })
        assertTrue(SecurityHelper.verifyPin(pin, pin))
        assertFalse(SecurityHelper.verifyPin(pin, "00000000".takeIf { it != pin } ?: "11111111"))
    }

    @Test
    fun `test LunarCalendarHelper conversion`() {
        val todayLunar = LunarCalendarHelper.getTodayLunarDate()
        assertNotNull(todayLunar)
        assertTrue(todayLunar.day in 1..30)
        assertTrue(todayLunar.month in 1..12)

        val lunarString = LunarCalendarHelper.getTodayLunarString()
        assertTrue(lunarString.contains("ÂL"))
        assertTrue(lunarString.contains("Năm"))

        val lunar = LunarCalendarHelper.convertSolar2Lunar(10, 2, 2024)
        assertNotNull(lunar)
        assertTrue(lunar.day in 1..30)
    }

    @Test
    fun `test AlarmEntity model defaults`() {
        val alarm = AlarmEntity(
            label = "Morning Alarm",
            hour = 6,
            minute = 30,
            clockId = "ND-CL01",
            ndid = "ND-MASTER"
        )
        assertTrue(alarm.isEnabled)
        assertFalse(alarm.isLunarCalendar)
        assertEquals("DAILY", alarm.repeatRule)
        assertEquals("idle", alarm.status)
    }
}
