package eu.eurostat.ui.component.states

import androidx.compose.runtime.Composable
import eu.eurostat.core.common.AppError
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_error_cache_empty
import myeurostatapp.core_ui.generated.resources.ui_error_http
import myeurostatapp.core_ui.generated.resources.ui_error_http_5xx
import myeurostatapp.core_ui.generated.resources.ui_error_no_network
import myeurostatapp.core_ui.generated.resources.ui_error_parse
import myeurostatapp.core_ui.generated.resources.ui_error_unknown
import org.jetbrains.compose.resources.stringResource

/**
 * Maps an [AppError] to a short, localized, user-facing message.
 *
 * This is the single source of truth for error copy shown in `ErrorState`
 * across every feature screen — do not reintroduce a per-feature copy.
 * Components keep the raw [AppError] in their `Error` UI state and screens
 * resolve it here, so the text follows the app locale (including runtime
 * switches via the Settings language picker).
 */
@Composable
fun AppError.localizedMessage(): String = when (this) {
    AppError.NoNetwork -> stringResource(Res.string.ui_error_no_network)
    is AppError.HttpError -> if (code in 500..599) {
        stringResource(Res.string.ui_error_http_5xx, code)
    } else {
        stringResource(Res.string.ui_error_http, code)
    }
    is AppError.ParseError -> stringResource(Res.string.ui_error_parse)
    AppError.CacheEmpty -> stringResource(Res.string.ui_error_cache_empty)
    is AppError.Unknown -> stringResource(Res.string.ui_error_unknown)
}
