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
import androidx.compose.ui.unit.dp
import com.example.car_maintenance.ui.components.CarCard
import com.example.car_maintenance.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarsScreen(
    viewModel: MainViewModel,
    onNavigateToAddCar: () -> Unit,
    onNavigateToEditCar: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val cars by viewModel.cars.collectAsState()
    val selectedCarId by viewModel.selectedCarId.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var carToDelete by remember { mutableStateOf<Long?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cars") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCar
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add car")
            }
        }
    ) { paddingValues ->
        if (cars.isEmpty()) {
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
                    Button(onClick = onNavigateToAddCar) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = cars,
                    key = { it.id }
                ) { car ->
                    CarCard(
                        car = car,
                        isSelected = car.id == selectedCarId,
                        onClick = {
                            // Select car and navigate back immediately
                            viewModel.selectCar(car.id)
                            onNavigateBack()
                        },
                        onEdit = { onNavigateToEditCar(car.id) },
                        onDelete = {
                            carToDelete = car.id
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && carToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                carToDelete = null
            },
            title = { Text("Delete Car") },
            text = { Text("Are you sure you want to delete this car? All associated activities and data will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        carToDelete?.let { viewModel.deleteCar(it) }
                        showDeleteDialog = false
                        carToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        carToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}