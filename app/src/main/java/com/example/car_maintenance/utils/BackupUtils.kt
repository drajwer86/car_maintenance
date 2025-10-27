package com.example.car_maintenance.utils

import android.content.Context
import android.os.Environment
import com.example.car_maintenance.data.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val cars: List<Car>,
    val activities: List<Activity>,
    val activityImages: List<ActivityImage>,
    val reminders: List<Reminder>,
    val refuelingDetails: List<RefuelingDetails>
)

object BackupUtils {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    private const val BACKUP_JSON_NAME = "backup.json"
    private const val IMAGES_DIR = "images/"
    private const val THUMBNAILS_DIR = "images/thumbnails/"
    private const val MAX_BACKUP_SIZE_MB = 500 // Maximum backup size in MB
    
    /**
     * Creates a backup ZIP file containing all app data and images
     * @param context Application context
     * @param backupData Data to backup
     * @return File object of created backup, or null if failed
     */
    suspend fun createBackup(
        context: Context,
        backupData: BackupData
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Validate backup data
            if (backupData.cars.isEmpty()) {
                return@withContext null
            }
            
            // Create backup directory
            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CarMaintenance"
            )
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Create backup file with timestamp
            val dateFormat = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.zip")
            
            // Create ZIP file
            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zipOut ->
                // Add JSON data
                val jsonData = gson.toJson(backupData)
                val jsonEntry = ZipEntry(BACKUP_JSON_NAME)
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(jsonData.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
                
                // Add images
                var addedImagesCount = 0
                var failedImagesCount = 0
                
                backupData.activityImages.forEach { image ->
                    val imageAdded = addFileToZip(zipOut, File(image.filePath), IMAGES_DIR)
                    val thumbnailAdded = addFileToZip(zipOut, File(image.thumbnailPath), THUMBNAILS_DIR)
                    
                    if (imageAdded && thumbnailAdded) {
                        addedImagesCount++
                    } else {
                        failedImagesCount++
                    }
                }
                
                // Log backup stats
                println("Backup created: ${backupData.cars.size} cars, ${backupData.activities.size} activities, $addedImagesCount images")
                if (failedImagesCount > 0) {
                    println("Warning: $failedImagesCount images failed to backup")
                }
            }
            
            // Verify backup file was created and has content
            if (!backupFile.exists() || backupFile.length() == 0L) {
                backupFile.delete()
                return@withContext null
            }
            
            // Check if backup size is reasonable
            val backupSizeMB = backupFile.length() / (1024 * 1024)
            if (backupSizeMB > MAX_BACKUP_SIZE_MB) {
                println("Warning: Backup size is ${backupSizeMB}MB (max recommended: ${MAX_BACKUP_SIZE_MB}MB)")
            }
            
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Adds a file to the ZIP archive
     * @param zipOut ZIP output stream
     * @param file File to add
     * @param pathPrefix Path prefix in ZIP
     * @return true if file was added successfully, false otherwise
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, pathPrefix: String): Boolean {
        if (!file.exists()) {
            println("File not found: ${file.absolutePath}")
            return false
        }
        
        if (!file.canRead()) {
            println("Cannot read file: ${file.absolutePath}")
            return false
        }
        
        try {
            val entry = ZipEntry(pathPrefix + file.name)
            entry.size = file.length()
            zipOut.putNextEntry(entry)
            
            BufferedInputStream(FileInputStream(file)).use { input ->
                input.copyTo(zipOut, bufferSize = 8192)
            }
            
            zipOut.closeEntry()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Restores data from a backup ZIP file
     * @param context Application context
     * @param backupFile Backup ZIP file to restore from
     * @return BackupData object if successful, null if failed
     */
    suspend fun restoreBackup(
    context: Context,
    backupFile: File
): BackupData? = withContext(Dispatchers.IO) {
    try {
        println("========== RESTORE BACKUP START ==========")
        println("Backup file path: ${backupFile.absolutePath}")
        println("Backup file exists: ${backupFile.exists()}")
        println("Backup file size: ${backupFile.length()} bytes")
        println("Backup file can read: ${backupFile.canRead()}")
        
        // Validate file exists and is readable
        if (!backupFile.exists()) {
            println("ERROR: Backup file does not exist")
            return@withContext null
        }
        
        if (backupFile.length() == 0L) {
            println("ERROR: Backup file is empty (0 bytes)")
            return@withContext null
        }
        
        if (!backupFile.canRead()) {
            println("ERROR: Cannot read backup file (permission issue)")
            return@withContext null
        }
        
        // Validate it's a valid ZIP file
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(backupFile))).use { testZip ->
                val firstEntry = testZip.nextEntry
                if (firstEntry == null) {
                    println("ERROR: Not a valid ZIP file or empty ZIP")
                    return@withContext null
                }
                println("ZIP file is valid, first entry: ${firstEntry.name}")
            }
        } catch (e: Exception) {
            println("ERROR: Invalid ZIP file: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
        
        var backupData: BackupData? = null
        val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
        
        try {
            // Clean up temp directory if exists
            if (tempDir.exists()) {
                println("Cleaning existing temp directory")
                tempDir.deleteRecursively()
            }
            tempDir.mkdirs()
            println("Created temp directory: ${tempDir.absolutePath}")
            
            // Extract ZIP file
            var foundBackupJson = false
            var extractedFilesCount = 0
            
            ZipInputStream(BufferedInputStream(FileInputStream(backupFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    println("Processing ZIP entry: ${entry.name}")
                    val filePath = File(tempDir, entry.name)
                    
                    // Security check: prevent path traversal
                    if (!filePath.canonicalPath.startsWith(tempDir.canonicalPath)) {
                        println("SECURITY WARNING: Path traversal attempt detected: ${entry.name}")
                        return@withContext null
                    }
                    
                    if (entry.isDirectory) {
                        filePath.mkdirs()
                        println("Created directory: ${filePath.name}")
                    } else {
                        filePath.parentFile?.mkdirs()
                        
                        if (entry.name == BACKUP_JSON_NAME) {
                            foundBackupJson = true
                            println("Found backup.json, reading...")
                            
                            // Read JSON data
                            val jsonContent = zipIn.readBytes().toString(Charsets.UTF_8)
                            println("JSON content length: ${jsonContent.length} chars")
                            
                            // Validate JSON is not empty
                            if (jsonContent.isBlank()) {
                                println("ERROR: backup.json is empty")
                                return@withContext null
                            }
                            
                            try {
                                backupData = gson.fromJson(jsonContent, BackupData::class.java)
                                println("Parsed backup data successfully")
                                println("  - Cars: ${backupData?.cars?.size ?: 0}")
                                println("  - Activities: ${backupData?.activities?.size ?: 0}")
                                println("  - Images: ${backupData?.activityImages?.size ?: 0}")
                                
                                // Validate backup data structure
                                if (!validateBackupData(backupData)) {
                                    println("ERROR: Backup data validation failed")
                                    return@withContext null
                                }
                                
                            } catch (e: com.google.gson.JsonSyntaxException) {
                                println("ERROR: Invalid JSON syntax: ${e.message}")
                                e.printStackTrace()
                                return@withContext null
                            } catch (e: Exception) {
                                println("ERROR: Failed to parse JSON: ${e.message}")
                                e.printStackTrace()
                                return@withContext null
                            }
                        } else {
                            // Extract image/thumbnail files
                            println("Extracting file: ${entry.name}")
                            BufferedOutputStream(FileOutputStream(filePath)).use { output ->
                                zipIn.copyTo(output, bufferSize = 8192)
                            }
                            extractedFilesCount++
                            println("Extracted: ${filePath.name} (${filePath.length()} bytes)")
                        }
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            println("Extraction complete: $extractedFilesCount files")
            
            // Validate that backup.json was found
            if (!foundBackupJson) {
                println("ERROR: No backup.json found in ZIP file")
                return@withContext null
            }
            
            if (backupData == null) {
                println("ERROR: Backup data is null after parsing")
                return@withContext null
            }
            
            // Restore images to proper locations
            var restoredImagesCount = 0
            var failedImagesCount = 0
            
            backupData?.let { data ->
                val imagesDir = File(tempDir, "images")
                val thumbnailsDir = File(tempDir, "images/thumbnails")
                
                println("Restoring ${data.activityImages.size} images...")
                println("Images dir exists: ${imagesDir.exists()}")
                println("Thumbnails dir exists: ${thumbnailsDir.exists()}")
                
                data.activityImages.forEach { image ->
                    try {
                        val originalFile = File(imagesDir, File(image.filePath).name)
                        val thumbnailFile = File(thumbnailsDir, File(image.thumbnailPath).name)
                        
                        var imageRestored = false
                        var thumbnailRestored = false
                        
                        if (originalFile.exists() && originalFile.canRead()) {
                            val destFile = File(image.filePath)
                            destFile.parentFile?.mkdirs()
                            originalFile.copyTo(destFile, overwrite = true)
                            imageRestored = true
                            println("Restored image: ${destFile.name}")
                        } else {
                            println("WARNING: Image file not found: ${originalFile.name}")
                        }
                        
                        if (thumbnailFile.exists() && thumbnailFile.canRead()) {
                            val destFile = File(image.thumbnailPath)
                            destFile.parentFile?.mkdirs()
                            thumbnailFile.copyTo(destFile, overwrite = true)
                            thumbnailRestored = true
                            println("Restored thumbnail: ${destFile.name}")
                        } else {
                            println("WARNING: Thumbnail file not found: ${thumbnailFile.name}")
                        }
                        
                        if (imageRestored && thumbnailRestored) {
                            restoredImagesCount++
                        } else {
                            failedImagesCount++
                        }
                        
                    } catch (e: Exception) {
                        println("ERROR restoring image: ${e.message}")
                        e.printStackTrace()
                        failedImagesCount++
                    }
                }
            }
            
            println("Images restored: $restoredImagesCount, failed: $failedImagesCount")
            println("========== RESTORE BACKUP SUCCESS ==========")
            
            backupData
            
        } catch (e: Exception) {
            println("========== RESTORE BACKUP FAILED ==========")
            println("ERROR: ${e.message}")
            e.printStackTrace()
            null
        } finally {
            // Clean up temp directory
            try {
                if (tempDir.exists()) {
                    println("Cleaning up temp directory")
                    tempDir.deleteRecursively()
                }
            } catch (e: Exception) {
                println("Warning: Failed to clean up temp directory: ${e.message}")
            }
        }
        
    } catch (e: Exception) {
        println("========== RESTORE BACKUP EXCEPTION ==========")
        println("FATAL ERROR: ${e.message}")
        e.printStackTrace()
        null
    }
}
    
    /**
     * Validates the backup file before attempting to restore
     * @param backupFile File to validate
     * @return true if valid, false otherwise
     */
    private fun validateBackupFile(file: File): Boolean {
        // Check file exists
        if (!file.exists()) {
            println("Backup file does not exist")
            return false
        }
        
        // Check file is readable
        if (!file.canRead()) {
            println("Cannot read backup file")
            return false
        }
        
        // Check file size is not zero
        if (file.length() == 0L) {
            println("Backup file is empty")
            return false
        }
        
        // Check file size is reasonable (not too large)
        val fileSizeMB = file.length() / (1024 * 1024)
        if (fileSizeMB > MAX_BACKUP_SIZE_MB) {
            println("Backup file is too large: ${fileSizeMB}MB (max: ${MAX_BACKUP_SIZE_MB}MB)")
            return false
        }
        
        // Check it's a valid ZIP file by trying to read first entry
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zipIn ->
                val firstEntry = zipIn.nextEntry
                if (firstEntry == null) {
                    println("Backup file is not a valid ZIP or is empty")
                    return false
                }
            }
        } catch (e: Exception) {
            println("Invalid ZIP file: ${e.message}")
            return false
        }
        
        return true
    }
    
    /**
     * Validates the backup data structure
     * @param backupData Data to validate
     * @return true if valid, false otherwise
     */
    private fun validateBackupData(backupData: BackupData?): Boolean {
        if (backupData == null) {
            println("Backup data is null")
            return false
        }
        
        // Check version is supported
        if (backupData.version < 1 || backupData.version > 1) {
            println("Unsupported backup version: ${backupData.version}")
            return false
        }
        
        // Check timestamp is reasonable
        val currentTime = System.currentTimeMillis()
        val oneYearInMs = 365L * 24 * 60 * 60 * 1000
        if (backupData.timestamp > currentTime || backupData.timestamp < (currentTime - 10 * oneYearInMs)) {
            println("Backup timestamp is suspicious: ${Date(backupData.timestamp)}")
            // Don't fail, just warn
        }
        
        // Check cars list is not empty
        if (backupData.cars.isEmpty()) {
            println("Backup contains no cars")
            return false
        }
        
        // Validate car data
        backupData.cars.forEach { car ->
            if (car.brand.isBlank() || car.model.isBlank()) {
                println("Invalid car data: brand or model is blank")
                return false
            }
            if (car.year < 1900 || car.year > 2100) {
                println("Invalid car year: ${car.year}")
                return false
            }
        }
        
        // Validate activities reference valid cars
        val carIds = backupData.cars.map { it.id }.toSet()
        backupData.activities.forEach { activity ->
            if (activity.carId !in carIds) {
                println("Activity references non-existent car: ${activity.carId}")
                return false
            }
        }
        
        // Validate images reference valid activities
        val activityIds = backupData.activities.map { it.id }.toSet()
        backupData.activityImages.forEach { image ->
            if (image.activityId !in activityIds) {
                println("Image references non-existent activity: ${image.activityId}")
                return false
            }
        }
        
        // Validate reminders reference valid cars
        backupData.reminders.forEach { reminder ->
            if (reminder.carId !in carIds) {
                println("Reminder references non-existent car: ${reminder.carId}")
                return false
            }
        }
        
        // Validate refueling details reference valid activities
        backupData.refuelingDetails.forEach { details ->
            if (details.activityId !in activityIds) {
                println("Refueling details reference non-existent activity: ${details.activityId}")
                return false
            }
        }
        
        println("Backup data validation passed: ${backupData.cars.size} cars, ${backupData.activities.size} activities")
        return true
    }
    
    /**
     * Calculates the estimated size of a backup
     * @param backupData Data to calculate size for
          * @return Size in bytes
     */
    fun getBackupSize(backupData: BackupData): Long {
        var size = 0L
        
        try {
            // Estimate JSON size
            val jsonSize = gson.toJson(backupData).toByteArray(Charsets.UTF_8).size.toLong()
            size += jsonSize
            
            // Add actual image file sizes
            backupData.activityImages.forEach { image ->
                val imageFile = File(image.filePath)
                val thumbFile = File(image.thumbnailPath)
                
                if (imageFile.exists()) {
                    size += imageFile.length()
                }
                if (thumbFile.exists()) {
                    size += thumbFile.length()
                }
            }
            
            // Add ~10% overhead for ZIP compression metadata
            size = (size * 1.1).toLong()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return size
    }
    
    /**
     * Formats file size for human-readable display
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "15.5 MB")
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
    
    /**
     * Gets information about a backup file
     * @param backupFile Backup file to analyze
     * @return Map with backup information, or null if invalid
     */
    suspend fun getBackupInfo(backupFile: File): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            if (!validateBackupFile(backupFile)) {
                return@withContext null
            }
            
            var backupData: BackupData? = null
            
            ZipInputStream(BufferedInputStream(FileInputStream(backupFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    if (entry.name == BACKUP_JSON_NAME) {
                        val jsonContent = zipIn.readBytes().toString(Charsets.UTF_8)
                        try {
                            backupData = gson.fromJson(jsonContent, BackupData::class.java)
                        } catch (e: Exception) {
                            return@withContext null
                        }
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            backupData?.let { data ->
                val info = mutableMapOf<String, String>()
                info["File Name"] = backupFile.name
                info["File Size"] = formatFileSize(backupFile.length())
                info["Backup Date"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(data.timestamp))
                info["Version"] = data.version.toString()
                info["Cars"] = data.cars.size.toString()
                info["Activities"] = data.activities.size.toString()
                info["Images"] = data.activityImages.size.toString()
                info["Reminders"] = data.reminders.size.toString()
                
                // Calculate total cost
                val totalCost = data.activities.sumOf { it.cost }
                info["Total Cost"] = String.format("%.2f", totalCost)
                
                info
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lists all available backup files in the backup directory
     * @return List of backup files, sorted by date (newest first)
     */
    fun listBackupFiles(): List<File> {
        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CarMaintenance"
        )
        
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return emptyList()
        }
        
        return backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("backup_") && file.name.endsWith(".zip")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Deletes old backup files, keeping only the most recent N backups
     * @param keepCount Number of backups to keep
     * @return Number of backups deleted
     */
    fun cleanOldBackups(keepCount: Int = 5): Int {
        val backups = listBackupFiles()
        
        if (backups.size <= keepCount) {
            return 0
        }
        
        val toDelete = backups.drop(keepCount)
        var deletedCount = 0
        
        toDelete.forEach { file ->
            try {
                if (file.delete()) {
                    deletedCount++
                    println("Deleted old backup: ${file.name}")
                }
            } catch (e: Exception) {
                println("Failed to delete backup ${file.name}: ${e.message}")
            }
        }
        
        return deletedCount
    }
    
    /**
     * Checks if there's enough storage space for a backup
     * @param estimatedSize Estimated backup size in bytes
     * @return true if there's enough space, false otherwise
     */
    fun hasEnoughSpace(estimatedSize: Long): Boolean {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val availableSpace = downloadDir.usableSpace
        
        // Require at least 2x the estimated size for safety
        val requiredSpace = estimatedSize * 2
        
        return availableSpace >= requiredSpace
    }
    
    /**
     * Gets the backup directory path
     * @return Path to backup directory
     */
    fun getBackupDirectory(): String {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CarMaintenance"
        ).absolutePath
    }
    
    /**
     * Validates backup integrity by checking if it can be read and parsed
     * @param backupFile File to validate
     * @return Pair of (isValid, errorMessage)
     */
    suspend fun validateBackupIntegrity(backupFile: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Validate file
            if (!validateBackupFile(backupFile)) {
                return@withContext Pair(false, "Invalid backup file")
            }
            
            // Step 2: Try to parse JSON
            var backupData: BackupData? = null
            var foundJson = false
            
            ZipInputStream(BufferedInputStream(FileInputStream(backupFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    if (entry.name == BACKUP_JSON_NAME) {
                        foundJson = true
                        val jsonContent = zipIn.readBytes().toString(Charsets.UTF_8)
                        
                        try {
                            backupData = gson.fromJson(jsonContent, BackupData::class.java)
                        } catch (e: JsonSyntaxException) {
                            return@withContext Pair(false, "Corrupted backup data: ${e.message}")
                        }
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            if (!foundJson) {
                return@withContext Pair(false, "Backup file missing required data")
            }
            
            // Step 3: Validate data structure
            if (!validateBackupData(backupData)) {
                return@withContext Pair(false, "Invalid backup data structure")
            }
            
            // Step 4: Check if images are present (warn if missing, don't fail)
            val missingImages = backupData?.activityImages?.count { image ->
                val imageFile = File(image.filePath)
                val thumbFile = File(image.thumbnailPath)
                !imageFile.exists() && !thumbFile.exists()
            } ?: 0
            
            if (missingImages > 0) {
                return@withContext Pair(
                    true, 
                    "Valid backup, but $missingImages images are missing (they may have been deleted)"
                )
            }
            
            Pair(true, "Backup is valid and complete")
            
        } catch (e: Exception) {
            Pair(false, "Error validating backup: ${e.message}")
        }
    }
    
    /**
     * Creates a quick backup summary for display
     * @param backupData Backup data to summarize
     * @return Formatted summary string
     */
    fun getBackupSummary(backupData: BackupData): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val backupDate = dateFormat.format(Date(backupData.timestamp))
        
        val totalCost = backupData.activities.sumOf { it.cost }
        val estimatedSize = formatFileSize(getBackupSize(backupData))
        
        return buildString {
            appendLine("Backup Summary")
            appendLine("─────────────────")
            appendLine("Date: $backupDate")
            appendLine("Cars: ${backupData.cars.size}")
            appendLine("Activities: ${backupData.activities.size}")
            appendLine("Images: ${backupData.activityImages.size}")
            appendLine("Reminders: ${backupData.reminders.size}")
            appendLine("Total Cost: ${"%.2f".format(totalCost)}")
            appendLine("Estimated Size: $estimatedSize")
        }
    }
}