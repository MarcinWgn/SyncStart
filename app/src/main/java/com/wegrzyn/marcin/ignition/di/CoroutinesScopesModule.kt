package com.wegrzyn.marcin.ignition.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoScope

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainScope

@InstallIn(SingletonComponent::class)
@Module
object CoroutinesScopesModule {

    @Singleton
    @IoScope
    @Provides
    fun providesIoCoroutineScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)


    @Singleton
    @MainScope
    @Provides
    fun providesMainCoroutineScope(@MainDispatcher mainDispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)

}