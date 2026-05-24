package eu.eurostat.feature.settings.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.settings.ui.DefaultSettingsComponent
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureSettingsModule() = module {
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Settings::class.qualifiedName!!)
    ) {
        ComponentFactory { ctx -> DefaultSettingsComponent(ctx) }
    }
}
