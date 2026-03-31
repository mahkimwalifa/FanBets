package com.seamhealth.elsrt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.seamhealth.elsrt.data.local.dao.FavoriteDao
import com.seamhealth.elsrt.data.local.entity.FavoriteLeagueEntity
import com.seamhealth.elsrt.data.local.entity.FavoriteTeamEntity

@Database(
    entities = [FavoriteTeamEntity::class, FavoriteLeagueEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FanBetsDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
