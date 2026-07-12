package eu.eurostat.app.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * Desktop/JVM: Compose Resources resolves strings through [Locale.getDefault],
 * so the override just swaps the JVM default. A backing composition local keeps
 * [current] observable and gives [provides] a [ProvidedValue] to return.
 */
actual object LocalAppLocale {

    /** System locale captured before the first override, restored on `null`. */
    private var default: Locale? = null

    private val LocalAppLocaleState = staticCompositionLocalOf { Locale.getDefault().toString() }

    actual val current: String
        @Composable get() = LocalAppLocaleState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when (value) {
            null -> requireNotNull(default) { "default locale captured above" }
            else -> Locale(value)
        }
        Locale.setDefault(new)
        return LocalAppLocaleState.provides(new.toString())
    }
}
