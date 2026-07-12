package eu.eurostat.core.database

import eu.eurostat.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

/** [DispatcherProvider] that routes every lane onto the given test dispatcher. */
class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
