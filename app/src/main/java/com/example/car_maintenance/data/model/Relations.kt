package com.example.car_maintenance.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class CarWithActivities(
    @Embedded val car: Car,
    @Relation(
        parentColumn = "id",
        entityColumn = "carId"
    )
    val activities: List<Activity>
)

data class ActivityWithImages(
    @Embedded val activity: Activity,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val images: List<ActivityImage>
)

data class ActivityComplete(
    @Embedded val activity: Activity,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val images: List<ActivityImage>,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val refuelingDetails: RefuelingDetails?
)