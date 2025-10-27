package com.example.car_maintenance.data.dao

import androidx.room.*
import com.example.car_maintenance.data.model.RefuelingDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RefuelingDetailsDao {
    @Query("SELECT * FROM refueling_details WHERE activityId = :activityId")
    fun getRefuelingDetails(activityId: Long): Flow<RefuelingDetails?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefuelingDetails(details: RefuelingDetails)
    
    @Update
    suspend fun updateRefuelingDetails(details: RefuelingDetails)
    
    @Delete
    suspend fun deleteRefuelingDetails(details: RefuelingDetails)
}