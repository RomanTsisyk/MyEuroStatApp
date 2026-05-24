package eu.eurostat.feature.settings.ui

import com.arkivanov.decompose.ComponentContext

interface SettingsComponent

class DefaultSettingsComponent(
    componentContext: ComponentContext,
) : SettingsComponent, ComponentContext by componentContext
