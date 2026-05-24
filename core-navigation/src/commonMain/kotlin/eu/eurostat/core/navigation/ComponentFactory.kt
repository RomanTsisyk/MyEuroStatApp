package eu.eurostat.core.navigation

import com.arkivanov.decompose.ComponentContext

fun interface ComponentFactory<C : Any> {
    fun create(context: ComponentContext): C
}
