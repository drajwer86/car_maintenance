package com.example.car_maintenance.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.car_maintenance.data.model.Activity
import com.example.car_maintenance.data.model.ActivityImage
import com.example.car_maintenance.data.model.ActivityType
import com.example.car_maintenance.data.model.RefuelingDetails
import com.example.car_maintenance.ui.components.ActivityTypeIcon
import com.example.car_maintenance.ui.components.AppDatePickerDialog
import com.example.car_maintenance.utils.CurrencyUtils
import com.example.car_maintenance.utils.ImageUtils
import com.example.car_maintenance.utils.UnitUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val selectedCar by viewModel.selectedCar.collectAsState()
    val currency by viewModel.settingsManager.currency.collectAsState(initial = "USD")
    val distanceUnit by viewModel.settingsManager.distanceUnit.collectAsState(
        initial = UnitUtils.DistanceUnit.KILOMETERS
    )
    val volumeUnit by viewModel.settingsManager.volumeUnit.collectAsState(
        initial = UnitUtils.VolumeUnit.LITERS
    )
    
    var selectedType by remember { mutableStateOf(ActivityType.REFUELING) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var mileage by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    // Refueling specific
    var liters by remember { mutableStateOf("") }
    var pricePerLiter by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("Gasoline") }
    var fullTank by remember { mutableStateOf(true) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeSelector by remember { mutableStateOf(false) }
    
    // Camera URI state
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        imageUris = imageUris + uris
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            imageUris = imageUris + tempCameraUri!!
            tempCameraUri = null
        }
    }
    
    // Function to create a temporary file for camera
    fun createTempImageFile(): Uri {
        val timeStamp = System.currentTimeMillis()
        val imageFile = File(context.cacheDir, "temp_camera_${timeStamp}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Activity") },
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
            // Activity Type Selector
            Text(
                text = "Activity Type",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedCard(
                onClick = { showTypeSelector = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActivityTypeIcon(
                            activityType = selectedType,
                            size = 40.dp
                        )
                        Text(
                            text = selectedType.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select type"
                    )
                }
            }
            
            // Date
            Text(
                text = "Date",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(com.example.car_maintenance.utils.DateUtils.formatDate(date))
            }
            
            // Mileage
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("Mileage (${distanceUnit.getLabel()})") },
                leadingIcon = {
                    Icon(Icons.Default.Speed, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Cost
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it },
                label = { Text("Cost (${CurrencyUtils.getCurrencyByCode(currency).symbol})") },
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Refueling specific fields
            if (selectedType == ActivityType.REFUELING) {
                Divider()
                
                Text(
                    text = "Refueling Details",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = liters,
                    onValueChange = { liters = it },
                    label = { Text("Volume (${volumeUnit.getLabel()})") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = pricePerLiter,
                    onValueChange = { pricePerLiter = it },
                    label = { Text("Price per ${volumeUnit.getLabel()}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text("Fuel Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Full Tank")
                    Switch(
                        checked = fullTank,
                        onCheckedChange = { fullTank = it }
                    )
                }
            }
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            // Images
            Text(
                text = "Photos",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery")
                }
                
                OutlinedButton(
                    onClick = {
                        tempCameraUri = createTempImageFile()
                        cameraLauncher.launch(tempCameraUri!!)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Camera")
                }
            }
            
            if (imageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imageUris.toList()) { uri ->
                        Card(
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        imageUris = imageUris.filter { it != uri }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = {
                    scope.launch {
                        selectedCar?.let { car ->
                            val activity = Activity(
                                carId = car.id,
                                type = selectedType,
                                date = date,
                                mileage = mileage.toDoubleOrNull() ?: 0.0,
                                cost = cost.toDoubleOrNull() ?: 0.0,
                                currency = currency,
                                notes = notes
                            )
                            
                            val images = mutableListOf<ActivityImage>()
                            imageUris.forEach { uri ->
                                ImageUtils.saveImage(context, uri, car.id, 0)?.let { (path, thumbPath) ->
                                    images.add(
                                        ActivityImage(
                                            activityId = 0,
                                            filePath = path,
                                            thumbnailPath = thumbPath
                                        )
                                    )
                                }
                            }
                            
                            val refuelingDetails = if (selectedType == ActivityType.REFUELING) {
                                RefuelingDetails(
                                    activityId = 0,
                                    liters = liters.toDoubleOrNull() ?: 0.0,
                                    pricePerLiter = pricePerLiter.toDoubleOrNull() ?: 0.0,
                                    fuelType = fuelType,
                                    fullTank = fullTank
                                )
                            } else null
                            
                            viewModel.addActivity(activity, images, refuelingDetails)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = mileage.isNotEmpty() && cost.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Activity")
            }
        }
    }
    
    // Type Selector Dialog
    if (showTypeSelector) {
        AlertDialog(
            onDismissRequest = { showTypeSelector = false },
            title = { Text("Select Activity Type") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityType.values().forEach { type ->
                        OutlinedCard(
                            onClick = {
                                selectedType = type
                                showTypeSelector = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActivityTypeIcon(
                                    activityType = type,
                                    size = 40.dp
                                )
                                Text(
                                    text = type.getDisplayName(),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Date Picker
    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = date,
            onDateSelected = { selectedDate ->
                date = selectedDate
            },
            onDismiss = { showDatePicker = false }
        )
    }
}