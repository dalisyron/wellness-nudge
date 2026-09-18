package com.mimik.wellnessnudge.snapshots

import android.view.View
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.preview.PreviewData
import com.mimik.wellnessnudge.ui.theme.Manrope
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/** Pixel 9 Pro XL: 1008 x 2244 px at 360 dpi, i.e. 448 x 997 dp. */
val Pixel9ProXL: DeviceConfig = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1008,
    screenHeight = 2244,
    xdpi = 360,
    ydpi = 360,
    density = Density.create(360),
)

/**
 * The Paparazzi rule every snapshot test uses. Run Gradle with `-PsnapshotFullRes=true` to
 * record at device resolution while reviewing; commit the default, smaller images.
 */
fun wellnessPaparazzi(deviceConfig: DeviceConfig = Pixel9ProXL) = Paparazzi(
    deviceConfig = deviceConfig,
    theme = "android:Theme.Material.NoActionBar",
    maxPercentDifference = 0.1,
    useDeviceResolution = System.getProperty("snapshot.fullRes") == "true",
)

/** Records [content] once per theme, as `<name>_dark` and `<name>_light`. */
fun Paparazzi.snapshotThemes(name: String, content: @Composable () -> Unit) {
    snapshot("${name}_dark") { WellnessSnapshot(darkTheme = true, content = content) }
    snapshot("${name}_light") { WellnessSnapshot(darkTheme = false, content = content) }
}

/**
 * The frame around every snapshot: inspection mode (orbs and shimmers hold a still pose),
 * the preview clock (Thursday, Sep 17, 9:41 AM) and the theme over the Daybreak canvas.
 * Like the app on the phone it runs edge to edge under the Pixel's status bar (66 dp) and
 * gesture bar (24 dp): real insets reach the screen, and the bars are drawn on top.
 */
@Composable
fun WellnessSnapshot(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalWellnessClock provides PreviewData.clock,
    ) {
        WellnessTheme(darkTheme = darkTheme) {
            PixelSystemBarInsets()
            Box(Modifier.fillMaxSize()) {
                DaybreakBackground { content() }
                SystemBars()
            }
        }
    }
}

private val StatusBarHeight = 66.dp
private val GestureBarHeight = 24.dp

/**
 * Layoutlib reports no system bars. The host view swaps whatever insets reach it for the
 * Pixel's before passing them to Compose, which also covers the zero insets layoutlib
 * dispatches when Compose starts listening.
 */
@Composable
private fun PixelSystemBarInsets() {
    val host = LocalView.current.parent as View
    val density = LocalDensity.current
    SideEffect {
        val insets = with(density) {
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, StatusBarHeight.roundToPx(), 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, GestureBarHeight.roundToPx()))
                .build()
        }
        ViewCompat.setOnApplyWindowInsetsListener(host) { _, _ -> insets }
        ViewCompat.dispatchApplyWindowInsets(host, insets)
    }
}

/** A quiet stand-in for the Pixel status bar (demo-mode 9:41) and the gesture handle. */
@Composable
private fun SystemBars() {
    val tint = WellnessTheme.colors.textPrimary
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(StatusBarHeight)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "9:41",
                style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = tint,
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Wifi, null, Modifier.size(17.dp), tint = tint)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.SignalCellularAlt, null, Modifier.size(16.dp), tint = tint)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.BatteryFull,
                null,
                Modifier
                    .size(18.dp)
                    .rotate(90f),
                tint = tint,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .height(GestureBarHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 108.dp, height = 4.dp)
                    .background(tint.copy(alpha = 0.5f), CircleShape),
            )
        }
    }
}
