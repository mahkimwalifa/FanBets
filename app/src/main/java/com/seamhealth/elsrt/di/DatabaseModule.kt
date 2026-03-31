package com.seamhealth.elsrt.di

import android.content.Context
import androidx.room.Room
import com.seamhealth.elsrt.data.local.FanBetsDatabase
import com.seamhealth.elsrt.data.local.dao.FavoriteDao
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
    fun provideDatabase(@ApplicationContext context: Context): FanBetsDatabase =
        Room.databaseBuilder(
            context,
            FanBetsDatabase::class.java,
            "fanbets_db"
        ).build()

    @Provides
    @Singleton
    fun provideFavoriteDao(db: FanBetsDatabase): FavoriteDao = db.favoriteDao()
}
