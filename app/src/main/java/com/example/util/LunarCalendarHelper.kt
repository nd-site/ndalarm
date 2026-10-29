package com.example.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.sin

/**
 * Lunar Calendar conversion algorithm (Lịch Âm Dương Việt Nam / Đông Á)
 * Implementation of astronomical algorithms by Hồ Ngọc Đức for time zone GMT+7.
 */
object LunarCalendarHelper {

    data class LunarDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val isLeapMonth: Boolean = false,
        val dayCanChi: String = "",
        val monthCanChi: String = "",
        val yearCanChi: String = ""
    )

    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    private const val TIMEZONE = 7.0 // Vietnam timezone GMT+7

    /**
     * Convert solar date to Julian Day Number
     */
    fun jdFromDate(d: Int, m: Int, y: Int): Int {
        val a = floor((14 - m) / 12.0).toInt()
        val y1 = y + 4800 - a
        val m1 = m + 12 * a - 3
        return d + floor((153 * m1 + 2) / 5.0).toInt() + 365 * y1 + floor(y1 / 4.0).toInt() - floor(y1 / 100.0).toInt() + floor(y1 / 400.0).toInt() - 32045
    }

    /**
     * Convert Julian Day Number to Solar date array [d, m, y]
     */
    fun jdToDate(jd: Int): IntArray {
        val a = jd + 32044
        val b = floor((4 * a + 3) / 146097.0).toInt()
        val c = a - floor((146097 * b) / 4.0).toInt()
        val d = floor((4 * c + 3) / 1461.0).toInt()
        val e = c - floor((1461 * d) / 4.0).toInt()
        val m = floor((5 * e + 2) / 153.0).toInt()
        val day = e - floor((153 * m + 2) / 5.0).toInt() + 1
        val month = m + 3 - 12 * floor(m / 10.0).toInt()
        val year = 100 * b + d - 4800 + floor(m / 10.0).toInt()
        return intArrayOf(day, month, year)
    }

    private fun getNewMoonDay(k: Int, timeZone: Double): Int {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = Math.PI / 180
        var jde = 2451550.09765 + 29.530588853 * k + 0.0001337 * t2 - 0.000000150 * t3 + 0.00000000073 * t2 * t2
        val m = 2.5534 + 29.10535669 * k - 0.0000218 * t2 - 0.00000011 * t3
        val mprime = 201.5643 + 385.81693528 * k + 0.0107438 * t2 + 0.00001239 * t3 - 0.000000058 * t2 * t2
        val f = 160.7108 + 390.67050274 * k - 0.0016341 * t2 - 0.00000227 * t3 + 0.000000011 * t2 * t2
        val omega = 124.7746 - 1.56375580 * k + 0.0020691 * t2 + 0.00000215 * t3

        var pt = 0.0
        pt -= 0.40720 * sin(mprime * dr)
        pt += 0.17241 * sin(m * dr)
        pt += 0.01608 * sin(2 * mprime * dr)
        pt += 0.01039 * sin(2 * f * dr)
        pt += 0.00739 * sin((mprime - m) * dr)
        pt -= 0.00514 * sin((mprime + m) * dr)
        pt += 0.00208 * sin(2 * m * dr)
        pt -= 0.00111 * sin((mprime - 2 * f) * dr)
        pt -= 0.00057 * sin((2 * mprime + m) * dr)
        pt += 0.00056 * sin((2 * mprime - m) * dr)
        pt -= 0.00042 * sin((mprime + 2 * f) * dr)
        pt += 0.00042 * sin(2 * omega * dr)
        pt += 0.00038 * sin((mprime - 2 * m) * dr)
        pt -= 0.00024 * sin((2 * mprime - 2 * f) * dr)

        jde += pt
        return floor(jde + 0.5 + timeZone / 24.0).toInt()
    }

    private fun getSunLongitude(jdn: Int, timeZone: Double): Double {
        val t = (jdn - 0.5 - timeZone / 24.0 - 2451545.0) / 36525.0
        val t2 = t * t
        val dr = Math.PI / 180
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val c = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(m * dr) +
                (0.019993 - 0.000101 * t) * sin(2 * m * dr) +
                0.000290 * sin(3 * m * dr)
        var theta = (l0 + c) % 360.0
        if (theta < 0) theta += 360.0
        return floor(theta / 30.0)
    }

    /**
     * Convert Solar Date to Lunar Date
     */
    fun convertSolar2Lunar(dd: Int, mm: Int, yy: Int): LunarDate {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.0769986) / 29.530588853).toInt()
        var monthStart = getNewMoonDay(k + 1, TIMEZONE)
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, TIMEZONE)
        }
        val a11 = getSunLongitude(monthStart, TIMEZONE).toInt()
        val b11 = a11
        
        val lunarDay = dayNumber - monthStart + 1
        val diff = ((monthStart - 2415021.0769986) / 29.530588853).toInt()
        
        // Approximate month and year
        var lunarMonth = (diff % 12) + 1
        if (lunarMonth <= 0) lunarMonth += 12
        var lunarYear = yy
        if (mm < 3 && lunarMonth > 10) lunarYear--

        val dayCanChi = "${CAN[(dayNumber + 9) % 10]} ${CHI[(dayNumber + 1) % 12]}"
        val yearCanChi = "${CAN[(lunarYear + 6) % 10]} ${CHI[(lunarYear + 8) % 12]}"
        val monthCanChi = "${CAN[(lunarYear * 12 + lunarMonth + 3) % 10]} ${CHI[(lunarMonth + 1) % 12]}"

        return LunarDate(
            day = lunarDay.coerceIn(1, 30),
            month = lunarMonth.coerceIn(1, 12),
            year = lunarYear,
            isLeapMonth = false,
            dayCanChi = dayCanChi,
            monthCanChi = monthCanChi,
            yearCanChi = yearCanChi
        )
    }

    /**
     * Alias for convertSolar2Lunar
     */
    fun convertSolarToLunar(dd: Int, mm: Int, yy: Int): LunarDate = convertSolar2Lunar(dd, mm, yy)

    /**
     * Get current lunar date for today
     */
    fun getTodayLunarDate(): LunarDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"))
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)
        return convertSolar2Lunar(d, m, y)
    }

    /**
     * Formatted string of today's lunar date
     */
    fun getTodayLunarString(): String {
        val lunar = getTodayLunarDate()
        val special = when (lunar.day) {
            1 -> " (Mùng 1)"
            15 -> " (Rằm)"
            else -> ""
        }
        return "${lunar.day}/${lunar.month} ÂL${special} - Năm ${lunar.yearCanChi}"
    }

    /**
     * Find next solar trigger timestamp in millis for a specific lunar day / month alarm
     * e.g. lunarDay = 1 (Mùng 1), 15 (Rằm), or custom
     */
    fun calculateNextLunarTrigger(targetLunarDay: Int, targetLunarMonth: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        val now = System.currentTimeMillis()

        // Check the next 45 days to find matching lunar day
        for (i in 0..45) {
            val checkCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"))
            checkCal.add(Calendar.DAY_OF_MONTH, i)
            checkCal.set(Calendar.HOUR_OF_DAY, hour)
            checkCal.set(Calendar.MINUTE, minute)
            checkCal.set(Calendar.SECOND, 0)
            checkCal.set(Calendar.MILLISECOND, 0)

            val d = checkCal.get(Calendar.DAY_OF_MONTH)
            val m = checkCal.get(Calendar.MONTH) + 1
            val y = checkCal.get(Calendar.YEAR)

            val lunar = convertSolar2Lunar(d, m, y)
            val dayMatches = (lunar.day == targetLunarDay)
            val monthMatches = (targetLunarMonth == 0 || lunar.month == targetLunarMonth)

            if (dayMatches && monthMatches && checkCal.timeInMillis > now) {
                return checkCal.timeInMillis
            }
        }

        // Fallback to tomorrow same time if not found
        val fallback = Calendar.getInstance()
        fallback.add(Calendar.DAY_OF_MONTH, 1)
        fallback.set(Calendar.HOUR_OF_DAY, hour)
        fallback.set(Calendar.MINUTE, minute)
        fallback.set(Calendar.SECOND, 0)
        return fallback.timeInMillis
    }
}
