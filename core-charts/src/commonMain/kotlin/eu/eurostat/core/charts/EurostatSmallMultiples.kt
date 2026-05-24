package eu.eurostat.core.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.internal.ChartDefaults
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries

/**
 * One titled panel of an [EurostatSmallMultiples] grid.
 *
 * @property title Caption shown above the panel.
 * @property content Composable rendered as the panel body.
 */
data class SmallMultiplePanel(
    val title: String,
    val content: @Composable () -> Unit,
)

/**
 * Grid wrapper that renders independent chart panels at a uniform size.
 *
 * @param panels Panels to render in row-major order.
 * @param columns Number of grid columns (defaults to 2).
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatSmallMultiples(
    panels: List<SmallMultiplePanel>,
    columns: Int = 2,
    modifier: Modifier = Modifier,
) {
    if (panels.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val cols = columns.coerceAtLeast(1)
    val rows = (panels.size + cols - 1) / cols
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    if (idx < panels.size) {
                        val panel = panels[idx]
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = panel.title,
                                style = ChartDefaults.CategoryLabelStyle,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            panel.content()
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Preview composable for [EurostatSmallMultiples]. */
@Composable
fun EurostatSmallMultiplesPreview() {
    val years = (2015..2023).map { it.toDouble() }
    fun panel(label: String, seed: Double) = SmallMultiplePanel(
        title = label,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .background(ChartDefaults.Paper)
            ) {
                EurostatLineChart(
                    series = listOf(
                        ChartSeries(
                            label = label,
                            color = ChartDefaults.Accent,
                            points = years.mapIndexed { i, y -> ChartPoint(y, seed + i) },
                        ),
                    ),
                    xAxis = eu.eurostat.core.charts.model.ChartAxis(label = "Year"),
                    yAxis = eu.eurostat.core.charts.model.ChartAxis(label = ""),
                    hideAxis = true,
                )
            }
        },
    )
    EurostatSmallMultiples(
        panels = listOf(panel("DE", 80.0), panel("FR", 65.0), panel("IT", 59.0), panel("ES", 47.0)),
        columns = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}
