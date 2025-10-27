package com.example.car_maintenance.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.car_maintenance.data.model.ActivityType
import com.example.car_maintenance.ui.components.ActivityCard
import com.example.car_maintenance.ui.components.ActivityTypeIcon
import com.example.car_maintenance.ui.components.AppDatePickerDialog
import com.example.car_maintenance.utils.CurrencyUtils
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.utils.ExportUtils
import com.example.car_maintenance.utils.UnitUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToActivityDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val selectedCar by viewModel.selectedCar.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val currency by viewModel.settingsManager.currency.collectAsState(initial = "USD")
    val distanceUnit by viewModel.settingsManager.distanceUnit.collectAsState(
        initial = UnitUtils.DistanceUnit.KILOMETERS
    )
    
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedType by remember { mutableStateOf<ActivityType?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showTypeFilterDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val filteredActivities = remember(activities, startDate, endDate, selectedType) {
        activities.filter { activityComplete ->
            val activity = activityComplete.activity
            val dateMatch = if (startDate != null && endDate != null) {
                activity.date >= startDate!! && activity.date <= endDate!!
            } else true
            
            val typeMatch = selectedType?.let { activity.type == it } ?: true
            
            dateMatch && typeMatch
        }
    }
    
    val totalCost = remember(filteredActivities) {
        filteredActivities.sumOf { it.activity.cost }
    }
    
    val costByType = remember(filteredActivities) {
        filteredActivities.groupBy { it.activity.type }
            .mapValues { (_, activities) ->
                activities.sumOf { it.activity.cost }
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export to PDF") },
                            onClick = {
                                scope.launch {
                                    selectedCar?.let { car ->
                                        val file = ExportUtils.exportToPdf(
                                            context,
                                            "${car.brand} ${car.model}",
                                            filteredActivities.map { it.activity },
                                            totalCost,
                                            costByType,
                                            currency,
                                            startDate,
                                            endDate
                                        )
                                        file?.let { shareFile(context, it, "application/pdf") }
                                    }
                                    showExportMenu = false
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to Excel") },
                            onClick = {
                                scope.launch {
                                    selectedCar?.let { car ->
                                        val file = ExportUtils.exportToExcel(
                                            context,
                                            "${car.brand} ${car.model}",
                                            filteredActivities.map { it.activity },
                                            totalCost,
                                            costByType,
                                            currency
                                        )
                                        file?.let { shareFile(context, it, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
                                    }
                                    showExportMenu = false
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.TableChart, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to CSV") },
                            onClick = {
                                scope.launch {
                                    selectedCar?.let { car ->
                                        val file = ExportUtils.exportToCsv(
                                            context,
                                            "${car.brand} ${car.model}",
                                            filteredActivities.map { it.activity },
                                            currency
                                        )
                                        file?.let { shareFile(context, it, "text/csv") }
                                    }
                                    showExportMenu = false
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Filters",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Range Filter Button
                            OutlinedButton(
                                onClick = { showDateRangeDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (startDate != null && endDate != null)
                                        "Custom Range"
                                    else
                                        "All Time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            // Type Filter Button
                            OutlinedButton(
                                onClick = { showTypeFilterDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedType?.getDisplayName() ?: "All Types",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            }
                        }
                        
                        // Show active filters
                        if (startDate != null || selectedType != null) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            if (startDate != null && endDate != null) {
                                Text(
                                    text = "Period: ${DateUtils.formatDate(startDate!!)} - ${DateUtils.formatDate(endDate!!)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (selectedType != null) {
                                Text(
                                    text = "Type: ${selectedType!!.getDisplayName()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            TextButton(
                                onClick = {
                                    startDate = null
                                    endDate = null
                                    selectedType = null
                                }
                            ) {
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
            }
            
            // Summary
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Activities:")
                            Text(
                                text = filteredActivities.size.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Cost:")
                            Text(
                                text = CurrencyUtils.formatAmount(totalCost, currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (costByType.isNotEmpty()) {
                            Divider()
                            
                            Text(
                                text = "Cost by Type",
                                style = MaterialTheme.typography.titleSmall
                            )
                            
                            costByType.forEach { (type, cost) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ActivityTypeIcon(
                                            activityType = type,
                                            size = 20.dp
                                        )
                                        Text(
                                            text = type.getDisplayName(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = CurrencyUtils.formatAmount(cost, currency),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Activities List
            item {
                Text(
                    text = "Activities (${filteredActivities.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            if (filteredActivities.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No activities found")
                            if (startDate != null || selectedType != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Try adjusting your filters",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredActivities,
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
    
    // Date Range Dialog
    if (showDateRangeDialog) {
        AlertDialog(
            onDismissRequest = { showDateRangeDialog = false },
            title = { Text("Select Date Range") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Choose a time period for the report:")
                    
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (startDate != null)
                                "From: ${DateUtils.formatDate(startDate!!)}"
                            else
                                "Select Start Date"
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = startDate != null
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (endDate != null)
                                "To: ${DateUtils.formatDate(endDate!!)}"
                            else
                                "Select End Date"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDateRangeDialog = false },
                    enabled = startDate != null && endDate != null
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        startDate = null
                        endDate = null
                        showDateRangeDialog = false
                    }
                ) {
                                        Text("Clear")
                }
            }
        )
    }
    
    // Type Filter Dialog
    if (showTypeFilterDialog) {
        AlertDialog(
            onDismissRequest = { showTypeFilterDialog = false },
            title = { Text("Filter by Activity Type") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Types option
                    OutlinedCard(
                        onClick = {
                            selectedType = null
                            showTypeFilterDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selectedType == null)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
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
                                Icon(
                                    Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "All Types",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (selectedType == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Individual activity types
                    ActivityType.values().forEach { type ->
                        OutlinedCard(
                            onClick = {
                                selectedType = type
                                showTypeFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selectedType == type)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
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
                                        activityType = type,
                                        size = 40.dp
                                    )
                                    Text(
                                        text = type.getDisplayName(),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                if (selectedType == type) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Start Date Picker
    if (showStartDatePicker) {
        AppDatePickerDialog(
            initialDate = startDate ?: System.currentTimeMillis(),
            onDateSelected = { date ->
                startDate = DateUtils.getStartOfDay(date)
                // Clear end date if it's before start date
                if (endDate != null && endDate!! < startDate!!) {
                    endDate = null
                }
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        AppDatePickerDialog(
            initialDate = endDate ?: (startDate ?: System.currentTimeMillis()),
            onDateSelected = { date ->
                endDate = DateUtils.getEndOfDay(date)
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

// Helper function to share exported files
private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Car Maintenance Report")
            putExtra(Intent.EXTRA_TEXT, "Car maintenance report generated on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "Failed to share file: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}