package eu.eurostat.feature.transport.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

class FakeClock(initial: Instant) : Clock {
    private var current = initial
    fun advance(d: Duration) { current = current.plus(d) }
    override fun now(): Instant = current
}
