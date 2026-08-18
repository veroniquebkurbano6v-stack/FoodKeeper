package com.example.foodkeeper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 主题色（对应原应用 THEME 字典）
val BgColor = Color(0xFFFAF6EF)
val CardColor = Color(0xFFFFFEFB)
val SectionColor = Color(0xFFF5EFE4)
val PrimaryColor = Color(0xFF7C9473)
val PrimaryDarkColor = Color(0xFF5E7A56)
val PrimaryBgColor = Color(0xFFEDF1E8)
val AccentColor = Color(0xFFC97B5C)
val AccentBgColor = Color(0xFFF7E8DF)
val ColdColor = Color(0xFF6E8CA8)
val ColdBgColor = Color(0xFFE8EEF4)
val TextColor = Color(0xFF3D352E)
val Text2Color = Color(0xFF8B7E6F)
val Text3Color = Color(0xFFB5A99A)
val BorderColor = Color(0xFFECE4D6)
val FreshColor = Color(0xFF7C9473)
val FreshBgColor = Color(0xFFEDF1E8)
val WarnColor = Color(0xFFD4A55B)
val WarnBgColor = Color(0xFFFBF1DE)
val DangerColor = Color(0xFFC75D4D)
val DangerBgColor = Color(0xFFFAE6E2)
val ExpiredColor = Color(0xFF9A8E80)
val ExpiredBgColor = Color(0xFFEDEAE5)

// 八大分类配色
val CatColors = mapOf(
    1 to (Color(0xFF7C9473) to Color(0xFFEDF1E8)),
    2 to (Color(0xFFC97B6E) to Color(0xFFF5E6E2)),
    3 to (Color(0xFF6E8CA8) to Color(0xFFE8EEF4)),
    4 to (Color(0xFFA88B6A) to Color(0xFFF0E9DC)),
    5 to (Color(0xFF8BA0B0) to Color(0xFFE9EEF1)),
    6 to (Color(0xFFB5765A) to Color(0xFFF0E2DC)),
    7 to (Color(0xFFC75D4D) to Color(0xFFF5E3DF)),
    8 to (Color(0xFF9CB088) to Color(0xFFEDF0E4)),
)

// 菜谱分类配色
val RecipeCatColors = mapOf(
    0 to (Color(0xFF7C9473) to Color(0xFFEDF1E8)),  // 快手懒人餐
    1 to (Color(0xFFC97B5C) to Color(0xFFF7E8DF)),  // 家常正餐
    2 to (Color(0xFF6E8CA8) to Color(0xFFE8EEF4)),  // 减脂轻食餐
)
val RecipeCatLabels = mapOf(
    0 to "快手懒人餐",
    1 to "家常正餐",
    2 to "减脂轻食餐",
)

private val FoodkeeperColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = PrimaryBgColor,
    onPrimaryContainer = PrimaryDarkColor,
    secondary = AccentColor,
    onSecondary = Color.White,
    secondaryContainer = AccentBgColor,
    background = BgColor,
    onBackground = TextColor,
    surface = CardColor,
    onSurface = TextColor,
    surfaceVariant = SectionColor,
    onSurfaceVariant = Text2Color,
    outline = BorderColor,
    outlineVariant = Text3Color,
    error = DangerColor,
    onError = Color.White,
)

@Composable
fun FoodkeeperTheme(content: @Composable () -> Unit) {
    // 始终使用浅色主题（与原应用一致，不支持深色模式）
    MaterialTheme(
        colorScheme = FoodkeeperColorScheme,
        typography = FoodkeeperTypography,
        content = content
    )
}

// 排版（与原应用字号一致）
val FoodkeeperTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextColor),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextColor),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextColor),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextColor),
    bodyLarge = TextStyle(fontSize = 15.sp, color = TextColor),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Text2Color),
    bodySmall = TextStyle(fontSize = 12.sp, color = Text3Color),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColor),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Text2Color),
    labelSmall = TextStyle(fontSize = 11.sp, color = Text3Color),
)
