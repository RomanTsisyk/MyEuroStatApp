package eu.eurostat.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChildConfig {
    @Serializable data object Home : ChildConfig
    @Serializable data object Population : ChildConfig
    @Serializable data object Economy : ChildConfig
    @Serializable data object Environment : ChildConfig
    @Serializable data object Trade : ChildConfig
    @Serializable data object Transport : ChildConfig
    @Serializable data object Tourism : ChildConfig
    @Serializable data object Social : ChildConfig
    @Serializable data object Science : ChildConfig
    @Serializable data object Settings : ChildConfig
    @Serializable data object Search : ChildConfig
}
