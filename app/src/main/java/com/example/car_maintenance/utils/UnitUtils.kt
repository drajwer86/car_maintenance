package com.example.car_maintenance.utils

object UnitUtils {
    
    enum class DistanceUnit {
        KILOMETERS,
        MILES;
        
        fun getLabel(): String = when (this) {
            KILOMETERS -> "km"
            MILES -> "mi"
        }
        
        fun getLongLabel(): String = when (this) {
            KILOMETERS -> "kilometers"
            MILES -> "miles"
        }
    }
    
    enum class VolumeUnit {
        LITERS,
        GALLONS;
        
        fun getLabel(): String = when (this) {
            LITERS -> "L"
            GALLONS -> "gal"
        }
        
        fun getLongLabel(): String = when (this) {
            LITERS -> "liters"
            GALLONS -> "gallons"
        }
    }
    
    fun convertKmToMiles(km: Double): Double = km * 0.621371
    
    fun convertMilesToKm(miles: Double): Double = miles * 1.60934
    
    fun convertLitersToGallons(liters: Double): Double = liters * 0.264172
    
    fun convertGallonsToLiters(gallons: Double): Double = gallons * 3.78541
    
    fun formatDistance(distance: Double, unit: DistanceUnit): String {
        return "${"%.1f".format(distance)} ${unit.getLabel()}"
    }
    
    fun formatVolume(volume: Double, unit: VolumeUnit): String {
        return "${"%.2f".format(volume)} ${unit.getLabel()}"
    }
    
    fun calculateFuelEfficiency(
        distance: Double,
        volume: Double,
        distanceUnit: DistanceUnit,
        volumeUnit: VolumeUnit
    ): String {
        return when {
            distanceUnit == DistanceUnit.KILOMETERS && volumeUnit == VolumeUnit.LITERS -> {
                val consumption = (volume / distance) * 100
                "${"%.2f".format(consumption)} L/100km"
            }
            distanceUnit == DistanceUnit.MILES && volumeUnit == VolumeUnit.GALLONS -> {
                val mpg = distance / volume
                "${"%.2f".format(mpg)} MPG"
            }
            else -> {
                // Convert to L/100km for mixed units
                val distanceKm = if (distanceUnit == DistanceUnit.MILES) {
                    convertMilesToKm(distance)
                } else distance
                
                val volumeLiters = if (volumeUnit == VolumeUnit.GALLONS) {
                    convertGallonsToLiters(volume)
                } else volume
                
                val consumption = (volumeLiters / distanceKm) * 100
                "${"%.2f".format(consumption)} L/100km"
            }
        }
    }
}