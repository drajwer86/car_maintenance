package com.example.car_maintenance.data.dao

import androidx.room.*
import com.example.car_maintenance.data.model.Activity
import com.example.car_maintenance.data.model.ActivityComplete
import com.example.car_maintenance.data.model.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE carId = :carId ORDER BY date DESC")
    fun getActivitiesByCarId(carId: Long): Flow<List<Activity>>
    
    @Transaction
    @Query("SELECT * FROM activities WHERE carId = :carId ORDER BY date DESC")
    fun getCompleteActivitiesByCarId(carId: Long): Flow<List<ActivityComplete>>
    
    @Transaction
    @Query("SELECT * FROM activities WHERE id = :activityId")
    fun getCompleteActivityById(activityId: Long): Flow<ActivityComplete?>
    
    @Query("SELECT * FROM activities WHERE carId = :carId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getActivitiesByDateRange(carId: Long, startDate: Long, endDate: Long): Flow<List<Activity>>
    
    @Query("SELECT * FROM activities WHERE carId = :carId AND type = :type ORDER BY date DESC")
    fun getActivitiesByType(carId: Long, type: ActivityType): Flow<List<Activity>>
    
    @Query("SELECT * FROM activities WHERE carId = :carId AND cost BETWEEN :minCost AND :maxCost ORDER BY date DESC")
    fun getActivitiesByCostRange(carId: Long, minCost: Double, maxCost: Double): Flow<List<Activity>>
    
    @Query("SELECT SUM(cost) FROM activities WHERE carId = :carId")
    suspend fun getTotalCostByCarId(carId: Long): Double?
    
    @Query("SELECT SUM(cost) FROM activities WHERE carId = :carId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCostByDateRange(carId: Long, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT SUM(cost) FROM activities WHERE carId = :carId AND type = :type")
    suspend fun getTotalCostByType(carId: Long, type: ActivityType): Double?
    
    @Query("SELECT * FROM activities WHERE carId = :carId AND type = 'OIL_CHANGE' ORDER BY date DESC LIMIT 1")
    suspend fun getLastOilChange(carId: Long): Activity?
    
    @Insert
    suspend fun insertActivity(activity: Activity): Long
    
    @Update
    suspend fun updateActivity(activity: Activity)
    
    @Delete
    suspend fun deleteActivity(activity: Activity)
    
    @Query("DELETE FROM activities WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: Long)
}