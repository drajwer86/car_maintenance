package com.example.car_maintenance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.car_maintenance.data.model.Car
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCarScreen(
    viewModel: MainViewModel,
    carId: Long,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var car by remember { mutableStateOf<Car?>(null) }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var startMileage by remember { mutableStateOf("") }
    var insuranceExpiryDate by remember { mutableStateOf<Long?>(null) }
    var registrationExpiryDate by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(carId) {
        car = viewModel.repository.getCarById(carId).first()
        car?.let {
            brand = it.brand
            model = it.model
            year = it.year.toString()
            registrationNumber = it.registrationNumber
            vin = it.vin
            startMileage = it.startMileage.toString()
            insuranceExpiryDate = it.insuranceExpiryDate
            registrationExpiryDate = it.registrationExpiryDate
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Car") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand") },
                leadingIcon = {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                leadingIcon = {
                    Icon(Icons.Default.DriveEta, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = year,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                        year = newValue
                    }
                },
                label = { Text("Year") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("YYYY") }
            )
            
            OutlinedTextField(
                value = registrationNumber,
                onValueChange = { registrationNumber = it.uppercase() },
                label = { Text("Registration Number") },
                leadingIcon = {
                    Icon(Icons.Default.Tag, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it.uppercase() },
                label = { Text("VIN") },
                leadingIcon = {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = startMileage,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        startMileage = newValue
                    }
                },
                label = { Text("Start Mileage") },
                leadingIcon = {
                    Icon(Icons.Default.Speed, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        car?.let { originalCar ->
                            val updatedCar = originalCar.copy(
                                brand = brand,
                                model = model,
                                year = year.toIntOrNull() ?: originalCar.year,
                                registrationNumber = registrationNumber,
                                vin = vin,
                                startMileage = startMileage.toDoubleOrNull() ?: originalCar.startMileage,
                                insuranceExpiryDate = insuranceExpiryDate,
                                registrationExpiryDate = registrationExpiryDate
                            )
                            viewModel.updateCar(updatedCar)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = brand.isNotEmpty() && model.isNotEmpty() && 
                         year.isNotEmpty() && year.length == 4 &&
                         registrationNumber.isNotEmpty() && 
                         vin.isNotEmpty() && startMileage.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes")
            }
        }
    }
}