package eu.eurostat.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Guards the bundled icon vectors that replaced material-icons-extended. */
class EuroIconsTest {

    private val all: Map<String, ImageVector> = mapOf(
        "MoreHoriz" to EuroIcons.MoreHoriz,
        "SsidChart" to EuroIcons.SsidChart,
        "People" to EuroIcons.People,
        "Euro" to EuroIcons.Euro,
        "Park" to EuroIcons.Park,
        "DirectionsBus" to EuroIcons.DirectionsBus,
        "FlightTakeoff" to EuroIcons.FlightTakeoff,
        "Groups" to EuroIcons.Groups,
        "Science" to EuroIcons.Science,
    )

    private fun paths(node: VectorNode): List<VectorPath> = when (node) {
        is VectorPath -> listOf(node)
        is VectorGroup -> node.flatMap { paths(it) }
        else -> emptyList()
    }

    @Test
    fun every_icon_is_a_24_by_24_vector_named_after_itself() {
        for ((name, icon) in all) {
            assertEquals(name, icon.name)
            assertEquals(24.dp, icon.defaultWidth, name)
            assertEquals(24.dp, icon.defaultHeight, name)
            assertEquals(24f, icon.viewportWidth, name)
            assertEquals(24f, icon.viewportHeight, name)
        }
    }

    @Test
    fun every_icon_has_at_least_one_non_trivial_path() {
        for ((name, icon) in all) {
            val ps = icon.root.flatMap { paths(it) }
            assertTrue(ps.isNotEmpty(), "$name has no path")
            // A parse failure would leave an empty or single-command path.
            assertTrue(ps.all { it.pathData.size > 3 }, "$name has a degenerate path")
        }
    }

    @Test
    fun the_bus_keeps_its_two_wheel_paths() {
        // DirectionsBus is the only multi-path icon: body plus two wheels.
        assertEquals(3, EuroIcons.DirectionsBus.root.flatMap { paths(it) }.size)
    }
}
