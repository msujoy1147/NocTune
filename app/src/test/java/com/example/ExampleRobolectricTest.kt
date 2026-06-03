package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Noc Tune", appName)
  }

  @Test
  fun `test UI navigation and dark mode toggle`() {
    // 1. Verify Home screen and toggle dark mode
    composeTestRule.onNodeWithTag("night_mode_toggle_btn").assertExists().performClick()

    // 2. Click library tab and toggle dark mode
    composeTestRule.onNodeWithTag("nav_library").assertExists().performClick()
    composeTestRule.onNodeWithTag("night_mode_toggle_library").assertExists().performClick()

    // 3. Click search tab and toggle dark mode
    composeTestRule.onNodeWithTag("nav_search").assertExists().performClick()
    composeTestRule.onNodeWithTag("night_mode_toggle_search").assertExists().performClick()

    // 4. Go back to Home
    composeTestRule.onNodeWithTag("nav_home").assertExists().performClick()
  }
}
