package com.example.car_maintenance.data.repository

import com.example.car_maintenance.data.dao.*
import com.example.car_maintenance.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class CarRepository(
    private val carDao: CarDao,
    private val activityDao: ActivityDao,
    private val activityImageDao: ActivityImageDao,
    private val reminderDao: ReminderDao,
    private val refuelingDetailsDao: RefuelingDetailsDao
) {
    // Car operations
    fun getAllCars(): Flow<List<Car>> = carDao.getAllCars()
    
    fun getCarById(carId: Long): Flow<Car?> = carDao.getCarById(carId)
    
    fun getCarWithActivities(carId: Long): Flow<CarWithActivities?> = 
        carDao.getCarWithActivities(carId)
    
    suspend fun insertCar(car: Car): Long = carDao.insertCar(car)
    
    suspend fun updateCar(car: Car) = carDao.updateCar(car)
    
    suspend fun deleteCar(carId: Long) = carDao.deleteCar(carId)
    
    suspend fun getActiveCarsCount(): Int = carDao.getActiveCarsCount()
    
    // Activity operations
    fun getActivitiesByCarId(carId: Long): Flow<List<Activity>> = 
        activityDao.getActivitiesByCarId(carId)
    
    fun getCompleteActivitiesByCarId(carId: Long): Flow<List<ActivityComplete>> = 
        activityDao.getCompleteActivitiesByCarId(carId)
    
    fun getCompleteActivityById(activityId: Long): Flow<ActivityComplete?> = 
        activityDao.getCompleteActivityById(activityId)
    
    fun getActivitiesByDateRange(carId: Long, startDate: Long, endDate: Long): Flow<List<Activity>> = 
        activityDao.getActivitiesByDateRange(carId, startDate, endDate)
    
    fun getActivitiesByType(carId: Long, type: ActivityType): Flow<List<Activity>> = 
        activityDao.getActivitiesByType(carId, type)
    
    fun getActivitiesByCostRange(carId: Long, minCost: Double, maxCost: Double): Flow<List<Activity>> = 
        activityDao.getActivitiesByCostRange(carId, minCost, maxCost)
    
    suspend fun getTotalCostByCarId(carId: Long): Double = 
        activityDao.getTotalCostByCarId(carId) ?: 0.0
    
    suspend fun getTotalCostByDateRange(carId: Long, startDate: Long, endDate: Long): Double = 
        activityDao.getTotalCostByDateRange(carId, startDate, endDate) ?: 0.0
    
    suspend fun getTotalCostByType(carId: Long, type: ActivityType): Double = 
        activityDao.getTotalCostByType(carId, type) ?: 0.0
    
    suspend fun getLastOilChange(carId: Long): Activity? = 
        activityDao.getLastOilChange(carId)
    
    suspend fun insertActivity(activity: Activity): Long = 
        activityDao.insertActivity(activity)
    
    suspend fun updateActivity(activity: Activity) = 
        activityDao.updateActivity(activity)
    
    suspend fun deleteActivity(activity: Activity) {
        // Delete associated images from filesystem
        val images = activityImageDao.getImagesByActivityId(activity.id).first()
        images.forEach { image ->
            File(image.filePath).delete()
            File(image.thumbnailPath).delete()
        }
        activityDao.deleteActivity(activity)
    }
    
    suspend fun deleteActivityById(activityId: Long) {
        val images = activityImageDao.getImagesByActivityId(activityId).first()
        images.forEach { image ->
            File(image.filePath).delete()
            File(image.thumbnailPath).delete()
        }
        activityDao.deleteActivityById(activityId)
    }
    
    // Image operations
    fun getImagesByActivityId(activityId: Long): Flow<List<ActivityImage>> = 
        activityImageDao.getImagesByActivityId(activityId)
    
    suspend fun insertImage(image: ActivityImage): Long = 
        activityImageDao.insertImage(image)
    
    suspend fun insertImages(images: List<ActivityImage>) = 
        activityImageDao.insertImages(images)
    
    suspend fun deleteImage(image: ActivityImage) {
        File(image.filePath).delete()
        File(image.thumbnailPath).delete()
        activityImageDao.deleteImage(image)
    }
    
    suspend fun deleteImageById(imageId: Long) {
        // This would need to fetch the image first to delete files
        // For simplicity, we'll handle this in the ViewModel
        activityImageDao.deleteImageById(imageId)
    }
    
    // Reminder operations
    fun getActiveRemindersByCarId(carId: Long): Flow<List<Reminder>> = 
        reminderDao.getActiveRemindersByCarId(carId)
    
    suspend fun getPendingReminders(currentTime: Long): List<Reminder> = 
        reminderDao.getPendingReminders(currentTime)
    
    fun getAllReminders(): Flow<List<Reminder>> = 
        reminderDao.getAllReminders()
    
    suspend fun insertReminder(reminder: Reminder): Long = 
        reminderDao.insertReminder(reminder)
    
    suspend fun updateReminder(reminder: Reminder) = 
        reminderDao.updateReminder(reminder)
    
    suspend fun deleteReminder(reminder: Reminder) = 
        reminderDao.deleteReminder(reminder)
    
    suspend fun markReminderCompleted(reminderId: Long) = 
        reminderDao.markReminderCompleted(reminderId)
    
    // Refueling details operations
    fun getRefuelingDetails(activityId: Long): Flow<RefuelingDetails?> = 
        refuelingDetailsDao.getRefuelingDetails(activityId)
    
    suspend fun insertRefuelingDetails(details: RefuelingDetails) = 
        refuelingDetailsDao.insertRefuelingDetails(details)
    
    suspend fun updateRefuelingDetails(details: RefuelingDetails) = 
        refuelingDetailsDao.updateRefuelingDetails(details)
    
    suspend fun deleteRefuelingDetails(details: RefuelingDetails) = 
        refuelingDetailsDao.deleteRefuelingDetails(details)
    
    // Analytics
    suspend fun calculateCostPerKm(carId: Long): Double {
        val car = carDao.getCarById(carId).first() ?: return 0.0
        val activities = activityDao.getActivitiesByCarId(carId).first()
        
        if (activities.isEmpty()) return 0.0
        
        val latestMileage = activities.maxOfOrNull { it.mileage } ?: car.startMileage
        val totalDistance = latestMileage - car.startMileage
        
        if (totalDistance <= 0) return 0.0
        
        val totalCost = getTotalCostByCarId(carId)
        return totalCost / totalDistance
    }
    
    suspend fun calculateFuelEfficiency(carId: Long): Double? {
        val activities = activityDao.getActivitiesByCarId(carId).first()
        val refuelings = activities.filter { it.type == ActivityType.REFUELING }
            .sortedBy { it.date }
        
        if (refuelings.size < 2) return null
        
        var totalDistance = 0.0
        var totalLiters = 0.0
        
        for (i in 1 until refuelings.size) {
            val distance = refuelings[i].mileage - refuelings[i-1].mileage
            if (distance > 0) {
                totalDistance += distance
                // Get refueling details
                val details = refuelingDetailsDao.getRefuelingDetails(refuelings[i].id).first()
                if (details != null && details.fullTank) {
                    totalLiters += details.liters
                }
            }
        }
        
        return if (totalDistance > 0 && totalLiters > 0) {
            (totalLiters / totalDistance) * 100 // L/100km
        } else null
    }
}