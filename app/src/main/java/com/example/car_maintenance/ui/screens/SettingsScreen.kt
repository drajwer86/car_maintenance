package com.example.car_maintenance.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.car_maintenance.data.SettingsManager
import com.example.car_maintenance.utils.CurrencyUtils
import com.example.car_maintenance.utils.UnitUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val theme by viewModel.settingsManager.theme.collectAsState(
        initial = SettingsManager.Theme.SYSTEM
    )
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
    val serviceRemindersEnabled by viewModel.settingsManager.serviceRemindersEnabled.collectAsState(
        initial = true
    )
    val insuranceRemindersEnabled by viewModel.settingsManager.insuranceRemindersEnabled.collectAsState(
        initial = true
    )
    val autoBackupEnabled by viewModel.settingsManager.autoBackupEnabled.collectAsState(
        initial = false
    )
    val autoBackupFrequency by viewModel.settingsManager.autoBackupFrequency.collectAsState(
        initial = SettingsManager.BackupFrequency.WEEKLY
    )
    
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }
    var isBackupInProgress by remember { mutableStateOf(false) }
    var isRestoreInProgress by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    
    // File picker for restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreConfirmDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null)
                            Column {
                                Text(
                                    text = "Theme",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Units & Currency
            Text(
                text = "Units & Currency",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCurrencyDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null)
                            Column {
                                Text(
                                    text = "Currency",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = CurrencyUtils.getCurrencyByCode(currency).name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null)
                            Column {
                                Text(
                                    text = "Distance Unit",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = distanceUnit.getLongLabel(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = distanceUnit == UnitUtils.DistanceUnit.MILES,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsManager.setDistanceUnit(
                                        if (checked) UnitUtils.DistanceUnit.MILES
                                        else UnitUtils.DistanceUnit.KILOMETERS
                                    )
                                }
                            }
                        )
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null)
                            Column {
                                Text(
                                    text = "Volume Unit",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = volumeUnit.getLongLabel(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = volumeUnit == UnitUtils.VolumeUnit.GALLONS,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsManager.setVolumeUnit(
                                        if (checked) UnitUtils.VolumeUnit.GALLONS
                                        else UnitUtils.VolumeUnit.LITERS
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Features
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null)
                            Column {
                                Text(
                                    text = "Fuel Efficiency Tracking",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Calculate MPG/L per 100km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = fuelEfficiencyEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsManager.setFuelEfficiencyEnabled(checked)
                                }
                            }
                        )
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Column {
                                Text(
                                    text = "Service Reminders",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Auto-suggest next oil change",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = serviceRemindersEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsManager.setServiceRemindersEnabled(checked)
                                }
                            }
                        )
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Column {
                                Text(
                                    text = "Insurance/Registration Reminders",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Notify before expiry dates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = insuranceRemindersEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsManager.setInsuranceRemindersEnabled(checked)
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Backup
            Text(
                text = "Backup & Data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Manual Backup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Column {
                                Text(
                                    text = "Manual Backup",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Create backup file now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isBackupInProgress = true
                                    viewModel.createBackup { backupFile ->
                                        isBackupInProgress = false
                                        if (backupFile != null) {
                                            shareBackupFile(context, backupFile)
                                            Toast.makeText(
                                                context,
                                                                                                "Backup created: ${backupFile.name}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to create backup",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isBackupInProgress
                        ) {
                            if (isBackupInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isBackupInProgress) "Creating..." else "Backup")
                        }
                    }
                    
                    Divider()
                    
                    // Restore Backup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Column {
                                Text(
                                    text = "Restore Backup",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Restore from backup file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = {
                                restoreFileLauncher.launch("application/zip")
                            },
                            enabled = !isRestoreInProgress
                        ) {
                            if (isRestoreInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isRestoreInProgress) "Restoring..." else "Restore")
                        }
                    }
                    
                    Divider()
                    
                    // Auto Backup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (autoBackupEnabled) showBackupFrequencyDialog = true 
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Column {
                                Text(
                                    text = "Auto Backup",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (autoBackupEnabled) 
                                        "Enabled - ${autoBackupFrequency.name.lowercase()}"
                                    else 
                                        "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    viewModel.settingsManager.setAutoBackupEnabled(checked)
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Column {
                                Text(
                                    text = "Version",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "1.0.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Currency Selection Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CurrencyUtils.currencies.forEach { curr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.settingsManager.setCurrency(curr.code)
                                        showCurrencyDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = curr.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${curr.symbol} - ${curr.code}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (curr.code == currency) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (curr != CurrencyUtils.currencies.last()) {
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    SettingsManager.Theme.values().forEach { themeOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.settingsManager.setTheme(themeOption)
                                        showThemeDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = themeOption.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (themeOption == theme) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Backup Frequency Dialog
    if (showBackupFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showBackupFrequencyDialog = false },
            title = { Text("Backup Frequency") },
            text = {
                Column {
                    SettingsManager.BackupFrequency.values().forEach { freq ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.settingsManager.setAutoBackupFrequency(freq)
                                        showBackupFrequencyDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = freq.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (freq == autoBackupFrequency) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupFrequencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { 
                Text(
                    "Restore Backup?",
                    style = MaterialTheme.typography.headlineSmall
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "This will replace ALL current data with the backup.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Divider()
                    
                    Text(
                        "✅ Safety Features:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "• Your current data will be saved temporarily\n" +
                        "• If restore fails, data will be recovered automatically\n" +
                        "• Backup file will be validated before restore",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Divider()
                    
                    Text(
                        "⚠️ Warning:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "• All current cars and activities will be replaced\n" +
                        "• Make sure you selected the correct backup file\n" +
                        "• This action cannot be undone once completed",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                // In the restore confirmation dialog confirmButton:
Button(
    onClick = {
        showRestoreConfirmDialog = false
        pendingRestoreUri?.let { uri ->
            scope.launch {
                isRestoreInProgress = true
                try {
                    // Create a temporary file from URI
                    val tempFile = File(context.cacheDir, "restore_backup_${System.currentTimeMillis()}.zip")
                    
                    // Copy content from URI to temp file
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        isRestoreInProgress = false
                        Toast.makeText(
                            context,
                            "Cannot read backup file",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    
                    // Copy with progress
                    var bytesCopied = 0L
                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytes = input.read(buffer)
                            while (bytes >= 0) {
                                output.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                bytes = input.read(buffer)
                            }
                        }
                    }
                    
                    println("Copied $bytesCopied bytes to temp file")
                    
                    // Verify temp file was created
                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        isRestoreInProgress = false
                        Toast.makeText(
                            context,
                            "Failed to prepare backup file",
                            Toast.LENGTH_LONG
                        ).show()
                        tempFile.delete()
                        return@launch
                    }
                    
                    println("Temp file size: ${tempFile.length()} bytes")
                    
                    // Restore from temp file
                    viewModel.restoreBackup(tempFile) { success ->
                        isRestoreInProgress = false
                        
                        // Clean up temp file
                        try {
                            tempFile.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        if (success) {
                            Toast.makeText(
                                context,
                                "✅ Backup restored successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "❌ Restore failed. Your data is safe and unchanged.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    isRestoreInProgress = false
                    e.printStackTrace()
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        pendingRestoreUri = null
    },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error
    )
) {
    Icon(Icons.Default.Restore, contentDescription = null)
    Spacer(modifier = Modifier.width(8.dp))
    Text("Restore Anyway")
}
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper function to share backup file
private fun shareBackupFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Car Maintenance Backup")
            putExtra(
                Intent.EXTRA_TEXT,
                "Car Maintenance backup created on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share backup: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}