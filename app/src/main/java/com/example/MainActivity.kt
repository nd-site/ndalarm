package com.example

import android.content.Context
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleAuthHelper
import com.example.data.model.AlarmEntity
import com.example.ui.components.AddEditAlarmDialog
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.HistorySettingsScreen
import com.example.ui.screens.LunarCalendarScreen
import com.example.ui.screens.SignInScreen
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
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val credentialManager = remember { CredentialManager.create(context) }

                val alarms by viewModel.alarms.collectAsState()
                val trustedNdids by viewModel.trustedNdids.collectAsState()
                val historyList by viewModel.history.collectAsState()
                val clockId by viewModel.clockId.collectAsState()
                val ndid by viewModel.ndid.collectAsState()
                val selectedTab by viewModel.selectedTab.collectAsState()
                val showAddDialog by viewModel.showAddDialog.collectAsState()
                val editingAlarm by viewModel.editingAlarm.collectAsState()

                val currentUser by viewModel.currentUser.collectAsState()
                val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
                val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()

                var isOfflineMode by remember { mutableStateOf(false) }

                // Attempt silent auto-sign-in on startup
                LaunchedEffect(Unit) {
                    GoogleAuthHelper.attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = { viewModel.syncWithCloud() },
                        onUnauthenticated = { /* Keep current state */ },
                        scope = coroutineScope
                    )
                }

                // Auth Gate: Require Google Sign-In unless user explicitly opts into offline mode
                if (currentUser == null && !isOfflineMode) {
                    SignInScreen(
                        onAuthSuccess = {
                            viewModel.syncWithCloud()
                        },
                        onContinueOffline = {
                            isOfflineMode = true
                        }
                    )
                } else {
                    val navItems = listOf(
                        NavTabItem("Báo thức", Icons.Filled.Alarm, Icons.Outlined.Alarm, "tab_alarms"),
                        NavTabItem("Lịch Âm", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendar"),
                        NavTabItem("Web Sync", Icons.Filled.Language, Icons.Outlined.Language, "tab_sync"),
                        NavTabItem("Lịch sử", Icons.Filled.History, Icons.Outlined.History, "tab_history")
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            // Top Account & Cloud Status Bar
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentUser != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                                            listOf(SkyPrimary, SkySecondary)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (currentUser?.displayName?.take(1)
                                                        ?: currentUser?.email?.take(1)
                                                        ?: "U").uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = currentUser?.displayName ?: currentUser?.email ?: "Google Account",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = SkyPrimaryContainer,
                                                        modifier = Modifier.clickable {
                                                            val webIntent = android.content.Intent(
                                                                android.content.Intent.ACTION_VIEW,
                                                                android.net.Uri.parse("https://ndsite.web.app/login")
                                                            )
                                                            context.startActivity(webIntent)
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(com.example.ui.theme.AccentSuccess)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "ndsite.web.app",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SkyPrimary
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = cloudSyncStatus,
                                                    fontSize = 11.sp,
                                                    color = if (isCloudSyncing) SkyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.syncWithCloud() },
                                                enabled = !isCloudSyncing
                                            ) {
                                                if (isCloudSyncing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = SkyPrimary
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Sync,
                                                        contentDescription = "Đồng bộ",
                                                        tint = SkyPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    GoogleAuthHelper.signOut(
                                                        context = context,
                                                        credentialManager = credentialManager,
                                                        onSignOutComplete = {
                                                            isOfflineMode = false
                                                        },
                                                        scope = coroutineScope
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                    contentDescription = "Đăng xuất",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Offline banner prompt to login
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = "OFFLINE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Chế độ cục bộ trên máy",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = SkyPrimaryContainer,
                                                modifier = Modifier.clickable { isOfflineMode = false }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudSync,
                                                        contentDescription = null,
                                                        tint = SkyPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Đăng nhập ndsite",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SkyPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
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
}
