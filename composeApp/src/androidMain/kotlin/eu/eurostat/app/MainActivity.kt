package eu.eurostat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.arkivanov.decompose.retainedComponent
import eu.eurostat.ui.theme.EuroPlatform
import eu.eurostat.ui.theme.LocalEuroPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val root = retainedComponent { ctx -> createRootComponent(ctx) }
        setContent {
            CompositionLocalProvider(LocalEuroPlatform provides EuroPlatform.Android) {
                EurostatApp(root)
            }
        }
    }
}
