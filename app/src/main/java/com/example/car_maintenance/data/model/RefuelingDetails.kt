package com.example.car_maintenance.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "refueling_details",
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RefuelingDetails(
    @PrimaryKey
    val activityId: Long,
    val liters: Double, // Will be gallons for imperial
    val pricePerLiter: Double,
    val fuelType: String = "Gasoline",
    val fullTank: Boolean = true
)