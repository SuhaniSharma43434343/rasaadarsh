package com.example.rasaushadhies.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY id ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<MedicineEntity>)

    @Query("UPDATE medicines SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkState(id: Int, isBookmarked: Boolean)
    
    @Query("UPDATE medicines SET lastViewedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastViewed(id: Int, timestamp: Long)

    @Query("SELECT * FROM medicines WHERE lastViewedTimestamp > 0 ORDER BY lastViewedTimestamp DESC LIMIT 10")
    fun getRecentlyViewed(): Flow<List<MedicineEntity>>

    @Query("UPDATE medicines SET clinicalNotes = :notes WHERE id = :id")
    suspend fun updateClinicalNotes(id: Int, notes: String)

    @Query("SELECT COUNT(id) FROM medicines")
    suspend fun getMedicineCount(): Int

    @Query("SELECT * FROM medicines LIMIT 1")
    suspend fun getAllMedicinesOnce(): List<MedicineEntity>

    @Query("DELETE FROM medicines")
    suspend fun deleteAll()
}
