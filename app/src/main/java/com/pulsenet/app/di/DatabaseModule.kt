package com.pulsenet.app.di

import android.content.Context
import androidx.room.Room
import com.pulsenet.app.data.local.PulseDatabase
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.dao.PeerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePulseDatabase(@ApplicationContext context: Context): PulseDatabase =
        Room.databaseBuilder(context, PulseDatabase::class.java, "pulsenet.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMessageDao(database: PulseDatabase): MessageDao = database.messageDao()

    @Provides
    fun providePeerDao(database: PulseDatabase): PeerDao = database.peerDao()
}
