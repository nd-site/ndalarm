package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AlarmEntity
import com.example.ui.components.AlarmCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun alarm_card_screenshot() {
    val sampleAlarm = AlarmEntity(
      id = 1L,
      label = "Thức dậy ND Labs",
      hour = 7,
      minute = 0,
      isEnabled = true,
      isLunarCalendar = true,
      repeatRule = "LUNAR_1_15",
      lunarDay = 1,
      ringtoneType = "gentle_breeze",
      clockId = "ND-CL01",
      ndid = "ND-MASTER"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        AlarmCard(
          alarm = sampleAlarm,
          onToggle = {},
          onEdit = {},
          onDelete = {},
          onTestRinging = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
