package eu.eurostat.feature.science.ui

import androidx.compose.ui.graphics.Color
import eu.eurostat.feature.science.domain.ScienceDataPoint
import eu.eurostat.feature.science.domain.ScienceTimeSeries
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests that the sparkline tiles read "as of the selected year". */
class SparkSeriesTest {

    private val series = ScienceTimeSeries(
        countryCode = "PL",
        countryName = "Poland",
        points = listOf(
            ScienceDataPoint("PL", 2014, rdSpendPctGdp = 0.94),
            ScienceDataPoint("PL", 2016, rdSpendPctGdp = 0.96),
            ScienceDataPoint("PL", 2018, rdSpendPctGdp = null),
            ScienceDataPoint("PL", 2024, rdSpendPctGdp = 1.41),
        ),
    )

    @Test
    fun points_after_the_selected_year_are_excluded() {
        val spark = series.toSparkSeries(Color.Black, upToYear = 2016) { it.rdSpendPctGdp }
        assertEquals(listOf(2014.0, 2016.0), spark.points.map { it.x })
        // The tile value is the last point: the selected year's, not the latest year's.
        assertEquals(0.96, spark.points.last().y)
    }

    @Test
    fun null_year_keeps_every_known_point() {
        val spark = series.toSparkSeries(Color.Black, upToYear = null) { it.rdSpendPctGdp }
        assertEquals(listOf(2014.0, 2016.0, 2024.0), spark.points.map { it.x })
        assertEquals(1.41, spark.points.last().y)
    }

    @Test
    fun selected_year_without_a_value_falls_back_to_the_last_earlier_one() {
        val spark = series.toSparkSeries(Color.Black, upToYear = 2018) { it.rdSpendPctGdp }
        assertEquals(0.96, spark.points.last().y)
    }
}
