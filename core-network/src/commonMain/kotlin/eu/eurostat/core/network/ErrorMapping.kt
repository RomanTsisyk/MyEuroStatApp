package eu.eurostat.core.network

import eu.eurostat.core.common.AppError

/**
 * Maps any [Throwable] to a typed [AppError] for consumption by the domain/UI layer.
 *
 * Platform-specific actual implementations (androidMain / iosMain) perform type-safe
 * `is` checks against Ktor 3.0.2 exception classes that are not visible from the
 * commonMain metadata compilation. Fallback: [AppError.Unknown].
 */
expect fun Throwable.toAppError(): AppError
