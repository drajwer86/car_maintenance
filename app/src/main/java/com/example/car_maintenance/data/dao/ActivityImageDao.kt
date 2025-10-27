package com.example.car_maintenance.data.dao

import androidx.room.*
import com.example.car_maintenance.data.model.ActivityImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityImageDao {
    @Query("SELECT * FROM activity_images WHERE activityId = :activityId ORDER BY createdAt")
    fun getImagesByActivityId(activityId: Long): Flow<List<ActivityImage>>
    
    @Insert
    suspend fun insertImage(image: ActivityImage): Long
    
    @Insert
    suspend fun insertImages(images: List<ActivityImage>)
    
    @Delete
    suspend fun deleteImage(image: ActivityImage)
    
    @Query("DELETE FROM activity_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Long)
    
    @Query("DELETE FROM activity_images WHERE activityId = :activityId")
    suspend fun deleteImagesByActivityId(activityId: Long)
}