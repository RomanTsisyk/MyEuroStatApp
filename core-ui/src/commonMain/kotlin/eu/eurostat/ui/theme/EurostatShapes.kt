package eu.eurostat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens used throughout the design system. [pill] yields a
 * fully-rounded capsule shape regardless of size.
 */
data class EurostatShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val xlarge: RoundedCornerShape = RoundedCornerShape(20.dp),
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)
