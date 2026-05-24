package eu.eurostat.feature.trade.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.trade.data.TradeApiService
import eu.eurostat.feature.trade.data.TradeApiServiceImpl
import eu.eurostat.feature.trade.data.TradeCacheDao
import eu.eurostat.feature.trade.data.TradeCacheDaoImpl
import eu.eurostat.feature.trade.data.TradeRepositoryImpl
import eu.eurostat.feature.trade.domain.GetTradeTimeSeriesUseCase
import eu.eurostat.feature.trade.domain.TradeRepository
import eu.eurostat.feature.trade.ui.DefaultTradeComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureTradeModule() = module {
    factory<TradeApiService> { TradeApiServiceImpl(get()) }
    factory<TradeCacheDao> { TradeCacheDaoImpl(get()) }
    single<TradeRepository> { TradeRepositoryImpl(get(), get(), get(), get()) }
    factory<Clock> { Clock.System }
    factory { GetTradeTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Trade::class.qualifiedName!!)
    ) {
        ComponentFactory { ctx -> DefaultTradeComponent(ctx, get(), get()) }
    }
}
