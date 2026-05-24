package eu.eurostat.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Indirection over [Dispatchers] so tests can substitute [StandardTestDispatcher].
 *
 * Inject via Koin in production: single { DefaultDispatcherProvider() bind DispatcherProvider::class }
 * Override in tests:                module { single<DispatcherProvider> { TestDispatcherProvider(testDispatcher) } }
 *
 * Never reference [Dispatchers.IO] or [Dispatchers.Default] directly in repositories or use cases.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/**
 * Default implementation backed by platform [Dispatchers].
 * On Android: io maps to [Dispatchers.IO] (thread pool).
 * On iOS/native: io maps to [Dispatchers.Default] (native equivalent).
 */
expect class DefaultDispatcherProvider() : DispatcherProvider {
    override val main: CoroutineDispatcher
    override val io: CoroutineDispatcher
    override val default: CoroutineDispatcher
}
