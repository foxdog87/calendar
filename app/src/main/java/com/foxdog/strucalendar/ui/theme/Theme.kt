package com.foxdog.strucalendar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// アプリのアクセントカラー（追加ボタン・選択日・今日の丸など）
private val AccentBlueLight = Color(0xFF1A73E8)
private val AccentBlueDark = Color(0xFF8AB4F8)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlueLight,
    onPrimary = Color.White,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFD8D8D8),
    outlineVariant = Color(0xFFBDBDBD),

    // 期限切れ/エラー系はアプリの赤に合わせて上書き
    error = Color(0xFFD93025),
    onError = Color.White,
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = Color(0xFFC5221F),
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlueDark,
    onPrimary = Color(0xFF00315C),
    secondary = PurpleGrey80,
    tertiary = Pink80,

    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF4A4A4A),

    // ライト同様、期限切れ/エラー系を明示指定
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF4A2521),
    onErrorContainer = Color(0xFFF9C6C2),
)

data class CalendarExtendedColors(
    val sunday: Color,
    val selectedDateBackground: Color,
    val templateAccent: Color,
    val templateAccentContainer: Color,
    val success: Color,
    val successContainer: Color, // 完了バッジの背景
    val onSuccessContainer: Color, // 完了バッジの文字
)

private val LightExtendedColors = CalendarExtendedColors(
    sunday = Color(0xFFD93025),
    selectedDateBackground = Color(0xFFE8F0FE),
    templateAccent = Color(0xFF6200EE),
    templateAccentContainer = Color(0xFFF1ECFB),
    success = Color(0xFF34A853),
    successContainer = Color(0xFFE6F4EA),
    onSuccessContainer = Color(0xFF137333),
)

private val DarkExtendedColors = CalendarExtendedColors(
    sunday = Color(0xFFF28B82),
    selectedDateBackground = Color(0xFF2A3B55),
    templateAccent = Color(0xFFCFA9FF),
    templateAccentContainer = Color(0xFF352A47),
    success = Color(0xFF81C995),
    successContainer = Color(0xFF1E3B2A),
    onSuccessContainer = Color(0xFFA8DAB5),
)

private val LocalCalendarExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** MaterialTheme.calendarColors.sunday のように呼び出す */
val MaterialTheme.calendarColors: CalendarExtendedColors
    @Composable
    get() = LocalCalendarExtendedColors.current

@Composable
fun CalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // dynamicColorがONだと端末の壁紙色でsunday/selectedDateBackgroundまで
    //   変わってしまい違和感が出るため、拡張カラーはdynamicColorの影響を受けず
    //   ダーク/ライトの固定値のみで切り替える
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalCalendarExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}