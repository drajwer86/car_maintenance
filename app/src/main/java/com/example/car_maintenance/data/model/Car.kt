package com.example.car_maintenance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val registrationNumber: String,
    val vin: String,
    val startMileage: Double,
    val insuranceExpiryDate: Long? = null, // Timestamp
    val registrationExpiryDate: Long? = null, // Timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)