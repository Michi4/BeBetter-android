package at.websters.bebetter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import at.websters.bebetter.BeBetterApp
import kotlinx.coroutines.launch

// Exact tokens from frontend/src/assets/main.css :root / html.light
object BeBetterTokens {
    // Dark (default, dark-first like web)
    val BgDark = Color(0xFF0B0C0F)
    val BgSoftDark = Color(0xFF101217)
    val CardDark = Color(0xFF14171D)
    val InkDark = Color(0xFFF2F4F8)
    val MutedDark = Color(0xFFA3ABB8)
    val FaintDark = Color(0xFF7C8494)
    val LineDark = Color(0xFF232833)
    val CardBorderDark = Color(0xFF262C37)
    val InputBgDark = Color(0xFF14171D)
    val InputBorderDark = Color(0xFF2C3440)
    val BtnSecBgDark = Color(0xFF1D2129)
    val BtnSecInkDark = Color(0xFFD7DBE3)
    val Accent = Color(0xFF34D399)        // --bb-accent / emerald-400
    val AccentStrong = Color(0xFF10B981)  // emerald-500
    val AccentBtn = Color(0xFF047857)     // emerald-700 (.btn bg)
    val AccentBtnHover = Color(0xFF059669)// emerald-600

    // Light (html.light)
    val BgLight = Color(0xFFFAF9F6)
    val CardLight = Color(0xFFFFFFFF)
    val InkLight = Color(0xFF1C1917)
    val MutedLight = Color(0xFF57534E)
    val FaintLight = Color(0xFF79746D)
    val LineLight = Color(0xFFE3DED6)
    val CardBorderLight = Color(0xFFE7E4DE)
    val InputBorderLight = Color(0xFFD3D0C9)
    val AccentLight = Color(0xFF047857)
    val AccentStrongLight = Color(0xFF065F46)
}

private val DarkScheme = darkColorScheme(
    primary = BeBetterTokens.AccentStrong,          // buttons (.btn emerald-700-ish, use strong for contrast)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF064E3B),           // emerald-500/20-ish
    onPrimaryContainer = BeBetterTokens.Accent,
    secondary = BeBetterTokens.MutedDark,
    background = BeBetterTokens.BgDark,
    onBackground = BeBetterTokens.InkDark,
    surface = BeBetterTokens.CardDark,
    onSurface = BeBetterTokens.InkDark,
    surfaceVariant = BeBetterTokens.BgSoftDark,
    onSurfaceVariant = BeBetterTokens.MutedDark,
    outline = BeBetterTokens.CardBorderDark,
    outlineVariant = BeBetterTokens.LineDark,
    error = Color(0xFFEF4444),
    surfaceContainer = BeBetterTokens.CardDark,
    surfaceContainerHigh = Color(0xFF1B1E26)
)

private val LightScheme = lightColorScheme(
    primary = BeBetterTokens.AccentLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = BeBetterTokens.AccentStrongLight,
    secondary = BeBetterTokens.MutedLight,
    background = BeBetterTokens.BgLight,
    onBackground = BeBetterTokens.InkLight,
    surface = BeBetterTokens.CardLight,
    onSurface = BeBetterTokens.InkLight,
    surfaceVariant = Color(0xFFF2F0EA),
    onSurfaceVariant = BeBetterTokens.MutedLight,
    outline = BeBetterTokens.CardBorderLight,
    outlineVariant = BeBetterTokens.LineLight,
    error = Color(0xFFDC2626),
    surfaceContainer = BeBetterTokens.CardLight,
    surfaceContainerHigh = Color(0xFFF2F0EA)
)

@Composable
fun BeBetterTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as? BeBetterApp
    var stored by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        stored = runCatching { app?.session?.getTheme() }.getOrNull()
        // observe changes (settings toggle) without restart
        runCatching {
            app?.session?.themeFlow?.collect { stored = it }
        }
    }
    // Web behaviour: stored 'light'/'dark' wins, else follow system.
    val effectiveDark = when (stored) {
        "light" -> false
        "dark" -> true
        else -> dark
    }
    MaterialTheme(
        colorScheme = if (effectiveDark) DarkScheme else LightScheme,
        typography = Typography(
            titleSmall = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            bodySmall = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            labelSmall = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium)
        ),
        content = content
    )
}

@Composable
fun isBeBetterDark(): Boolean = MaterialTheme.colorScheme.background == BeBetterTokens.BgDark

fun levelColor(level: Double, dark: Boolean): Color {
    // Match ContributionGrid.vue legend: gray-800/40 → emerald-400
    return when {
        level <= 0 -> if (dark) Color(0x661E242E) else Color(0xFFE9EDF2)
        level < 0.34 -> if (dark) Color(0xFF064E3B) else Color(0xFFA7F3D0)
        level < 0.67 -> Color(0xFF047857)
        level < 1.0 -> Color(0xFF10B981)
        else -> Color(0xFF34D399)
    }
}

// Section title like web: text-xs uppercase tracking-wider, faint
@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    )
}
