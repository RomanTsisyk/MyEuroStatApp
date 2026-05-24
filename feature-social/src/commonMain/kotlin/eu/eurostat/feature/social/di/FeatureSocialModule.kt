package eu.eurostat.feature.social.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.social.data.SocialApiService
import eu.eurostat.feature.social.data.SocialApiServiceImpl
import eu.eurostat.feature.social.data.SocialCacheDao
import eu.eurostat.feature.social.data.SocialCacheDaoImpl
import eu.eurostat.feature.social.data.SocialRepositoryImpl
import eu.eurostat.feature.social.domain.GetSocialTimeSeriesUseCase
import eu.eurostat.feature.social.domain.SocialRepository
import eu.eurostat.feature.social.ui.DefaultSocialComponent
import kotlinx.datetime.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun featureSocialModule() = module {
    factory<SocialApiService> { SocialApiServiceImpl(get()) }
    factory<SocialCacheDao> { SocialCacheDaoImpl(get()) }
    single<SocialRepository> { SocialRepositoryImpl(get(), get(), get(), get()) }
    factory<Clock> { Clock.System }
    factory { GetSocialTimeSeriesUseCase(get()) }
    factory<ComponentFactory<Any>>(qualifier = named(ChildConfig.Social::class.qualifiedName!!)) {
        ComponentFactory { ctx -> DefaultSocialComponent(ctx, get(), get()) }
    }
}
