package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.ui.components.AddEditAlarmDialog
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.HistorySettingsScreen
import com.example.ui.screens.LunarCalendarScreen
import com.example.ui.screens.WebPortalSyncScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkyPrimaryContainer
import com.example.ui.theme.SkySecondary
import com.example.ui.viewmodel.AlarmViewModel

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val alarms by viewModel.alarms.collectAsState()
                val trustedNdids by viewModel.trustedNdids.collectAsState()
                val historyList by viewModel.history.collectAsState()
                val clockId by viewModel.clockId.collectAsState()
                val ndid by viewModel.ndid.collectAsState()
                val selectedTab by viewModel.selectedTab.collectAsState()
                val showAddDialog by viewModel.showAddDialog.collectAsState()
                val editingAlarm by viewModel.editingAlarm.collectAsState()

                val navItems = listOf(
                    NavTabItem("Báo thức", Icons.Filled.Alarm, Icons.Outlined.Alarm, "tab_alarms"),
                    NavTabItem("Lịch Âm", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendar"),
                    NavTabItem("Web Sync", Icons.Filled.Language, Icons.Outlined.Language, "tab_sync"),
                    NavTabItem("Lịch sử", Icons.Filled.History, Icons.Outlined.History, "tab_history")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Surface(
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .height(68.dp)
                            ) {
                                navItems.forEachIndexed { index, item ->
                                    val isSelected = selectedTab == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { viewModel.selectTab(index) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = SkyPrimary,
                                            selectedTextColor = SkyPrimary,
                                            indicatorColor = SkyPrimaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(item.tag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                0 -> AlarmsScreen(
                                    alarms = alarms,
                                    clockId = clockId,
                                    ndid = ndid,
                                    onToggleAlarm = { viewModel.toggleAlarm(it) },
                                    onEditAlarm = { viewModel.openAddAlarmDialog(it) },
                                    onDeleteAlarm = { viewModel.deleteAlarm(it) },
                                    onTestAlarm = { viewModel.triggerTestRinging(it) },
                                    onAddNewAlarm = { viewModel.openAddAlarmDialog(null) }
                                )
                                1 -> LunarCalendarScreen(
                                    onSetLunarAlarm = { lunarDay, lunarMonth ->
                                        viewModel.openAddAlarmDialog(
                                            AlarmEntity(
                                                label = "Báo thức Âm lịch (Ngày $lunarDay/$lunarMonth)",
                                                hour = 6,
                                                minute = 30,
                                                isLunarCalendar = true,
                                                repeatRule = "LUNAR_CUSTOM",
                                                lunarDay = lunarDay,
                                                lunarMonth = lunarMonth,
                                                clockId = clockId,
                                                ndid = ndid
                                            )
                                        )
                                    }
                                )
                                2 -> WebPortalSyncScreen(
                                    clockId = clockId,
                                    ndid = ndid,
                                    trustedNdids = trustedNdids,
                                    onAddTrustedNdid = { id, lbl -> viewModel.addTrustedNdid(id, lbl) },
                                    onRemoveTrustedNdid = { viewModel.removeTrustedNdid(it) },
                                    onRegenerateClockId = { viewModel.regenerateClockId() }
                                )
                                3 -> HistorySettingsScreen(
                                    historyList = historyList,
                                    onClearHistory = { viewModel.clearHistory() },
                                    onTestTriggerAlarm = {
                                        if (alarms.isNotEmpty()) {
                                            viewModel.triggerTestRinging(alarms.first())
                                        } else {
                                            val dummy = AlarmEntity(
                                                label = "Báo thức thử nghiệm",
                                                hour = 7,
                                                minute = 0,
                                                clockId = clockId,
                                                ndid = ndid
                                            )
                                            viewModel.triggerTestRinging(dummy)
                                        }
                                    }
                                )
                            }
                        }

                        // Add / Edit Dialog
                        if (showAddDialog) {
                            AddEditAlarmDialog(
                                alarmToEdit = editingAlarm,
                                onDismiss = { viewModel.closeAddAlarmDialog() },
                                onSave = { label, hour, minute, isLunar, repeatRule, lDay, lMonth, ringtone, vibrate, antiCheat ->
                                    viewModel.saveAlarm(
                                        label = label,
                                        hour = hour,
                                        minute = minute,
                                        isLunar = isLunar,
                                        repeatRule = repeatRule,
                                        lunarDay = lDay,
                                        lunarMonth = lMonth,
                                        ringtoneType = ringtone,
                                        vibrate = vibrate,
                                        antiCheatMode = antiCheat
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
