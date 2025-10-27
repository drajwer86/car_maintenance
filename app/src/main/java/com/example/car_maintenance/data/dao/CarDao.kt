package com.example.car_maintenance.data.dao

import androidx.room.*
import com.example.car_maintenance.data.model.Car
import com.example.car_maintenance.data.model.CarWithActivities
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllCars(): Flow<List<Car>>
    
    @Query("SELECT * FROM cars WHERE id = :carId")
    fun getCarById(carId: Long): Flow<Car?>
    
    @Transaction
    @Query("SELECT * FROM cars WHERE id = :carId")
    fun getCarWithActivities(carId: Long): Flow<CarWithActivities?>
    
    @Insert
    suspend fun insertCar(car: Car): Long
    
    @Update
    suspend fun updateCar(car: Car)
    
    @Query("UPDATE cars SET isActive = 0 WHERE id = :carId")
    suspend fun deleteCar(carId: Long)
    
    @Query("SELECT COUNT(*) FROM cars WHERE isActive = 1")
    suspend fun getActiveCarsCount(): Int
}