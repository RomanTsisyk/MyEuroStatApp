package eu.eurostat.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The handful of Material icons the app uses that are not in
 * `material-icons-core`. They used to come from `material-icons-extended`, a
 * ~11 000-icon library whose Kotlin/Native cache build exhausts the compiler's
 * heap on CI (`ios-test`) and adds several MB to the APK, so only the nine icons
 * actually used are kept here as plain vector paths.
 *
 * Path data is taken from Google's Material Icons as shipped in
 * `androidx.compose.material:material-icons-extended` (Apache License 2.0,
 * https://www.apache.org/licenses/LICENSE-2.0). All are 24x24, filled black
 * (tint them at the call site) and identical to the originals they replace.
 */
object EuroIcons {
    val MoreHoriz: ImageVector by lazy {
        icon("MoreHoriz", "M6,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2s-0.9,-2 -2,-2zM18,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2s-0.9,-2 -2,-2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2s-0.9,-2 -2,-2z")
    }

    val SsidChart: ImageVector by lazy {
        icon("SsidChart", "M21,5.47L12,12L7.62,7.62L3,11V8.52L7.83,5l4.38,4.38L21,3L21,5.47zM21,15h-4.7l-4.17,3.34L6,12.41l-3,2.13L3,17l2.8,-2l6.2,6l5,-4h4V15z")
    }

    val People: ImageVector by lazy {
        icon("People", "M9,13.75c-2.34,0 -7,1.17 -7,3.5L2,19h14v-1.75c0,-2.33 -4.66,-3.5 -7,-3.5zM4.34,17c0.84,-0.58 2.87,-1.25 4.66,-1.25s3.82,0.67 4.66,1.25L4.34,17zM9,12c1.93,0 3.5,-1.57 3.5,-3.5S10.93,5 9,5S5.5,6.57 5.5,8.5S7.07,12 9,12zM9,7c0.83,0 1.5,0.67 1.5,1.5S9.83,10 9,10s-1.5,-0.67 -1.5,-1.5S8.17,7 9,7zM16.04,13.81c1.16,0.84 1.96,1.96 1.96,3.44L18,19h4v-1.75c0,-2.02 -3.5,-3.17 -5.96,-3.44zM15,12c1.93,0 3.5,-1.57 3.5,-3.5S16.93,5 15,5c-0.54,0 -1.04,0.13 -1.5,0.35c0.63,0.89 1,1.98 1,3.15s-0.37,2.26 -1,3.15c0.46,0.22 0.96,0.35 1.5,0.35z")
    }

    val Euro: ImageVector by lazy {
        icon("Euro", "M15,18.5c-2.51,0 -4.68,-1.42 -5.76,-3.5H15l1,-2H8.58c-0.05,-0.33 -0.08,-0.66 -0.08,-1s0.03,-0.67 0.08,-1H15l1,-2H9.24C10.32,6.92 12.5,5.5 15,5.5c1.61,0 3.09,0.59 4.23,1.57L21,5.3C19.41,3.87 17.3,3 15,3c-3.92,0 -7.24,2.51 -8.48,6H3l-1,2h4.06C6.02,11.33 6,11.66 6,12s0.02,0.67 0.06,1H3l-1,2h4.52c1.24,3.49 4.56,6 8.48,6c2.31,0 4.41,-0.87 6,-2.3l-1.78,-1.77C18.09,17.91 16.62,18.5 15,18.5z")
    }

    val Park: ImageVector by lazy {
        icon("Park", "M17,12h2L12,2L5.05,12H7l-3.9,6h6.92v4h3.95v-4H21L17,12zM6.79,16l3.9,-6H8.88l3.13,-4.5l3.15,4.5h-1.9l4,6H6.79z")
    }

    val DirectionsBus: ImageVector by lazy {
        icon(
            "DirectionsBus",
            "M12,2c-4.42,0 -8,0.5 -8,4v10c0,0.88 0.39,1.67 1,2.22L5,20c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-1h8v1c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-1.78c0.61,-0.55 1,-1.34 1,-2.22L20,6c0,-3.5 -3.58,-4 -8,-4zM17.66,4.99L6.34,4.99C6.89,4.46 8.31,4 12,4s5.11,0.46 5.66,0.99zM18,6.99L18,10L6,10L6,6.99h12zM17.66,16.73l-0.29,0.27L6.63,17l-0.29,-0.27C6.21,16.62 6,16.37 6,16v-4h12v4c0,0.37 -0.21,0.62 -0.34,0.73z",
            "M8.5,14.5m-1.5,0a1.5,1.5 0 1,1 3,0a1.5,1.5 0 1,1 -3,0",
            "M15.5,14.5m-1.5,0a1.5,1.5 0 1,1 3,0a1.5,1.5 0 1,1 -3,0",
        )
    }

    val FlightTakeoff: ImageVector by lazy {
        icon("FlightTakeoff", "M2.5,19h19v2h-19v-2zM22.07,9.64c-0.21,-0.8 -1.04,-1.28 -1.84,-1.06L14.92,10l-6.9,-6.43l-1.93,0.51l4.14,7.17l-4.97,1.33l-1.97,-1.54l-1.45,0.39l2.59,4.49L21,11.49c0.81,-0.23 1.28,-1.05 1.07,-1.85z")
    }

    val Groups: ImageVector by lazy {
        icon("Groups", "M4,13c1.1,0 2,-0.9 2,-2c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2C2,12.1 2.9,13 4,13zM5.13,14.1C4.76,14.04 4.39,14 4,14c-0.99,0 -1.93,0.21 -2.78,0.58C0.48,14.9 0,15.62 0,16.43V18l4.5,0v-1.61C4.5,15.56 4.73,14.78 5.13,14.1zM20,13c1.1,0 2,-0.9 2,-2c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2C18,12.1 18.9,13 20,13zM24,16.43c0,-0.81 -0.48,-1.53 -1.22,-1.85C21.93,14.21 20.99,14 20,14c-0.39,0 -0.76,0.04 -1.13,0.1c0.4,0.68 0.63,1.46 0.63,2.29V18l4.5,0V16.43zM16.24,13.65c-1.17,-0.52 -2.61,-0.9 -4.24,-0.9c-1.63,0 -3.07,0.39 -4.24,0.9C6.68,14.13 6,15.21 6,16.39V18h12v-1.61C18,15.21 17.32,14.13 16.24,13.65zM8.07,16c0.09,-0.23 0.13,-0.39 0.91,-0.69c0.97,-0.38 1.99,-0.56 3.02,-0.56s2.05,0.18 3.02,0.56c0.77,0.3 0.81,0.46 0.91,0.69H8.07zM12,8c0.55,0 1,0.45 1,1s-0.45,1 -1,1s-1,-0.45 -1,-1S11.45,8 12,8M12,6c-1.66,0 -3,1.34 -3,3c0,1.66 1.34,3 3,3s3,-1.34 3,-3C15,7.34 13.66,6 12,6L12,6z")
    }

    val Science: ImageVector by lazy {
        icon("Science", "M13,11.33L18,18H6l5,-6.67V6h2M15.96,4H8.04C7.62,4 7.39,4.48 7.65,4.81L9,6.5v4.17L3.2,18.4C2.71,19.06 3.18,20 4,20h16c0.82,0 1.29,-0.94 0.8,-1.6L15,10.67V6.5l1.35,-1.69C16.61,4.48 16.38,4 15.96,4L15.96,4z")
    }
}

private fun icon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        for (path in paths) addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    }.build()
