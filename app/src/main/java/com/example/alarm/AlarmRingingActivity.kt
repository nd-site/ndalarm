package com.example.alarm

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NDAlarmApp
import com.example.data.model.RingingHistoryEntity
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkyPrimaryContainer
import com.example.ui.theme.SkySecondary
import com.example.util.LunarCalendarHelper
import com.example.util.SecurityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Long = 0L
    private var alarmLabel: String = "Báo thức ND"
    private var secretToken50: String = ""
    private var emergencyPin8: String = ""
    private var clockId: String = "ND-CL01"
    private var ndid: String = "ND-USER"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure full screen display over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Read intent extras
        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
        alarmLabel = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Báo thức ND"
        secretToken50 = intent.getStringExtra(AlarmScheduler.EXTRA_TOKEN_50) ?: SecurityHelper.generateSecretToken50()
        emergencyPin8 = intent.getStringExtra(AlarmScheduler.EXTRA_PIN_8) ?: SecurityHelper.generateEmergencyPin8()
        clockId = intent.getStringExtra(AlarmScheduler.EXTRA_CLOCK_ID) ?: "ND-CL01"
        ndid = intent.getStringExtra(AlarmScheduler.EXTRA_NDID) ?: "ND-USER"

        // Anti-Cheat: Intercept back button so users cannot bypass alarm
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@AlarmRingingActivity, "Vui lòng nhập mật khẩu 50 ký tự hoặc tắt qua Web Portal!", Toast.LENGTH_SHORT).show()
            }
        })

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingScreen(
                        alarmId = alarmId,
                        alarmLabel = alarmLabel,
                        secretToken50 = secretToken50,
                        emergencyPin8 = emergencyPin8,
                        clockId = clockId,
                        ndid = ndid,
                        onDismissAlarm = { unlockMethod ->
                            dismissAlarm(unlockMethod)
                        }
                    )
                }
            }
        }
    }

    private fun dismissAlarm(unlockMethod: String) {
        // Stop foreground sound service
        AlarmRingtoneService.stopRinging(this)

        // Record history and update status in database
        val app = application as? NDAlarmApp
        val db = app?.database
        if (db != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                db.alarmDao().updateStatus(alarmId, "unlocked")
                db.ringingHistoryDao().insertHistory(
                    RingingHistoryEntity(
                        alarmId = alarmId,
                        alarmLabel = alarmLabel,
                        triggeredTime = System.currentTimeMillis() - 30000L,
                        unlockedTime = System.currentTimeMillis(),
                        unlockMethod = unlockMethod,
                        unlockedByNdid = ndid
                    )
                )
            }
        }

        Toast.makeText(this, "Đã tắt báo thức thành công ($unlockMethod)!", Toast.LENGTH_LONG).show()
        finish()
    }
}

@Composable
fun AlarmRingingScreen(
    alarmId: Long,
    alarmLabel: String,
    secretToken50: String,
    emergencyPin8: String,
    clockId: String,
    ndid: String,
    onDismissAlarm: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputToken by remember { mutableStateOf("") }
    var inputPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentTab by remember { mutableStateOf(0) } // 0: 50-char Token / Autofill, 1: 8-digit PIN, 2: Web Sync, 3: Math Challenge

    // Math challenge state
    val mathA = remember { (12..35).random() }
    val mathB = remember { (4..9).random() }
    val mathC = remember { (15..45).random() }
    val mathAnswer = remember { mathA * mathB + mathC }
    var inputMath by remember { mutableStateOf("") }

    // Live clock pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }
    var currentTimeString by remember { mutableStateOf(timeFormat.format(Date())) }
    val lunarString = remember { LunarCalendarHelper.getTodayLunarString() }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = timeFormat.format(Date())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Pulsing Ringing Icon & Badge
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .size(72.dp)
                .clip(CircleShape)
                .background(SkyPrimaryContainer)
                .border(2.dp, SkyPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Báo thức đang reo",
                tint = SkyPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pill status tag
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFEE2E2),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentError,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BÁO THỨC ĐANG REO (ANTI-CHEAT KHÓA)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentError
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large time display
        Text(
            text = currentTimeString,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1).sp
        )

        Text(
            text = alarmLabel,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SkyPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Solar + Lunar Date
        Text(
            text = dateFormat.format(Date()),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "🌙 $lunarString",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0369A1)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Device Clock ID & NDID Pill Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SkyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mã đồng hồ: $clockId",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SkyPrimaryContainer
                ) {
                    Text(
                        text = "NDID: $ndid",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unlock Method Switcher Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Token 50", "Mã PIN 8", "Web Portal", "Toán học").forEachIndexed { index, label ->
                val isSelected = currentTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) SkyPrimary else Color.Transparent)
                        .clickable { currentTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message if invalid
        AnimatedVisibility(visible = errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = AccentError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // TAB 0: 50-Character Secret Token (Autofill / Keychain / Copy & Verify)
        if (currentTab == 0) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = SkyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mật khẩu 50 ký tự (Autofill/Keychain)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Nhập hoặc tự động điền mật khẩu 50 ký tự bảo mật được tạo cho thiết bị này để tắt chuông:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = {
                            inputToken = it
                            errorMessage = null
                        },
                        placeholder = { Text("Dán hoặc gõ mật khẩu 50 ký tự...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("token_input_field"),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        inputToken = clip.getItemAt(0).text.toString()
                                        Toast.makeText(context, "Đã dán từ bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Dán", tint = SkyPrimary)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick simulation buttons for demo & testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Simulate Android Autofill from ND Keychain
                                inputToken = secretToken50
                                Toast.makeText(context, "ND Keychain đã tự động điền mật khẩu 50 ký tự!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("autofill_button"),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Autofill Keychain", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("ND Token 50", secretToken50)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Đã sao chép mật khẩu 50 ký tự!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_token_button"),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sao chép mẫu", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (SecurityHelper.verifyToken(inputToken.trim(), secretToken50.trim())) {
                                onDismissAlarm("TOKEN_50")
                            } else {
                                errorMessage = "Mật khẩu 50 ký tự không chính xác! Vui lòng kiểm tra lại."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_token_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xác thực & Tắt Báo Thức", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TAB 1: 8-Digit Emergency PIN Challenge (Cross-device)
        else if (currentTab == 1) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = SkyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mã PIN khẩn cấp 8 số",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Mã PIN 8 số dùng để tắt chéo từ thiết bị khác hoặc Web Portal:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // PIN Display box
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SkyPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MÃ PIN ĐANG HIỂN THỊ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = emergencyPin8,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 6.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SkyPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = {
                            inputPin = it
                            errorMessage = null
                        },
                        placeholder = { Text("Nhập 8 chữ số PIN...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_input_field"),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (SecurityHelper.verifyPin(inputPin, emergencyPin8)) {
                                onDismissAlarm("PIN_8")
                            } else {
                                errorMessage = "Mã PIN 8 số không chính xác!"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_pin_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xác thực PIN & Tắt Chuông", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TAB 2: Web Portal Remote Sync
        else if (currentTab == 2) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = SkyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đồng bộ tắt qua Web Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Mở Web Portal (ndsite.web.app / ND Alarm Web) trên máy tính hoặc điện thoại khác để tắt báo thức từ xa bằng Credential Management API:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("1. Nhập Clock ID: $clockId", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("2. Mật khẩu 50 ký tự tự động Autofill qua browser", fontSize = 12.sp)
                            Text("3. Nếu khác NDID: Nhập mã PIN 8 số ($emergencyPin8)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulate remote Web Portal unlock trigger
                    Button(
                        onClick = {
                            onDismissAlarm("WEB_PORTAL_REMOTE")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("simulate_web_unlock_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nhận tín hiệu Web Portal Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TAB 3: Math Challenge (Anti-sleep wake-up)
        else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = SkyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thử thách Toán Tỉnh Táo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Giải phép tính dưới đây để kích hoạt não bộ và tắt chuông:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SkyPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$mathA × $mathB + $mathC = ?",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = SkyPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputMath,
                        onValueChange = {
                            inputMath = it
                            errorMessage = null
                        },
                        placeholder = { Text("Nhập kết quả phép tính...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("math_input_field"),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (inputMath.trim().toIntOrNull() == mathAnswer) {
                                onDismissAlarm("MATH_CHALLENGE")
                            } else {
                                errorMessage = "Kết quả chưa đúng! Hãy tính cẩn thận lại."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_math_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Giải đúng & Tắt Báo Thức", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
