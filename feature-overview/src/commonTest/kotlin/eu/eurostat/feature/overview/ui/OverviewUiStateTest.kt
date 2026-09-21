package eu.eurostat.feature.overview.ui

import eu.eurostat.core.common.AppError
import eu.eurostat.core.navigation.ChildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import myeurostatapp.feature_overview.generated.resources.Res
import myeurostatapp.feature_overview.generated.resources.overview_module_title_economy
import myeurostatapp.feature_overview.generated.resources.overview_module_unit_economy

/** Pure logic of [OverviewUiState.unavailableError]: when does the dashboard say "nothing loaded"? */
class OverviewUiStateTest {

    private fun teaser(status: TeaserStatus, error: AppError? = null) = ModuleTeaser(
        destination = ChildConfig.Economy,
        accentKey = "economy",
        titleRes = Res.string.overview_module_title_economy,
        emoji = "x",
        value = if (status == TeaserStatus.Loaded) 1.0 else null,
        format = TeaserFormat.Grouped,
        unitRes = Res.string.overview_module_unit_economy,
        year = null,
        status = status,
        error = error,
    )

    private fun failed(error: AppError = AppError.NoNetwork) = teaser(TeaserStatus.Error, error)

    private fun stateOf(vararg teasers: ModuleTeaser) = OverviewUiState(teasers.toList(), "DE")

    @Test
    fun teaser_error_defaults_to_null() {
        val teaser = ModuleTeaser(
            destination = ChildConfig.Economy,
            accentKey = "economy",
            titleRes = Res.string.overview_module_title_economy,
            emoji = "x",
            value = null,
            format = TeaserFormat.Grouped,
            unitRes = Res.string.overview_module_unit_economy,
            year = null,
            status = TeaserStatus.Loading,
        )
        assertNull(teaser.error)
    }

    @Test
    fun all_error_returns_the_error() {
        val state = stateOf(failed(), failed(), failed())
        assertEquals(AppError.NoNetwork, state.unavailableError)
    }

    @Test
    fun error_mixed_with_empty_returns_the_errors_cause() {
        val cause = AppError.HttpError(503, "unavailable")
        val state = stateOf(
            teaser(TeaserStatus.Empty),
            failed(cause),
            teaser(TeaserStatus.Empty),
        )
        assertEquals(cause, state.unavailableError)
    }

    @Test
    fun all_empty_without_an_error_returns_null() {
        val state = stateOf(teaser(TeaserStatus.Empty), teaser(TeaserStatus.Empty))
        assertNull(state.unavailableError)
    }

    @Test
    fun any_loaded_teaser_returns_null() {
        val state = stateOf(failed(), teaser(TeaserStatus.Loaded), failed())
        assertNull(state.unavailableError)
    }

    @Test
    fun any_loading_teaser_returns_null() {
        val state = stateOf(failed(), teaser(TeaserStatus.Loading), failed())
        assertNull(state.unavailableError)
    }

    @Test
    fun empty_teaser_list_returns_null() {
        assertNull(stateOf().unavailableError)
    }

    @Test
    fun first_error_in_teaser_order_wins_when_causes_differ() {
        val later = AppError.Unknown(IllegalStateException("boom"))
        val state = stateOf(
            failed(AppError.NoNetwork),
            failed(later),
            failed(AppError.HttpError(500, "server")),
        )
        assertEquals(AppError.NoNetwork, state.unavailableError)
    }

    @Test
    fun error_status_without_a_recorded_cause_returns_null() {
        val state = stateOf(teaser(TeaserStatus.Error), teaser(TeaserStatus.Error))
        assertNull(state.unavailableError)
    }
}
