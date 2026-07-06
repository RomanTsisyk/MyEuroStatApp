package eu.eurostat.core.navigation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Decompose stack persists [ChildConfig] via kotlinx-serialization, so every
 * member of the sealed hierarchy must round-trip through the serializer.
 */
class ChildConfigSerializationTest {

    private val json = Json

    private val allConfigs: List<ChildConfig> = listOf(
        ChildConfig.Home,
        ChildConfig.Population,
        ChildConfig.Economy,
        ChildConfig.Environment,
        ChildConfig.Trade,
        ChildConfig.Transport,
        ChildConfig.Tourism,
        ChildConfig.Social,
        ChildConfig.Science,
        ChildConfig.Settings,
    )

    @Test
    fun every_config_round_trips_through_the_serializer() {
        allConfigs.forEach { config ->
            val encoded = json.encodeToString(ChildConfig.serializer(), config)
            val decoded = json.decodeFromString(ChildConfig.serializer(), encoded)
            assertEquals(config, decoded, "Round-trip failed for $config (encoded=$encoded)")
        }
    }

    @Test
    fun distinct_configs_encode_to_distinct_payloads() {
        val encodings = allConfigs.map { json.encodeToString(ChildConfig.serializer(), it) }
        assertEquals(encodings.size, encodings.toSet().size, "Configs collided on encoding: $encodings")
    }

    @Test
    fun home_is_the_default_landing_destination() {
        // Guards the initialConfiguration contract used by DefaultRootComponent.
        assertTrue(ChildConfig.Home in allConfigs)
    }
}
