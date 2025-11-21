package com.example.navigation.di

import com.example.navigation.AppComposeNavigator
import com.example.navigation.ComposeNavigator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds
    @Singleton
    abstract fun provideComposeNavigator(
        composeNavigator: ComposeNavigator,
    ): AppComposeNavigator
}