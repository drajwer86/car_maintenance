package com.example.car_maintenance.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.car_maintenance.data.dao.*
import com.example.car_maintenance.data.model.*

@Database(
    entities = [
        Car::class,
        Activity::class,
        ActivityImage::class,
        Reminder::class,
        RefuelingDetails::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun activityDao(): ActivityDao
    abstract fun activityImageDao(): ActivityImageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun refuelingDetailsDao(): RefuelingDetailsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_maintenance_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}