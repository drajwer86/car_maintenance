package com.example.car_maintenance

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.car_maintenance.data.SettingsManager
import com.example.car_maintenance.ui.navigation.NavGraph
import com.example.car_maintenance.ui.theme.CarMaintenanceTheme
import com.example.car_maintenance.utils.ReminderWorker
import com.example.car_maintenance.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }
        
        if (notificationGranted) {
            scheduleReminderCheck()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
        
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settingsManager = remember { SettingsManager(applicationContext) }
            val theme by settingsManager.theme.collectAsState(initial = SettingsManager.Theme.SYSTEM)
            
            val darkTheme = when (theme) {
                SettingsManager.Theme.LIGHT -> false
                SettingsManager.Theme.DARK -> true
                SettingsManager.Theme.SYSTEM -> isSystemInDarkTheme()
            }
            
            CarMaintenanceTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
    
    private fun scheduleReminderCheck() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ReminderCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}