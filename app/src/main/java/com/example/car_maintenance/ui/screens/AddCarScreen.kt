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
import com.example.car_maintenance.ui.components.AppDatePickerDialog
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var startMileage by remember { mutableStateOf("") }
    var insuranceExpiryDate by remember { mutableStateOf<Long?>(null) }
    var registrationExpiryDate by remember { mutableStateOf<Long?>(null) }
    
    var showInsuranceDatePicker by remember { mutableStateOf(false) }
    var showRegistrationDatePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Car") },
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
            
            // Year field with error state
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
    isError = year.isNotEmpty() && year.length != 4,
    supportingText = {
        if (year.isNotEmpty() && year.length != 4) {
            Text(
                "Year must be 4 digits",
                color = MaterialTheme.colorScheme.error
            )
        }
    },
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
    supportingText = {
        Text("Automatically converted to uppercase")
    },
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
    supportingText = {
        Text("Vehicle Identification Number (auto-uppercase)")
    },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true
)
            
            OutlinedTextField(
                value = startMileage,
                onValueChange = { newValue ->
                    // Only allow digits and decimal point
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        startMileage = newValue
                    }
                },
                label = { Text("Current Mileage") },
                leadingIcon = {
                    Icon(Icons.Default.Speed, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Divider()
            
            Text(
                text = "Optional Information",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Insurance Expiry
            OutlinedButton(
                onClick = { showInsuranceDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (insuranceExpiryDate != null)
                        "Insurance Expires: ${DateUtils.formatDate(insuranceExpiryDate!!)}"
                    else
                        "Set Insurance Expiry Date"
                )
            }
            
            if (insuranceExpiryDate != null) {
                TextButton(
                    onClick = { insuranceExpiryDate = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Insurance Date")
                }
            }
            
            // Registration Expiry
            OutlinedButton(
                onClick = { showRegistrationDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (registrationExpiryDate != null)
                        "Registration Expires: ${DateUtils.formatDate(registrationExpiryDate!!)}"
                    else
                        "Set Registration Expiry Date"
                )
            }
            
            if (registrationExpiryDate != null) {
                TextButton(
                    onClick = { registrationExpiryDate = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Registration Date")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        val car = Car(
                            brand = brand,
                            model = model,
                            year = year.toIntOrNull() ?: 0,
                            registrationNumber = registrationNumber,
                            vin = vin,
                            startMileage = startMileage.toDoubleOrNull() ?: 0.0,
                            insuranceExpiryDate = insuranceExpiryDate,
                            registrationExpiryDate = registrationExpiryDate
                        )
                        viewModel.addCar(car)
                        onNavigateBack()
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
                Text("Save Car")
            }
        }
    }
    
    // Insurance Date Picker Dialog
    if (showInsuranceDatePicker) {
        AppDatePickerDialog(
            initialDate = insuranceExpiryDate ?: System.currentTimeMillis(),
            onDateSelected = { selectedDate ->
                insuranceExpiryDate = selectedDate
            },
            onDismiss = { showInsuranceDatePicker = false }
        )
    }
    
    // Registration Date Picker Dialog
    if (showRegistrationDatePicker) {
        AppDatePickerDialog(
            initialDate = registrationExpiryDate ?: System.currentTimeMillis(),
            onDateSelected = { selectedDate ->
                registrationExpiryDate = selectedDate
            },
            onDismiss = { showRegistrationDatePicker = false }
        )
    }
}