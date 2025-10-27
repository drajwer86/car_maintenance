package com.example.car_maintenance.data.dao

import androidx.room.*
import com.example.car_maintenance.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE carId = :carId AND isCompleted = 0 ORDER BY triggerDate ASC")
    fun getActiveRemindersByCarId(carId: Long): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 AND triggerDate <= :currentTime ORDER BY triggerDate ASC")
    suspend fun getPendingReminders(currentTime: Long): List<Reminder>
    
    @Query("SELECT * FROM reminders ORDER BY triggerDate DESC")
    fun getAllReminders(): Flow<List<Reminder>>
    
    @Insert
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :reminderId")
    suspend fun markReminderCompleted(reminderId: Long)
}