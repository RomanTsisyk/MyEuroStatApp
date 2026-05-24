package eu.eurostat.core.navigation.di

import org.koin.dsl.module

fun coreNavigationModule() = module {
    // RootComponent is created per ComponentContext at the composition root —
    // feature modules contribute ComponentFactory bindings keyed by ChildConfig::class.qualifiedName.
}
