package eu.eurostat.core.common

/**
 * Three-state result wrapper for data layer operations.
 *
 * Used as the value type inside [kotlinx.coroutines.flow.Flow] returned from
 * UseCases and Repositories. Flow emissions over time can transition:
 *   Loading -> Success    (cache hit or network success)
 *   Loading -> Error      (network failure, no cache)
 *   Success -> Success    (stale-while-revalidate: cached -> fresh)
 *   Success -> Error      (revalidation failed but stale data still shown)
 *
 * UI layer maps this to its own UiState; do not expose Result directly to Composables.
 */
sealed interface Result<out T> {
    data object Loading : Result<Nothing>
    data class Success<T>(val data: T, val isStale: Boolean = false) : Result<T>
    data class Error(val cause: AppError) : Result<Nothing>
}

/**
 * Domain-level error taxonomy. Maps to user-facing messages in UI layer via
 * a string resource lookup — never expose raw exceptions to UI.
 */
sealed interface AppError {
    data object NoNetwork : AppError
    data class HttpError(val code: Int, val message: String) : AppError
    data class ParseError(val message: String) : AppError
    data object CacheEmpty : AppError
    data class Unknown(val throwable: Throwable) : AppError
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Loading -> Result.Loading
    is Result.Success -> Result.Success(transform(data), isStale)
    is Result.Error -> this
}
