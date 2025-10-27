package com.example.car_maintenance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.car_maintenance.utils.UnitUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val DISTANCE_UNIT_KEY = stringPreferencesKey("distance_unit")
        val VOLUME_UNIT_KEY = stringPreferencesKey("volume_unit")
        val FUEL_EFFICIENCY_ENABLED_KEY = booleanPreferencesKey("fuel_efficiency_enabled")
        val SERVICE_REMINDERS_ENABLED_KEY = booleanPreferencesKey("service_reminders_enabled")
        val INSURANCE_REMINDERS_ENABLED_KEY = booleanPreferencesKey("insurance_reminders_enabled")
        val BIOMETRIC_LOCK_ENABLED_KEY = booleanPreferencesKey("biometric_lock_enabled")
        val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY_KEY = stringPreferencesKey("auto_backup_frequency")
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }
    
    enum class Theme {
        LIGHT, DARK, SYSTEM
    }
    
    enum class BackupFrequency {
        DAILY, WEEKLY, MONTHLY
    }
    
    val theme: Flow<Theme> = context.dataStore.data.map { preferences ->
        Theme.valueOf(preferences[THEME_KEY] ?: Theme.SYSTEM.name)
    }
    
    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_KEY] ?: "USD"
    }
    
    val distanceUnit: Flow<UnitUtils.DistanceUnit> = context.dataStore.data.map { preferences ->
        UnitUtils.DistanceUnit.valueOf(
            preferences[DISTANCE_UNIT_KEY] ?: UnitUtils.DistanceUnit.KILOMETERS.name
        )
    }
    
    val volumeUnit: Flow<UnitUtils.VolumeUnit> = context.dataStore.data.map { preferences ->
        UnitUtils.VolumeUnit.valueOf(
            preferences[VOLUME_UNIT_KEY] ?: UnitUtils.VolumeUnit.LITERS.name
        )
    }
    
    val fuelEfficiencyEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FUEL_EFFICIENCY_ENABLED_KEY] ?: true
    }
    
    val serviceRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SERVICE_REMINDERS_ENABLED_KEY] ?: true
    }
    
    val insuranceRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[INSURANCE_REMINDERS_ENABLED_KEY] ?: true
    }
    
    val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_LOCK_ENABLED_KEY] ?: false
    }
    
    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_ENABLED_KEY] ?: false
    }
    
    val autoBackupFrequency: Flow<BackupFrequency> = context.dataStore.data.map { preferences ->
        BackupFrequency.valueOf(
            preferences[AUTO_BACKUP_FREQUENCY_KEY] ?: BackupFrequency.WEEKLY.name
        )
    }
    
    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "en"
    }
    
    suspend fun setTheme(theme: Theme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
    
    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }
    
    suspend fun setDistanceUnit(unit: UnitUtils.DistanceUnit) {
        context.dataStore.edit { preferences ->
            preferences[DISTANCE_UNIT_KEY] = unit.name
        }
    }
    
    suspend fun setVolumeUnit(unit: UnitUtils.VolumeUnit) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_UNIT_KEY] = unit.name
        }
    }
    
    suspend fun setFuelEfficiencyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FUEL_EFFICIENCY_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setServiceRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_REMINDERS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setInsuranceRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INSURANCE_REMINDERS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_LOCK_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setAutoBackupFrequency(frequency: BackupFrequency) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_FREQUENCY_KEY] = frequency.name
        }
    }
    
    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
}