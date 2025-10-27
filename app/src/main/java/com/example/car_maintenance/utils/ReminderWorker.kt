package com.example.car_maintenance.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.car_maintenance.MainActivity
import com.example.car_maintenance.R
import com.example.car_maintenance.data.database.AppDatabase
import com.example.car_maintenance.data.repository.CarRepository

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val CHANNEL_ID = "car_maintenance_reminders"
        const val CHANNEL_NAME = "Maintenance Reminders"
        const val ACTION_DISMISS = "com.example.car_maintenance.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.car_maintenance.ACTION_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
    
    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CarRepository(
            database.carDao(),
            database.activityDao(),
            database.activityImageDao(),
            database.reminderDao(),
            database.refuelingDetailsDao()
        )
        
        val currentTime = System.currentTimeMillis()
        val pendingReminders = repository.getPendingReminders(currentTime)
        
        pendingReminders.forEach { reminder ->
            showNotification(
                message = reminder.message,
                reminderId = reminder.id.toInt(),
                carId = reminder.carId
            )
        }
        
        return Result.success()
    }
    
    private fun showNotification(message: String, reminderId: Int, carId: Long) {
        createNotificationChannel()
        
        // Main intent - open app
        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Dismiss action
        val dismissIntent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        
        val dismissPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            reminderId * 10 + 1,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Snooze action
        val snoozeIntent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        
        val snoozePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            reminderId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Car Maintenance Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notifications,
                "Snooze",
                snoozePendingIntent
            )
            .addAction(
                R.drawable.ic_close,
                "Dismiss",
                dismissPendingIntent
            )
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for car maintenance reminders"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}