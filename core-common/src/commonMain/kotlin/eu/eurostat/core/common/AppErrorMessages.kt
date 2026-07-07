package eu.eurostat.core.common

/**
 * Maps an [AppError] to a short, user-facing message.
 *
 * This is the single source of truth for error copy shown in `ErrorState` across every
 * feature screen — do not reintroduce a per-feature `toUserMessage()` copy. Strings are
 * English-only for now; KMP-level (Compose Resources) localization is a later phase
 * (see `NEXT_STEPS.md`).
 */
fun AppError.toUserMessage(): String = when (this) {
    AppError.NoNetwork -> "No connection — check your network"
    is AppError.HttpError -> if (code in 500..599) {
        "Eurostat servers are temporarily unavailable (HTTP $code)"
    } else {
        "Eurostat request failed (HTTP $code)"
    }
    is AppError.ParseError -> "Got unexpected data from Eurostat"
    AppError.CacheEmpty -> "No cached data yet"
    is AppError.Unknown -> "Something went wrong"
}
