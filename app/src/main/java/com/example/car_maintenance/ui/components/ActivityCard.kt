package com.example.car_maintenance.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.car_maintenance.R
import com.example.car_maintenance.data.model.ActivityComplete
import com.example.car_maintenance.utils.CurrencyUtils
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.utils.UnitUtils

@Composable
fun ActivityCard(
    activity: ActivityComplete,
    currency: String,
    distanceUnit: UnitUtils.DistanceUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActivityTypeIcon(
                        activityType = activity.activity.type,
                        size = 40.dp
                    )
                    
                    Column {
                        Text(
                            text = activity.activity.type.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = DateUtils.formatDate(activity.activity.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = CurrencyUtils.formatAmount(activity.activity.cost, currency),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (activity.activity.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activity.activity.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Mileage",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = UnitUtils.formatDistance(activity.activity.mileage, distanceUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (activity.images.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(
                            onClick = {
                                // Open first image in fullscreen
                                selectedImagePath = activity.images.first().filePath
                            }
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = "Images",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${activity.images.size} photo${if (activity.images.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Fullscreen Image Dialog
    selectedImagePath?.let { imagePath ->
        FullscreenImageDialog(
            imagePath = imagePath,
            onDismiss = { selectedImagePath = null }
        )
    }
}