package com.example.data.di

import com.example.data.repository.PasskeysRepository
import com.example.data.repository.PasskeysRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface PasskeysDataModule {
    @Binds
    fun providePasskeysRepository(impl: PasskeysRepositoryImpl): PasskeysRepository
}