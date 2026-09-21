package eu.eurostat.ui.component

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Regression guard for the app bar's removed "More" pill.
 *
 * `ModuleAppBar` once drew a third action pill whose `onClick` was an empty lambda whenever
 * a screen passed no `onRefresh`: a dead affordance with no overflow menu behind it. core-ui
 * has no Compose UI test infrastructure, so this reads the sources straight from the module
 * directory (Gradle runs tests with it as working directory) and checks that a no-op click
 * handler and the orphaned `ui_action_more` string do not come back. JVM-only because common
 * code has no file access.
 */
class ModuleAppBarSourceGuardTest {

    private val noOpClick = Regex("""onClick\s*=\s*\{\s*\}""")
    private val moreStringName = Regex("""<string\s+name="ui_action_more"""")

    private fun moduleFile(relative: String): File =
        listOf(File(relative), File("core-ui/$relative")).firstOrNull { it.exists() }
            ?: fail("Cannot find $relative from ${File(".").absoluteFile}")

    @Test
    fun module_app_bar_has_no_noop_click_handler() {
        val source = moduleFile("src/commonMain/kotlin/eu/eurostat/ui/component/ModuleAppBar.kt").readText()
        assertEquals(
            0,
            noOpClick.findAll(source).count(),
            "ModuleAppBar must not draw a pill whose onClick is an empty lambda",
        )
    }

    @Test
    fun no_locale_defines_the_removed_more_string() {
        val resources = moduleFile("src/commonMain/composeResources")
        val stringsFiles = resources.listFiles { file -> file.isDirectory && file.name.startsWith("values") }
            .orEmpty()
            .map { File(it, "strings.xml") }
            .filter { it.isFile }
        assertTrue(stringsFiles.isNotEmpty(), "No strings.xml found under $resources")
        val offenders = stringsFiles.filter { moreStringName.containsMatchIn(it.readText()) }.map { it.path }
        assertEquals(emptyList(), offenders, "ui_action_more has no consumer left and must stay removed")
    }
}
