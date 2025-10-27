package com.example.car_maintenance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.car_maintenance.data.model.ActivityType
import com.example.car_maintenance.ui.components.ActivityCard
import com.example.car_maintenance.ui.components.StatCard
import com.example.car_maintenance.utils.CurrencyUtils
import com.example.car_maintenance.utils.UnitUtils
import com.example.car_maintenance.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAddActivity: () -> Unit,
    onNavigateToActivityDetail: (Long) -> Unit,
    onNavigateToCars: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val selectedCar by viewModel.selectedCar.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()
    val costPerKm by viewModel.costPerKm.collectAsState()
    val fuelEfficiency by viewModel.fuelEfficiency.collectAsState()
    
    val currency by viewModel.settingsManager.currency.collectAsState(initial = "USD")
    val distanceUnit by viewModel.settingsManager.distanceUnit.collectAsState(
        initial = UnitUtils.DistanceUnit.KILOMETERS
    )
    val volumeUnit by viewModel.settingsManager.volumeUnit.collectAsState(
        initial = UnitUtils.VolumeUnit.LITERS
    )
    val fuelEfficiencyEnabled by viewModel.settingsManager.fuelEfficiencyEnabled.collectAsState(
        initial = true
    )
    
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Car Maintenance",
                            style = MaterialTheme.typography.titleLarge
                        )
                        selectedCar?.let { car ->
                            Text(
                                text = "${car.brand} ${car.model}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("My Cars") },
                            onClick = {
                                showMenu = false
                                onNavigateToCars()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reports") },
                            onClick = {
                                showMenu = false
                                onNavigateToReports()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Assessment, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reminders") },
                            onClick = {
                                showMenu = false
                                onNavigateToReminders()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCar != null) {
                FloatingActionButton(
                    onClick = onNavigateToAddActivity,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add activity"
                    )
                }
            }
        }
    ) { paddingValues ->
        if (selectedCar == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No cars added yet",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(onClick = onNavigateToCars) {
                        Text("Add Your First Car")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics
                item {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Total Cost",
                            value = CurrencyUtils.formatAmount(totalCost, currency),
                            icon = Icons.Default.AttachMoney,
                            modifier = Modifier.weight(1f)
                        )
                        
                        StatCard(
                            title = "Cost/${distanceUnit.getLabel()}",
                            value = CurrencyUtils.formatAmount(costPerKm, currency),
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (fuelEfficiencyEnabled && fuelEfficiency != null) {
                    item {
                        StatCard(
                            title = "Fuel Efficiency",
                            value = UnitUtils.calculateFuelEfficiency(
                                100.0,
                                fuelEfficiency!!,
                                distanceUnit,
                                volumeUnit
                            ),
                            icon = Icons.Default.LocalGasStation,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Activities
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activities",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToReports) {
                            Text("View All")
                        }
                    }
                }
                
                if (activities.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "No activities yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Add your first maintenance activity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = activities.take(10),
                        key = { it.activity.id }
                    ) { activity ->
                        ActivityCard(
                            activity = activity,
                            currency = currency,
                            distanceUnit = distanceUnit,
                            onClick = {
                                onNavigateToActivityDetail(activity.activity.id)
                            }
                        )
                    }
                }
            }
        }
    }
}