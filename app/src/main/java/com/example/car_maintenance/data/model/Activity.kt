package com.example.car_maintenance.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val carId: Long,
    val type: ActivityType,
    val date: Long, // Timestamp
    val mileage: Double,
    val cost: Double,
    val currency: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActivityType {
    REFUELING,
    MECHANIC_VISIT,
    OIL_CHANGE,
    TIRE_SWITCH,
    CAR_WASH,
    CAR_ACCIDENT,
    CUSTOM;
    
    fun getDisplayName(): String {
        return when (this) {
            REFUELING -> "Refueling"
            MECHANIC_VISIT -> "Mechanic Visit"
            OIL_CHANGE -> "Oil Change"
            TIRE_SWITCH -> "Tire Switch"
            CAR_WASH -> "Car Wash"
            CAR_ACCIDENT -> "Car Accident"
            CUSTOM -> "Custom Activity"
        }
    }
    
    fun getIcon(): String {
        return when (this) {
            REFUELING -> "⛽"
            MECHANIC_VISIT -> "🔧"
            OIL_CHANGE -> "🛢️"
            TIRE_SWITCH -> "🛞"
            CAR_WASH -> "🚿"
            CAR_ACCIDENT -> "⚠️"
            CUSTOM -> "📝"
        }
    }
}