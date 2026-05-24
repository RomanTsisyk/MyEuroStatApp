package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EuroPlatform

/**
 * Hi-fi M3 Expressive top app bar for module screens.
 *
 * Layout (top to bottom):
 *  - Row of circular icon buttons: back · search · overflow
 *  - Eyebrow tagline (uppercase, accent-colored)
 *  - Large 40sp Inter Tight title
 *  - Optional year chip + country line
 *
 * When [tagline] is null the bar collapses to a single-row "compact" variant
 * (back · title · accent dot · actions) for non-module screens.
 *
 * @param title screen title rendered in [Euro.typography.displayLarge].
 * @param accent the per-module accent color used for the eyebrow / year chip.
 * @param onBack invoked when the back chevron is tapped.
 * @param tagline uppercase eyebrow shown above the title (e.g. "DEMOGRAPHY"). When null, falls back to compact layout.
 * @param year optional year shown in a small pill below the title.
 * @param country optional country line shown next to the year pill.
 * @param onSearch optional handler for the search icon; hidden when null.
 * @param onRefresh optional handler for the refresh icon; hidden when null.
 */
@Composable
fun ModuleAppBar(
    title: String,
    accent: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tagline: String? = null,
    year: Int? = null,
    country: String? = null,
    onSearch: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
) {
    if (tagline == null) {
        CompactBar(title, accent, onBack, onSearch, onRefresh, modifier)
        return
    }

    if (Euro.platform == EuroPlatform.Ios) {
        IosModuleAppBar(title, accent, onBack, tagline, year, country, onSearch, onRefresh, modifier)
    } else {
        AndroidModuleAppBar(title, accent, onBack, tagline, year, country, onSearch, onRefresh, modifier)
    }
}

/**
 * Android M3 Expressive variant — solid `paper` background, 40sp Inter Tight
 * Medium title (ls -1.4).
 */
@Composable
private fun AndroidModuleAppBar(
    title: String,
    accent: Color,
    onBack: () -> Unit,
    tagline: String,
    year: Int?,
    country: String?,
    onSearch: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Euro.colors.paper)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = Euro.spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconPill(icon = Icons.AutoMirrored.Filled.ArrowBack, description = "Back", onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(Euro.spacing.xs)) {
                if (onSearch != null) {
                    IconPill(icon = Icons.Default.Search, description = "Search", onClick = onSearch)
                }
                if (onRefresh != null) {
                    IconPill(icon = Icons.Default.Refresh, description = "Refresh", onClick = onRefresh)
                } else {
                    IconPill(icon = Icons.Default.MoreHoriz, description = "More", onClick = {})
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = tagline.uppercase(),
            style = Euro.typography.eyebrow,
            color = accent,
        )
        Spacer(Modifier.height(Euro.spacing.xs))
        Text(
            text = title,
            style = Euro.typography.displayLarge,
            color = Euro.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (year != null || country != null) {
            Spacer(Modifier.height(Euro.spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (year != null) {
                    YearChip(year = year, accent = accent)
                    if (country != null) Spacer(Modifier.width(Euro.spacing.s))
                }
                if (country != null) {
                    Text(
                        text = country,
                        style = Euro.typography.bodySmall,
                        color = Euro.colors.muted,
                    )
                }
            }
        }
    }
}

/**
 * iOS variant — semi-transparent `glass` background with a hairline
 * `glassBorder` bottom, SF-like large title (34sp Bold, ls -0.8) using
 * [Euro.typography.displayMedium], and 36dp glass-tinted icon pills.
 *
 * True backdrop blur is not available without a third-party library; the
 * semi-transparent [Euro.colors.glass] token (0x8CFFFFFF / 0x8C141210)
 * approximates the frosted appearance.
 */
@Composable
private fun IosModuleAppBar(
    title: String,
    accent: Color,
    onBack: () -> Unit,
    tagline: String,
    year: Int?,
    country: String?,
    onSearch: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
    modifier: Modifier,
) {
    // Eyebrow style: 12sp w600 uppercase, ls 0.4 — slightly different from the
    // Android eyebrow (11sp SemiBold ls 1.2).
    val iosEyebrow = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        fontFamily = Euro.typography.eyebrow.fontFamily,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Euro.colors.glass)
            .border(
                width = 0.5.dp,
                color = Euro.colors.glassBorder,
                // Only draw the bottom hairline by wrapping it in a bottom-biased shape
                // — we approximate this by drawing the full border and relying on the
                // fact that the glass surface sits above content so only the bottom edge
                // is visible in practice. A proper bottom-only hairline would need a
                // custom DrawModifier; the current approximation is acceptable.
            )
            .padding(horizontal = 20.dp, vertical = Euro.spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassIconPill(icon = Icons.AutoMirrored.Filled.ArrowBack, description = "Back", onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(Euro.spacing.xs)) {
                if (onSearch != null) {
                    GlassIconPill(icon = Icons.Default.Search, description = "Search", onClick = onSearch)
                }
                if (onRefresh != null) {
                    GlassIconPill(icon = Icons.Default.Refresh, description = "Refresh", onClick = onRefresh)
                } else {
                    GlassIconPill(icon = Icons.Default.MoreHoriz, description = "More", onClick = {})
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = tagline.uppercase(),
            style = iosEyebrow,
            color = accent,
        )
        Spacer(Modifier.height(Euro.spacing.xs))
        Text(
            text = title,
            // 34sp Bold, ls -0.8 — matches displayMedium exactly
            style = Euro.typography.displayMedium,
            color = Euro.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (year != null || country != null) {
            Spacer(Modifier.height(Euro.spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (year != null) {
                    YearChip(year = year, accent = accent)
                    if (country != null) Spacer(Modifier.width(Euro.spacing.s))
                }
                if (country != null) {
                    Text(
                        text = country,
                        style = Euro.typography.bodySmall,
                        color = Euro.colors.muted,
                    )
                }
            }
        }
    }
}

/**
 * Circular 36dp icon button on the warm `surface2` tint (Android).
 *
 * Wrapped in a 48dp tap region (WCAG 2.5.5 / Material minimum) — the visual
 * pill stays 36dp but the invisible clickable extends to 48dp around it.
 */
@Composable
private fun IconPill(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Euro.colors.surface2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Euro.colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Circular 36dp icon button on a semi-transparent `glass` tint (iOS). */
@Composable
private fun GlassIconPill(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Euro.colors.glass, CircleShape)
                .border(0.5.dp, Euro.colors.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Euro.colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Small year pill — surface2 background with accent dot. */
@Composable
private fun YearChip(year: Int, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Euro.colors.surface2)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(Euro.spacing.xs))
        Text(
            text = year.toString(),
            style = Euro.typography.tabularNumSmall,
            color = Euro.colors.muted,
        )
    }
}

/**
 * Compact fallback for screens without a tagline (e.g. Settings).
 * Single-row layout: back · title · accent dot · search · refresh.
 */
@Composable
private fun CompactBar(
    title: String,
    accent: Color,
    onBack: () -> Unit,
    onSearch: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Euro.colors.paper)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = Euro.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        IconPill(icon = Icons.AutoMirrored.Filled.ArrowBack, description = "Back", onClick = onBack)
        Spacer(Modifier.width(Euro.spacing.m))
        Text(
            text = title,
            style = Euro.typography.headlineLarge,
            color = Euro.colors.ink,
        )
        Spacer(Modifier.width(Euro.spacing.s))
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.weight(1f))
        if (onSearch != null) {
            IconPill(icon = Icons.Default.Search, description = "Search", onClick = onSearch)
            Spacer(Modifier.width(Euro.spacing.xs))
        }
        if (onRefresh != null) {
            IconPill(icon = Icons.Default.Refresh, description = "Refresh", onClick = onRefresh)
        }
    }
}
