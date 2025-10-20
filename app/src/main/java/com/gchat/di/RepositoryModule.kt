package com.gchat.di

import com.gchat.data.repository.AuthRepositoryImpl
import com.gchat.data.repository.ConversationRepositoryImpl
import com.gchat.data.repository.MediaRepositoryImpl
import com.gchat.data.repository.MessageRepositoryImpl
import com.gchat.data.repository.UserRepositoryImpl
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.MediaRepository
import com.gchat.domain.repository.MessageRepository
import com.gchat.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository
    
    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        conversationRepositoryImpl: ConversationRepositoryImpl
    ): ConversationRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository
}

