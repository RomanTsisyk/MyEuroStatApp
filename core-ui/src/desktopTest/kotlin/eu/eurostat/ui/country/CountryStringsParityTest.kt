package eu.eurostat.ui.country

import eu.eurostat.core.common.EurostatCountries
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Key-parity guard for core-ui's localized `strings.xml` files.
 *
 * Reads the resource XML straight from the source tree (Gradle runs tests with the
 * module directory as working directory), so it needs no resource runtime. JVM-only
 * because common code has no file access.
 */
class CountryStringsParityTest {

    private val nameRegex = Regex("""<string\s+name="([^"]+)"\s*>(.*?)</string>""")

    private fun stringsFile(valuesDir: String): File {
        val relative = "src/commonMain/composeResources/$valuesDir/strings.xml"
        return listOf(File(relative), File("core-ui/$relative")).firstOrNull { it.isFile }
            ?: fail("Cannot find $relative from ${File(".").absoluteFile}")
    }

    private fun entries(valuesDir: String): Map<String, String> =
        nameRegex.findAll(stringsFile(valuesDir).readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun english_defines_a_name_for_every_catalogue_code() {
        val en = entries("values")
        EurostatCountries.ALL.forEach { country ->
            assertTrue("country_${country.code.lowercase()}" in en, "EN missing name for '${country.code}'")
        }
    }

    @Test
    fun english_names_match_the_catalogue() {
        val en = entries("values")
        EurostatCountries.ALL.forEach { country ->
            assertEquals(country.name, en["country_${country.code.lowercase()}"], "code '${country.code}'")
        }
    }

    @Test
    fun polish_defines_every_key_english_defines() {
        assertEquals(emptySet(), entries("values").keys - entries("values-pl").keys)
    }

    @Test
    fun ukrainian_defines_every_key_english_defines() {
        assertEquals(emptySet(), entries("values").keys - entries("values-uk").keys)
    }

    @Test
    fun translated_country_names_are_not_blank() {
        listOf("values-pl", "values-uk").forEach { dir ->
            entries(dir).filterKeys { it.startsWith("country_") }.forEach { (key, value) ->
                assertTrue(value.isNotBlank(), "$dir/$key is blank")
            }
        }
    }
}
