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
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.utils.ImageUtils
import com.example.car_maintenance.utils.UnitUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivityScreen(
    viewModel: MainViewModel,
    activityId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
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
    
    var selectedType by remember { mutableStateOf(ActivityType.REFUELING) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var mileage by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var existingImages by remember { mutableStateOf<List<ActivityImage>>(emptyList()) }
    var newImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    // Refueling specific
    var liters by remember { mutableStateOf("") }
    var pricePerLiter by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("Gasoline") }
    var fullTank by remember { mutableStateOf(true) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // Initialize data
    LaunchedEffect(activityComplete) {
        activityComplete?.let { complete ->
            if (!isInitialized) {
                selectedType = complete.activity.type
                date = complete.activity.date
                mileage = complete.activity.mileage.toString()
                cost = complete.activity.cost.toString()
                notes = complete.activity.notes
                existingImages = complete.images
                
                complete.refuelingDetails?.let { details ->
                    liters = details.liters.toString()
                    pricePerLiter = details.pricePerLiter.toString()
                    fuelType = details.fuelType
                    fullTank = details.fullTank
                }
                
                isInitialized = true
            }
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        newImageUris = newImageUris + uris
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            newImageUris = newImageUris + tempCameraUri!!
            tempCameraUri = null
        }
    }
    
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
                title = { Text("Edit Activity") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (activityComplete == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Activity Type (Read-only display)
                Text(
                    text = "Activity Type",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                    Text(DateUtils.formatDate(date))
                }
                
                // Mileage
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            mileage = newValue
                        }
                    },
                    label = { Text("Mileage (${distanceUnit.getLabel()})") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Cost
                OutlinedTextField(
                    value = cost,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            cost = newValue
                        }
                    },
                    label = { Text("Cost (${CurrencyUtils.getCurrencyByCode(currency).symbol})") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
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
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                liters = newValue
                            }
                        },
                        label = { Text("Volume (${volumeUnit.getLabel()})") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = pricePerLiter,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                pricePerLiter = newValue
                            }
                        },
                        label = { Text("Price per ${volumeUnit.getLabel()}") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
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
                
                // Existing images
                if (existingImages.isNotEmpty()) {
                    Text(
                        text = "Existing Photos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(existingImages.toList()) { image ->
                            Card(
                                modifier = Modifier.size(100.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = image.filePath,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            existingImages = existingImages.filter { it.id != image.id }
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
                
                // New images
                if (newImageUris.isNotEmpty()) {
                    Text(
                        text = "New Photos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(newImageUris.toList()) { uri ->
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
                                            newImageUris = newImageUris.filter { it != uri }
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
                            activityComplete?.let { complete ->
                                // Update activity
                                val updatedActivity = complete.activity.copy(
                                    date = date,
                                                                        mileage = mileage.toDoubleOrNull() ?: complete.activity.mileage,
                                    cost = cost.toDoubleOrNull() ?: complete.activity.cost,
                                    notes = notes
                                )
                                viewModel.updateActivity(updatedActivity)
                                
                                // Delete removed existing images
                                complete.images.forEach { image ->
                                    if (!existingImages.contains(image)) {
                                        viewModel.repository.deleteImage(image)
                                    }
                                }
                                
                                // Add new images
                                val newImages = mutableListOf<ActivityImage>()
                                newImageUris.forEach { uri ->
                                    ImageUtils.saveImage(context, uri, complete.activity.carId, activityId)?.let { (path, thumbPath) ->
                                        newImages.add(
                                            ActivityImage(
                                                activityId = activityId,
                                                filePath = path,
                                                thumbnailPath = thumbPath
                                            )
                                        )
                                    }
                                }
                                if (newImages.isNotEmpty()) {
                                    viewModel.repository.insertImages(newImages)
                                }
                                
                                // Update refueling details if applicable
                                if (selectedType == ActivityType.REFUELING) {
                                    val refuelingDetails = RefuelingDetails(
                                        activityId = activityId,
                                        liters = liters.toDoubleOrNull() ?: 0.0,
                                        pricePerLiter = pricePerLiter.toDoubleOrNull() ?: 0.0,
                                        fuelType = fuelType,
                                        fullTank = fullTank
                                    )
                                    viewModel.repository.insertRefuelingDetails(refuelingDetails)
                                }
                                
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = mileage.isNotEmpty() && cost.isNotEmpty()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }
        }
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