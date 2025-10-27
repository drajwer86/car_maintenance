package com.example.car_maintenance.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.car_maintenance.data.database.AppDatabase
import com.example.car_maintenance.data.repository.CarRepository
import com.example.car_maintenance.ui.components.AppDatePickerDialog
import com.example.car_maintenance.ui.theme.CarMaintenanceTheme
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.utils.ReminderWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class SnoozeReminderActivity : ComponentActivity() {
    
    private lateinit var repository: CarRepository
    private var reminderId: Int = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        reminderId = intent.getIntExtra(ReminderWorker.EXTRA_REMINDER_ID, -1)
        
        if (reminderId == -1) {
            finish()
            return
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        repository = CarRepository(
            database.carDao(),
            database.activityDao(),
            database.activityImageDao(),
            database.reminderDao(),
            database.refuelingDetailsDao()
        )
        
        setContent {
            CarMaintenanceTheme {
                SnoozeReminderScreen(
                    reminderId = reminderId.toLong(),
                    repository = repository,
                    onSnooze = { newDate ->
                        snoozeReminder(newDate)
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
    
    private fun snoozeReminder(newDate: Long) {
        lifecycleScope.launch {
            try {
                val reminder = repository.getAllReminders().first()
                    .find { it.id == reminderId.toLong() }
                
                if (reminder != null) {
                    val updatedReminder = reminder.copy(
                        triggerDate = newDate,
                        isCompleted = false
                    )
                    repository.updateReminder(updatedReminder)
                    
                    Toast.makeText(
                        this@SnoozeReminderActivity,
                        "Reminder snoozed until ${DateUtils.formatDate(newDate)}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    finish()
                } else {
                    Toast.makeText(
                        this@SnoozeReminderActivity,
                        "Reminder not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@SnoozeReminderActivity,
                    "Failed to snooze reminder: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeReminderScreen(
    reminderId: Long,
    repository: CarRepository,
    onSnooze: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val reminder by repository.getAllReminders().collectAsState(initial = emptyList())
    val currentReminder = remember(reminder) {
        reminder.find { it.id == reminderId }
    }
    
    var selectedDate by remember { 
        mutableStateOf(currentReminder?.triggerDate ?: System.currentTimeMillis()) 
    }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Quick snooze options in milliseconds
    val quickOptions = listOf(
        "1 Hour" to (60 * 60 * 1000L),
        "3 Hours" to (3 * 60 * 60 * 1000L),
        "Tomorrow" to (24 * 60 * 60 * 1000L),
        "1 Week" to (7 * 24 * 60 * 60 * 1000L),
        "1 Month" to (30 * 24 * 60 * 60 * 1000L)
    )
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Snooze,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Snooze Reminder",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            currentReminder?.let {
                Text(
                    text = it.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Quick snooze options
            Text(
                text = "Quick Snooze",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            quickOptions.forEach { (label, duration) ->
                OutlinedButton(
                    onClick = {
                        selectedDate = System.currentTimeMillis() + duration
                        onSnooze(selectedDate)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Custom date
            Text(
                text = "Custom Date",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(DateUtils.formatDate(selectedDate))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                                        Text("Cancel")
                }
                
                Button(
                    onClick = { onSnooze(selectedDate) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snooze")
                }
            }
        }
    }
    
    // Date Picker
    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
            },
            onDismiss = { showDatePicker = false }
        )
    }
}