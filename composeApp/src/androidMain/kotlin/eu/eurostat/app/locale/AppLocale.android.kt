package eu.eurostat.app.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Android: Compose Resources resolves strings through the default JVM locale and
 * the Android resource [android.content.res.Configuration], so the override must
 * update both. Providing the mutated configuration back through
 * [LocalConfiguration] keeps configuration-derived composables consistent.
 */
actual object LocalAppLocale {

    /** System locale captured before the first override, restored on `null`. */
    private var default: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when (value) {
            null -> requireNotNull(default) { "default locale captured above" }
            else -> Locale(value)
        }
        Locale.setDefault(new)
        configuration.setLocale(new)
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}
