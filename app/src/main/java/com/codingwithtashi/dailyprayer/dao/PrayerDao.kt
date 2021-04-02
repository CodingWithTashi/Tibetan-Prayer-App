package com.codingwithtashi.dailyprayer.dao

import androidx.room.*
import com.codingwithtashi.dailyprayer.model.Prayer
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM PRAYER_TABLE")
    fun getPrayers(): Flow<List<Prayer>>
    @Query("SELECT * FROM PRAYER_TABLE  WHERE isFavourite=:liked")
    fun getFavouritePrayer(liked: Boolean = true): Flow<List<Prayer>>
    @Query("SELECT * FROM PRAYER_TABLE  WHERE id=:id")
    fun getPrayer(id: Int?): Flow<Prayer>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayer : Prayer)
    @Update
    suspend fun update(prayer: Prayer)
    @Query("SELECT * FROM PRAYER_TABLE  WHERE title=:title LIMIT 1")
    fun getPrayerByName(title: String?): Flow<Prayer>
}