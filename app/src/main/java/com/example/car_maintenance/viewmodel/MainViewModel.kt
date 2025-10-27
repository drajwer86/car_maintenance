package com.example.car_maintenance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.car_maintenance.data.SettingsManager
import com.example.car_maintenance.data.database.AppDatabase
import com.example.car_maintenance.data.model.*
import com.example.car_maintenance.data.repository.CarRepository
import com.example.car_maintenance.utils.BackupData  // ← ADD THIS LINE
import com.example.car_maintenance.utils.BackupUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    val repository = CarRepository(
        database.carDao(),
        database.activityDao(),
        database.activityImageDao(),
        database.reminderDao(),
        database.refuelingDetailsDao()
    )
    
    val settingsManager = SettingsManager(application)
    
    // UI State
    private val _selectedCarId = MutableStateFlow<Long?>(null)
    val selectedCarId: StateFlow<Long?> = _selectedCarId.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Data flows
    val cars: StateFlow<List<Car>> = repository.getAllCars()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val selectedCar: StateFlow<Car?> = selectedCarId
        .flatMapLatest { carId ->
            carId?.let { repository.getCarById(it) } ?: flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    val activities: StateFlow<List<ActivityComplete>> = selectedCarId
        .flatMapLatest { carId ->
            carId?.let { repository.getCompleteActivitiesByCarId(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val reminders: StateFlow<List<Reminder>> = selectedCarId
        .flatMapLatest { carId ->
            carId?.let { repository.getActiveRemindersByCarId(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Analytics
    val totalCost: StateFlow<Double> = combine(
        selectedCarId,
        activities  // Add activities as trigger
    ) { carId, _ ->
        carId?.let { repository.getTotalCostByCarId(it) } ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val costPerKm: StateFlow<Double> = combine(
        selectedCarId,
        activities  // Add activities as trigger
    ) { carId, _ ->
        carId?.let { repository.calculateCostPerKm(it) } ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val fuelEfficiency: StateFlow<Double?> = combine(
        selectedCarId,
        settingsManager.fuelEfficiencyEnabled,
        activities  // Add activities as trigger
    ) { carId, enabled, _ ->
        if (enabled && carId != null) {
            repository.calculateFuelEfficiency(carId)
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    init {
        // Select first car by default
        viewModelScope.launch {
            cars.collect { carsList ->
                if (_selectedCarId.value == null && carsList.isNotEmpty()) {
                    _selectedCarId.value = carsList.first().id
                }
            }
        }
    }
    
    // Car operations
    fun selectCar(carId: Long) {
        _selectedCarId.value = carId
    }
    
    fun addCar(car: Car) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val newCarId = repository.insertCar(car)
                _selectedCarId.value = newCarId
                _successMessage.value = "Car added successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add car: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateCar(car: Car) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateCar(car)
                _successMessage.value = "Car updated successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update car: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteCar(carId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteCar(carId)
                
                // Select another car if available
                val remainingCars = cars.value.filter { it.id != carId }
                _selectedCarId.value = remainingCars.firstOrNull()?.id
                
                _successMessage.value = "Car deleted successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete car: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Activity operations
    fun addActivity(activity: Activity, images: List<ActivityImage> = emptyList(), refuelingDetails: RefuelingDetails? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val activityId = repository.insertActivity(activity)
                
                if (images.isNotEmpty()) {
                    val imagesWithActivityId = images.map { it.copy(activityId = activityId) }
                    repository.insertImages(imagesWithActivityId)
                }
                
                refuelingDetails?.let {
                    repository.insertRefuelingDetails(it.copy(activityId = activityId))
                }
                
                _successMessage.value = "Activity added successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add activity: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateActivity(activity: Activity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateActivity(activity)
                _successMessage.value = "Activity updated successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update activity: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteActivityById(activityId)
                _successMessage.value = "Activity deleted successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete activity: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Reminder operations
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.insertReminder(reminder)
                _successMessage.value = "Reminder added successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateReminder(reminder)
                _successMessage.value = "Reminder updated successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteReminder(reminder)
                _successMessage.value = "Reminder deleted successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun markReminderCompleted(reminderId: Long) {
        viewModelScope.launch {
            try {
                repository.markReminderCompleted(reminderId)
                _successMessage.value = "Reminder marked as completed"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark reminder: ${e.message}"
            }
        }
    }
    
    // Backup operations
    fun createBackup(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val allCars = cars.value
                val allActivities = mutableListOf<Activity>()
                val allImages = mutableListOf<ActivityImage>()
                val allRefuelingDetails = mutableListOf<RefuelingDetails>()
                
                allCars.forEach { car ->
                    val carActivities = repository.getActivitiesByCarId(car.id).first()
                    allActivities.addAll(carActivities)
                    
                    carActivities.forEach { activity ->
                        val images = repository.getImagesByActivityId(activity.id).first()
                        allImages.addAll(images)
                        
                        if (activity.type == ActivityType.REFUELING) {
                            repository.getRefuelingDetails(activity.id).first()?.let {
                                allRefuelingDetails.add(it)
                            }
                        }
                    }
                }
                
                val allReminders = repository.getAllReminders().first()
                
                val backupData = BackupData(
                    cars = allCars,
                    activities = allActivities,
                    activityImages = allImages,
                    reminders = allReminders,
                    refuelingDetails = allRefuelingDetails
                )
                
                val backupFile = BackupUtils.createBackup(getApplication(), backupData)
                
                if (backupFile != null) {
                    _successMessage.value = "Backup created: ${backupFile.name}"
                    onComplete(backupFile)
                } else {
                    _errorMessage.value = "Failed to create backup"
                    onComplete(null)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Backup failed: ${e.message}"
                onComplete(null)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun restoreBackup(backupFile: File, onComplete: (Boolean) -> Unit) {
    viewModelScope.launch {
        try {
            _isLoading.value = true
            
            // Step 1: Restore backup data
            val backupData = BackupUtils.restoreBackup(getApplication(), backupFile)
            
            if (backupData == null) {
                _errorMessage.value = "Failed to read backup file"
                onComplete(false)
                _isLoading.value = false
                return@launch
            }
            
            // Step 2: Validate backup data
            if (backupData.cars.isEmpty()) {
                _errorMessage.value = "Backup file contains no cars"
                onComplete(false)
                _isLoading.value = false
                return@launch
            }
            
            // Step 3: Create a backup of current data before clearing
            val currentCars = cars.value.toList()
            val currentActivities = mutableListOf<Activity>()
            val currentImages = mutableListOf<ActivityImage>()
            val currentReminders = mutableListOf<Reminder>()
            val currentRefuelingDetails = mutableListOf<RefuelingDetails>()
            
            try {
                // Backup current data
                currentCars.forEach { car ->
                    val carActivities = repository.getActivitiesByCarId(car.id).first()
                    currentActivities.addAll(carActivities)
                    
                    carActivities.forEach { activity ->
                        val images = repository.getImagesByActivityId(activity.id).first()
                        currentImages.addAll(images)
                        
                        if (activity.type == ActivityType.REFUELING) {
                            repository.getRefuelingDetails(activity.id).first()?.let {
                                currentRefuelingDetails.add(it)
                            }
                        }
                    }
                }
                val currentRemindersData = repository.getAllReminders().first()
                currentReminders.addAll(currentRemindersData)
                
                // Step 4: Clear existing data
                currentCars.forEach { car ->
                    repository.deleteCar(car.id)
                }
                
                // Step 5: Restore data from backup
                val carIdMapping = mutableMapOf<Long, Long>() // Old ID to New ID
                
                backupData.cars.forEach { car ->
                    val newCarId = repository.insertCar(car.copy(id = 0))
                    carIdMapping[car.id] = newCarId
                }
                
                val activityIdMapping = mutableMapOf<Long, Long>()
                
                backupData.activities.forEach { activity ->
                    val newCarId = carIdMapping[activity.carId] ?: return@forEach
                    val newActivityId = repository.insertActivity(
                        activity.copy(id = 0, carId = newCarId)
                    )
                    activityIdMapping[activity.id] = newActivityId
                }
                
                backupData.activityImages.forEach { image ->
                    val newActivityId = activityIdMapping[image.activityId] ?: return@forEach
                    repository.insertImage(image.copy(id = 0, activityId = newActivityId))
                }
                
                backupData.reminders.forEach { reminder ->
                    val newCarId = carIdMapping[reminder.carId] ?: return@forEach
                    repository.insertReminder(reminder.copy(id = 0, carId = newCarId))
                }
                
                backupData.refuelingDetails.forEach { details ->
                    val newActivityId = activityIdMapping[details.activityId] ?: return@forEach
                    repository.insertRefuelingDetails(
                        details.copy(activityId = newActivityId)
                    )
                }
                
                _successMessage.value = "Backup restored successfully: ${backupData.cars.size} cars, ${backupData.activities.size} activities"
                onComplete(true)
                
            } catch (restoreException: Exception) {
                // Step 6: Rollback - Restore original data if restore failed
                _errorMessage.value = "Restore failed, rolling back changes..."
                
                try {
                    // Clear failed restore data
                    val failedCars = repository.getAllCars().first()
                    failedCars.forEach { car ->
                        repository.deleteCar(car.id)
                    }
                    
                    // Restore original data
                    val originalCarIdMapping = mutableMapOf<Long, Long>()
                    
                    currentCars.forEach { car ->
                        val restoredCarId = repository.insertCar(car.copy(id = 0))
                        originalCarIdMapping[car.id] = restoredCarId
                    }
                    
                    val originalActivityIdMapping = mutableMapOf<Long, Long>()
                    
                    currentActivities.forEach { activity ->
                        val restoredCarId = originalCarIdMapping[activity.carId] ?: return@forEach
                        val restoredActivityId = repository.insertActivity(
                            activity.copy(id = 0, carId = restoredCarId)
                        )
                        originalActivityIdMapping[activity.id] = restoredActivityId
                    }
                    
                    currentImages.forEach { image ->
                        val restoredActivityId = originalActivityIdMapping[image.activityId] ?: return@forEach
                        repository.insertImage(image.copy(id = 0, activityId = restoredActivityId))
                    }
                    
                    currentReminders.forEach { reminder ->
                        val restoredCarId = originalCarIdMapping[reminder.carId] ?: return@forEach
                        repository.insertReminder(reminder.copy(id = 0, carId = restoredCarId))
                    }
                    
                    currentRefuelingDetails.forEach { details ->
                        val restoredActivityId = originalActivityIdMapping[details.activityId] ?: return@forEach
                        repository.insertRefuelingDetails(
                            details.copy(activityId = restoredActivityId)
                        )
                    }
                    
                    _errorMessage.value = "Restore failed but original data was preserved: ${restoreException.message}"
                    
                } catch (rollbackException: Exception) {
                    _errorMessage.value = "Critical error: Restore failed and rollback failed. Please restart the app: ${rollbackException.message}"
                }
                
                onComplete(false)
            }
            
        } catch (e: Exception) {
            _errorMessage.value = "Restore failed: ${e.message}"
            e.printStackTrace()
            onComplete(false)
        } finally {
            _isLoading.value = false
        }
    }
}
    
    // Message handling
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}