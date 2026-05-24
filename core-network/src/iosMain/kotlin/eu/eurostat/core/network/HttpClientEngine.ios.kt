package eu.eurostat.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Darwin
