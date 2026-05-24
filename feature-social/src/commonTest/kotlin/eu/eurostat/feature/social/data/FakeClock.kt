package eu.eurostat.feature.social.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

class FakeClock(private var current: Instant) : Clock {
    override fun now(): Instant = current
    fun advance(d: Duration) { current += d }
}
