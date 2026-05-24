package eu.eurostat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic color palette for the Eurostat design system.
 *
 * Values match the hi-fi pass (iOS 26 Liquid Glass + Material 3 Expressive):
 * warm paper background, white card surface, ink that is nearly-black with a
 * touch of warmth, and translucent borders/glass tokens for iOS frosted bars.
 *
 * @property paper page background — warm off-white (light) / near-black (dark)
 * @property paperAlt primary card surface — white in light, lifted in dark
 * @property surface2 secondary surface — warm beige chip background
 * @property ink primary text and strokes
 * @property inkSubtle secondary text
 * @property muted tertiary text / supporting copy
 * @property mutedAlt faint text (4th tier — disabled, very low-emphasis)
 * @property accent primary brand accent (muted navy in light, lifted in dark)
 * @property accentSoft tinted background for accent surfaces
 * @property warn alert / stale / sienna semantic color
 * @property grid translucent grid stroke for chart backgrounds
 * @property border translucent hairline border for cards and dividers
 * @property glass frosted-glass fill for iOS top/tab bars and pills
 * @property glassBorder hairline edge that sits on a glass surface
 */
data class EurostatColors(
    val paper: Color,
    val paperAlt: Color,
    val surface2: Color,
    val ink: Color,
    val inkSubtle: Color,
    val muted: Color,
    val mutedAlt: Color,
    val accent: Color,
    val accentSoft: Color,
    val warn: Color,
    val grid: Color,
    val border: Color,
    val glass: Color,
    val glassBorder: Color,
)

/** Light theme color set — the primary aesthetic of the app. */
fun lightColors(): EurostatColors = EurostatColors(
    paper = Color(0xFFF4F1EA),
    paperAlt = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEAE6DD),
    ink = Color(0xFF1A1817),
    inkSubtle = Color(0xFF3A352E),
    muted = Color(0xFF6B6359),
    mutedAlt = Color(0xFFB3ACA1),
    accent = Color(0xFF2F4969),
    accentSoft = Color(0xFFC6D0DC),
    warn = Color(0xFFC25A35),
    grid = Color(0x141A1817),
    border = Color(0x12000000),
    glass = Color(0x8CFFFFFF),
    glassBorder = Color(0x0F000000),
)

/** Dark theme color set — inverted ink/paper, lifted accent for contrast. */
fun darkColors(): EurostatColors = EurostatColors(
    paper = Color(0xFF0E0D0C),
    paperAlt = Color(0xFF1A1816),
    surface2 = Color(0xFF252320),
    ink = Color(0xFFF5F2EB),
    inkSubtle = Color(0xFFE8E2D5),
    muted = Color(0xFF9C948A),
    mutedAlt = Color(0xFF5C544D),
    accent = Color(0xFF7DA0C9),
    accentSoft = Color(0xFF2F4969),
    warn = Color(0xFFC25A35),
    grid = Color(0x1AF5F2EB),
    border = Color(0x1AFFFFFF),
    glass = Color(0x8C141210),
    glassBorder = Color(0x1FFFFFFF),
)
