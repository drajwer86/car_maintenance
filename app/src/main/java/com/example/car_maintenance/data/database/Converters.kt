package com.example.car_maintenance.data.database

import androidx.room.TypeConverter
import com.example.car_maintenance.data.model.ActivityType

class Converters {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String {
        return value.name
    }
    
    @TypeConverter
    fun toActivityType(value: String): ActivityType {
        return ActivityType.valueOf(value)
    }
}