package eu.eurostat.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.theme.Euro

@Composable
fun SettingsScreen(
    component: SettingsComponent,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        ModuleAppBar(
            title = "Settings",
            accent = Euro.colors.accent,
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
                item {
                    SettingsListItem(
                        headline = "Language",
                        supporting = "System default",
                    )
                }
                item { HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f)) }
                item {
                    SettingsListItem(
                        headline = "Theme",
                        supporting = "Auto",
                    )
                }
                item { HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f)) }
                item {
                    SettingsListItem(
                        headline = "Default country",
                        supporting = "EU27_2020",
                    )
                }
                item { HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f)) }
                item {
                    SettingsListItem(
                        headline = "Data refresh interval",
                        supporting = "12 hours",
                    )
                }
                item { HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f)) }
                item {
                    SettingsListItem(
                        headline = "Clear cache",
                    )
                }
                item { HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f)) }
                item {
                    SettingsListItem(
                        headline = "About",
                        supporting = "v0.1.0",
                    )
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Settings — coming soon",
                        style = Euro.typography.bodySmall,
                        color = Euro.colors.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
        }
    }
}

@Composable
private fun SettingsListItem(
    headline: String,
    supporting: String? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = headline,
                style = Euro.typography.labelLarge,
                color = Euro.colors.ink,
            )
        },
        supportingContent = if (supporting != null) {
            {
                Text(
                    text = supporting,
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
            }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
    )
}
