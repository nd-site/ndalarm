package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkyPrimaryContainer
import com.example.ui.theme.SkySecondary
import com.example.util.LunarCalendarHelper
import java.util.Calendar

data class CalendarDayItem(
    val solarDay: Int,
    val solarMonth: Int,
    val solarYear: Int,
    val lunarDay: Int,
    val lunarMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSpecialLunar: Boolean // 1 (Sóc) or 15 (Vọng)
)

@Composable
fun LunarCalendarScreen(
    onSetLunarAlarm: (lunarDay: Int, lunarMonth: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayCal = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var selectedDayItem by remember { mutableStateOf<CalendarDayItem?>(null) }

    // Generate days for grid
    val daysInMonth = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1: Sunday, 2: Monday...
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<CalendarDayItem>()

        // Padding before month start (Monday as start)
        val offset = (firstDayOfWeek + 5) % 7
        for (i in 0 until offset) {
            list.add(
                CalendarDayItem(
                    solarDay = 0,
                    solarMonth = selectedMonth,
                    solarYear = selectedYear,
                    lunarDay = 0,
                    lunarMonth = 0,
                    isCurrentMonth = false,
                    isToday = false,
                    isSpecialLunar = false
                )
            )
        }

        // Days of current month
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayYear = todayCal.get(Calendar.YEAR)

        for (d in 1..maxDays) {
            val lunar = LunarCalendarHelper.convertSolarToLunar(d, selectedMonth + 1, selectedYear)
            val isToday = d == todayDay && selectedMonth == todayMonth && selectedYear == todayYear
            val isSpecial = lunar.day == 1 || lunar.day == 15

            val item = CalendarDayItem(
                solarDay = d,
                solarMonth = selectedMonth + 1,
                solarYear = selectedYear,
                lunarDay = lunar.day,
                lunarMonth = lunar.month,
                isCurrentMonth = true,
                isToday = isToday,
                isSpecialLunar = isSpecial
            )
            list.add(item)
            if (isToday && selectedDayItem == null) {
                selectedDayItem = item
            }
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Month Navigation Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedMonth == 0) {
                            selectedMonth = 11
                            selectedYear--
                        } else {
                            selectedMonth--
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước", tint = SkyPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tháng ${selectedMonth + 1}, $selectedYear",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lịch Âm - Dương song hành",
                        fontSize = 12.sp,
                        color = SkyPrimary
                    )
                }

                IconButton(
                    onClick = {
                        if (selectedMonth == 11) {
                            selectedMonth = 0
                            selectedYear++
                        } else {
                            selectedMonth++
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau", tint = SkyPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEachIndexed { index, day ->
                Text(
                    text = day,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (index >= 5) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Calendar Grid
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                val rows = daysInMonth.chunked(7)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        row.forEach { dayItem ->
                            if (!dayItem.isCurrentMonth) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isSelected = selectedDayItem?.solarDay == dayItem.solarDay &&
                                        selectedDayItem?.solarMonth == dayItem.solarMonth
                                val isMung1 = dayItem.lunarDay == 1
                                val isRam15 = dayItem.lunarDay == 15

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isSelected -> SkyPrimary
                                                dayItem.isToday -> SkyPrimaryContainer
                                                isMung1 || isRam15 -> Color(0xFFEFF6FF)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isMung1 || isRam15) 1.5.dp else 0.dp,
                                            color = if (isMung1 || isRam15) Color(0xFF3B82F6) else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedDayItem = dayItem },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${dayItem.solarDay}",
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || dayItem.isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                dayItem.isToday -> SkyPrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = if (dayItem.lunarDay == 1) "1/${dayItem.lunarMonth}" else "${dayItem.lunarDay}",
                                            fontSize = 9.sp,
                                            fontWeight = if (isMung1 || isRam15) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White.copy(alpha = 0.9f)
                                                isMung1 || isRam15 -> Color(0xFF1D4ED8)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Date Detail & Quick Alarm Setup Card
        selectedDayItem?.let { day ->
            val lunarDesc = when (day.lunarDay) {
                1 -> "MÙNG 1 (NGÀY SÓC) — Ngày đầu tháng Âm lịch"
                15 -> "RẰM 15 (NGÀY VỌNG) — Trăng tròn viên mãn"
                else -> "Ngày ${day.lunarDay} tháng ${day.lunarMonth} Âm lịch"
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dương lịch: ${day.solarDay}/${day.solarMonth}/${day.solarYear}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🌙 $lunarDesc",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                        }

                        if (day.isSpecialLunar) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (day.lunarDay == 1) "Mùng 1" else "Rằm 15",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onSetLunarAlarm(day.lunarDay, day.lunarMonth)
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tạo Báo Thức cho ngày ${day.lunarDay}/${day.lunarMonth} ÂL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
