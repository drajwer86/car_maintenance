package com.example.car_maintenance.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.car_maintenance.data.database.AppDatabase
import com.example.car_maintenance.data.repository.CarRepository
import com.example.car_maintenance.ui.SnoozeReminderActivity  // Add this import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(ReminderWorker.EXTRA_REMINDER_ID, -1)
        
        if (reminderId == -1) return
        
        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId)
        
        when (intent.action) {
            ReminderWorker.ACTION_DISMISS -> {
                handleDismiss(context, reminderId)
            }
            ReminderWorker.ACTION_SNOOZE -> {
                handleSnooze(context, reminderId)
            }
        }
    }
    
    private fun handleDismiss(context: Context, reminderId: Int) {
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = CarRepository(
                    database.carDao(),
                    database.activityDao(),
                    database.activityImageDao(),
                    database.reminderDao(),
                    database.refuelingDetailsDao()
                )
                
                // Mark reminder as completed
                repository.markReminderCompleted(reminderId.toLong())
                
                Toast.makeText(context, "Reminder dismissed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to dismiss reminder", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleSnooze(context: Context, reminderId: Int) {
        // Open snooze dialog activity
        val snoozeIntent = Intent(context, SnoozeReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ReminderWorker.EXTRA_REMINDER_ID, reminderId)
        }
        context.startActivity(snoozeIntent)
    }
}