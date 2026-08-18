package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.foxdog.strucalendar.data.entity.HolidayEntity

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays WHERE countryCode = :countryCode AND date LIKE :yearPrefix || '%'")
    suspend fun getHolidaysForYear(countryCode: String, yearPrefix: String): List<HolidayEntity>

    // ★ 追加：特定の日付の祝日をすべての国から取得（優先順位はRepository側で決める）
    @Query("SELECT * FROM holidays WHERE date = :date")
    suspend fun getHolidaysForDate(date: String): List<HolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<HolidayEntity>)
}