package com.wegrzyn.marcin.ignition.di

import android.content.Context
import android.content.SharedPreferences
import com.wegrzyn.marcin.ignition.bt.MyBtService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("preference_settings", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun providesBluetooth(@ApplicationContext context: Context) = MyBtService(context)


}