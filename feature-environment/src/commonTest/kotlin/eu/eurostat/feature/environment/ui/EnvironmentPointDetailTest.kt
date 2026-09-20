package eu.eurostat.feature.environment.ui

import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.feature.environment.domain.EnvMetric
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Unit tests for the tapped-point → detail-sheet mapping helpers. */
class EnvironmentPointDetailTest {

    @Test
    fun tappedPoint_mapsSeriesLabelYearAndValue() {
        val detail = tappedEnvironmentPoint("DE", ChartPoint(x = 2021.0, y = 762.5))

        assertEquals("DE", detail.countryCode)
        assertEquals(2021, detail.year)
        assertEquals(762.5, detail.value)
    }

    @Test
    fun tappedPoint_keepsNullValueAsNull() {
        val detail = tappedEnvironmentPoint("EU27_2020", ChartPoint(x = 2019.0, y = null))

        assertEquals("EU27_2020", detail.countryCode)
        assertEquals(2019, detail.year)
        assertNull(detail.value)
    }

    @Test
    fun datasetCode_matchesLiveEurostatDatasetPerMetric() {
        assertEquals("env_air_gge", EnvMetric.Ghg.datasetCode())
        assertEquals("nrg_bal_c", EnvMetric.Energy.datasetCode())
        assertEquals("sdg_13_10", EnvMetric.Sdg.datasetCode())
    }
}
