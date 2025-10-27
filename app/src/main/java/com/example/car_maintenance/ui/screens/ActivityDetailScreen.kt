package com.example.car_maintenance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.car_maintenance.data.model.ActivityType
import com.example.car_maintenance.ui.components.ActivityTypeIcon
import com.example.car_maintenance.ui.components.FullscreenImageDialog
import com.example.car_maintenance.utils.CurrencyUtils
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.utils.UnitUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    viewModel: MainViewModel,
    activityId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit  // Add this parameter
) {
    val scope = rememberCoroutineScope()
    
    val activityComplete by viewModel.repository.getCompleteActivityById(activityId)
        .collectAsState(initial = null)
    val currency by viewModel.settingsManager.currency.collectAsState(initial = "USD")
    val distanceUnit by viewModel.settingsManager.distanceUnit.collectAsState(
        initial = UnitUtils.DistanceUnit.KILOMETERS
    )
    val volumeUnit by viewModel.settingsManager.volumeUnit.collectAsState(
        initial = UnitUtils.VolumeUnit.LITERS
    )
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add Edit button
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // ... rest of the ActivityDetailScreen code remains the same
        activityComplete?.let { complete ->
            val activity = complete.activity
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ... all the existing content remains the same
                
                // Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActivityTypeIcon(
                            activityType = activity.type,
                            size = 56.dp
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.type.getDisplayName(),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = DateUtils.formatDateTime(activity.date),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = CurrencyUtils.formatAmount(activity.cost, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Details card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mileage:")
                            Text(
                                text = UnitUtils.formatDistance(activity.mileage, distanceUnit),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cost:")
                            Text(
                                text = CurrencyUtils.formatAmount(activity.cost, currency),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        if (activity.type == ActivityType.REFUELING && complete.refuelingDetails != null) {
                            Divider()
                            
                            Text(
                                text = "Refueling Details",
                                style = MaterialTheme.typography.titleSmall
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Volume:")
                                Text(
                                    text = UnitUtils.formatVolume(
                                        complete.refuelingDetails.liters,
                                        volumeUnit
                                    )
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Price per ${volumeUnit.getLabel()}:")
                                Text(
                                    text = CurrencyUtils.formatAmount(
                                        complete.refuelingDetails.pricePerLiter,
                                        currency
                                    )
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fuel Type:")
                                Text(complete.refuelingDetails.fuelType)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Full Tank:")
                                Text(
                                    if (complete.refuelingDetails.fullTank) "Yes" else "No"
                                )
                            }
                        }
                    }
                }
                
                // Notes
                if (activity.notes.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = activity.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Images - Clickable
                if (complete.images.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Photos (${complete.images.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Text(
                                text = "Tap on image to view fullscreen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(complete.images) { image ->
                                    Card(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clickable {
                                                selectedImagePath = image.filePath
                                            }
                                    ) {
                                        AsyncImage(
                                            model = image.filePath,
                                            contentDescription = "Activity photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Metadata
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Metadata",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Created:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                                                text = DateUtils.formatDateTime(activity.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete this activity? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteActivity(activityId)
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Fullscreen Image Dialog
    selectedImagePath?.let { imagePath ->
        FullscreenImageDialog(
            imagePath = imagePath,
            onDismiss = { selectedImagePath = null }
        )
    }
}