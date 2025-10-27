package com.example.car_maintenance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.car_maintenance.R
import com.example.car_maintenance.data.model.ActivityType
import com.example.car_maintenance.ui.theme.*

@Composable
fun ActivityTypeIcon(
    activityType: ActivityType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val (icon, color) = when (activityType) {
        ActivityType.REFUELING -> Pair(R.drawable.ic_fuel, RefuelingColor)
        ActivityType.MECHANIC_VISIT -> Pair(R.drawable.ic_mechanic, MechanicColor)
        ActivityType.OIL_CHANGE -> Pair(R.drawable.ic_oil, OilChangeColor)
        ActivityType.TIRE_SWITCH -> Pair(R.drawable.ic_tire, TireSwitchColor)
        ActivityType.CAR_WASH -> Pair(R.drawable.ic_wash, CarWashColor)
        ActivityType.CAR_ACCIDENT -> Pair(R.drawable.ic_accident, AccidentColor)
        ActivityType.CUSTOM -> Pair(R.drawable.ic_custom, CustomColor)
    }
    
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = activityType.getDisplayName(),
            tint = color,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}