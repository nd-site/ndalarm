package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AlarmEntity
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkyPrimaryContainer
import com.example.ui.theme.SkySecondary
import java.util.Locale

@Composable
fun AddEditAlarmDialog(
    alarmToEdit: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (
        label: String,
        hour: Int,
        minute: Int,
        isLunar: Boolean,
        repeatRule: String,
        lunarDay: Int,
        lunarMonth: Int,
        ringtoneType: String,
        vibrate: Boolean,
        antiCheatMode: String
    ) -> Unit
) {
    var hour by remember { mutableIntStateOf(alarmToEdit?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarmToEdit?.minute ?: 0) }
    var label by remember { mutableStateOf(alarmToEdit?.label ?: "Báo thức ND") }
    var isLunarCalendar by remember { mutableStateOf(alarmToEdit?.isLunarCalendar ?: false) }
    var repeatRule by remember { mutableStateOf(alarmToEdit?.repeatRule ?: "DAILY") }
    var lunarDay by remember { mutableIntStateOf(alarmToEdit?.lunarDay ?: 1) }
    var lunarMonth by remember { mutableIntStateOf(alarmToEdit?.lunarMonth ?: 0) }
    var ringtoneType by remember { mutableStateOf(alarmToEdit?.ringtoneType ?: "gentle_breeze") }
    var vibrate by remember { mutableStateOf(alarmToEdit?.vibrate ?: true) }
    var antiCheatMode by remember { mutableStateOf(alarmToEdit?.antiCheatMode ?: "TOKEN_50") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (alarmToEdit == null) "Thêm Báo Thức Mới" else "Chỉnh Sửa Báo Thức",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Spinner Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SkyPrimaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { hour = (hour + 1) % 24 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+1 Giờ", tint = SkyPrimary)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", hour),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = SkyPrimary
                            )
                            IconButton(
                                onClick = { hour = if (hour > 0) hour - 1 else 23 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-1 Giờ", tint = SkyPrimary)
                            }
                        }

                        Text(
                            text = ":",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = SkyPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Minute Column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { minute = (minute + 5) % 60 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+5 Phút", tint = SkyPrimary)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", minute),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = SkyPrimary
                            )
                            IconButton(
                                onClick = { minute = if (minute >= 5) minute - 5 else 55 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-5 Phút", tint = SkyPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Label Input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Tên báo thức") },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("alarm_label_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Calendar Type Selector (Dương Lịch vs Âm Lịch)
                Text(
                    text = "Loại Lịch Báo Thức",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isLunarCalendar) SkyPrimary else Color.Transparent)
                            .clickable {
                                isLunarCalendar = false
                                repeatRule = "DAILY"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = if (!isLunarCalendar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lịch Dương",
                                fontSize = 13.sp,
                                fontWeight = if (!isLunarCalendar) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isLunarCalendar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isLunarCalendar) Color(0xFF2563EB) else Color.Transparent)
                            .clickable {
                                isLunarCalendar = true
                                repeatRule = "LUNAR_1_15"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = if (isLunarCalendar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lịch Âm (VN)",
                                fontSize = 13.sp,
                                fontWeight = if (isLunarCalendar) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLunarCalendar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Repeat / Frequency Rules based on Calendar
                if (!isLunarCalendar) {
                    Text(
                        text = "Lặp lại (Dương Lịch)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "DAILY" to "Hằng ngày",
                            "MON,TUE,WED,THU,FRI" to "Thứ 2 - 6",
                            "ONCE" to "Một lần"
                        ).forEach { (key, title) ->
                            val isSelected = repeatRule == key
                            OutlinedButton(
                                onClick = { repeatRule = key },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) SkyPrimaryContainer else Color.Transparent,
                                    contentColor = if (isSelected) SkyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SkyPrimary)) else ButtonDefaults.outlinedButtonBorder
                            ) {
                                Text(title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                } else {
                    // Lunar options
                    Text(
                        text = "Chu kỳ Âm Lịch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val is115 = repeatRule == "LUNAR_1_15"
                            OutlinedButton(
                                onClick = { repeatRule = "LUNAR_1_15" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (is115) Color(0xFFDBEAFE) else Color.Transparent,
                                    contentColor = if (is115) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Mùng 1 & Rằm 15", fontSize = 11.sp, fontWeight = if (is115) FontWeight.Bold else FontWeight.Normal)
                            }

                            val isMung1 = repeatRule == "LUNAR_FIXED" && lunarDay == 1
                            OutlinedButton(
                                onClick = {
                                    repeatRule = "LUNAR_FIXED"
                                    lunarDay = 1
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isMung1) Color(0xFFDBEAFE) else Color.Transparent,
                                    contentColor = if (isMung1) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Mùng 1 (Sóc)", fontSize = 11.sp, fontWeight = if (isMung1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isRam15 = repeatRule == "LUNAR_FIXED" && lunarDay == 15
                            OutlinedButton(
                                onClick = {
                                    repeatRule = "LUNAR_FIXED"
                                    lunarDay = 15
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isRam15) Color(0xFFDBEAFE) else Color.Transparent,
                                    contentColor = if (isRam15) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Rằm 15 (Vọng)", fontSize = 11.sp, fontWeight = if (isRam15) FontWeight.Bold else FontWeight.Normal)
                            }

                            val isCustom = repeatRule == "LUNAR_CUSTOM"
                            OutlinedButton(
                                onClick = {
                                    repeatRule = "LUNAR_CUSTOM"
                                    if (lunarDay == 1 || lunarDay == 15) lunarDay = 10
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCustom) Color(0xFFDBEAFE) else Color.Transparent,
                                    contentColor = if (isCustom) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Ngày Âm tùy chọn", fontSize = 11.sp, fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal)
                            }
                        }

                        if (repeatRule == "LUNAR_CUSTOM") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ngày Âm lịch (1-30):", fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (lunarDay > 1) lunarDay-- }) {
                                        Icon(Icons.Default.Remove, contentDescription = "-")
                                    }
                                    Text(
                                        text = "Ngày $lunarDay",
                                        fontWeight = FontWeight.Bold,
                                        color = SkyPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(onClick = { if (lunarDay < 30) lunarDay++ }) {
                                        Icon(Icons.Default.Add, contentDescription = "+")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ringtone sound selection
                Text(
                    text = "Giai Điệu Báo Thức",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "gentle_breeze" to "Gentle Breeze",
                        "morning_ocean" to "Morning Ocean",
                        "digital_pulse" to "Digital Pulse",
                        "system" to "Hệ thống"
                    ).forEach { (type, name) ->
                        val isSelected = ringtoneType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) SkyPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { ringtoneType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rung khi reo chuông",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SkyPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            onSave(
                                label,
                                hour,
                                minute,
                                isLunarCalendar,
                                repeatRule,
                                lunarDay,
                                lunarMonth,
                                ringtoneType,
                                vibrate,
                                antiCheatMode
                            )
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("save_alarm_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Text("Lưu Báo Thức", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
