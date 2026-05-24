package eu.eurostat.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class DefaultDispatcherProvider actual constructor() : DispatcherProvider {
    override actual val main: CoroutineDispatcher get() = Dispatchers.Main
    override actual val io: CoroutineDispatcher get() = Dispatchers.IO
    override actual val default: CoroutineDispatcher get() = Dispatchers.Default
}
