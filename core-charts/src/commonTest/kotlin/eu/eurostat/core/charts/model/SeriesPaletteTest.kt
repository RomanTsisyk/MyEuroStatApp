package eu.eurostat.core.charts.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Behavior tests for [SeriesPalette]: distinctness of the first cycle and
 * safe cycling for out-of-range (including negative) indices.
 */
class SeriesPaletteTest {

    @Test
    fun first_cycle_colors_are_all_distinct() {
        val firstCycle = SeriesPalette.colors.indices.map { SeriesPalette.colorAt(it) }
        assertEquals(firstCycle.size, firstCycle.toSet().size)
    }

    @Test
    fun colorAt_matches_the_ordered_list_within_the_first_cycle() {
        SeriesPalette.colors.forEachIndexed { index, color ->
            assertEquals(color, SeriesPalette.colorAt(index))
        }
    }

    @Test
    fun cycles_past_the_palette_size() {
        val size = SeriesPalette.colors.size
        assertEquals(SeriesPalette.colorAt(0), SeriesPalette.colorAt(size))
        assertEquals(SeriesPalette.colorAt(3), SeriesPalette.colorAt(size + 3))
        assertEquals(SeriesPalette.colorAt(1), SeriesPalette.colorAt(2 * size + 1))
    }

    @Test
    fun negative_indices_cycle_backwards_from_the_end() {
        val size = SeriesPalette.colors.size
        assertEquals(SeriesPalette.colorAt(size - 1), SeriesPalette.colorAt(-1))
        assertEquals(SeriesPalette.colorAt(0), SeriesPalette.colorAt(-size))
    }
}
