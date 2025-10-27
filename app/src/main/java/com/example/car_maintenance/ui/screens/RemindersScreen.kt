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
import com.example.car_maintenance.data.model.Reminder
import com.example.car_maintenance.ui.components.ReminderCard
import com.example.car_maintenance.utils.DateUtils
import com.example.car_maintenance.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import com.example.car_maintenance.ui.components.AppDatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val selectedCar by viewModel.selectedCar.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCar != null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder")
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
                    Text("No car selected")
                }
            }
        } else if (reminders.isEmpty()) {
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
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No reminders set",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add First Reminder")
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
                    items = reminders,
                    key = { it.id }
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onComplete = {
                            viewModel.markReminderCompleted(reminder.id)
                        },
                        onDelete = {
                            viewModel.deleteReminder(reminder)
                        }
                    )
                }
            }
        }
    }
    
    // Add Reminder Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                message = ""
            },
            title = { Text("Add Reminder") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Reminder Message") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Change Oil soon!!!") }
                    )
                    
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(DateUtils.formatDate(selectedDate))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCar?.let { car ->
                            scope.launch {
                                val reminder = Reminder(
                                    carId = car.id,
                                    message = message,
                                    triggerDate = selectedDate
                                )
                                viewModel.addReminder(reminder)
                                showAddDialog = false
                                message = ""
                            }
                        }
                    },
                    enabled = message.isNotEmpty()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        message = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
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
}