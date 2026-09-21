package com.velo.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.velo.app.model.UnitSystem
import com.velo.app.sensors.SensorType
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "velo_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val KEY_MOCK_SIMULATOR = booleanPreferencesKey("mock_simulator_enabled")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_PAIRED_SENSORS = stringSetPreferencesKey("paired_sensor_types")
    }

    val unitSystemFlow: Flow<UnitSystem> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_UNIT_SYSTEM] ?: UnitSystem.METRIC.name
        try {
            UnitSystem.valueOf(name)
        } catch (e: Exception) {
            UnitSystem.METRIC
        }
    }

    val isMockSimulatorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_MOCK_SIMULATOR] ?: false
    }

    val isKeepScreenOnEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_KEEP_SCREEN_ON] ?: true
    }

    val pairedSensorsFlow: Flow<Set<SensorType>> = context.dataStore.data.map { preferences ->
        val rawSet = preferences[KEY_PAIRED_SENSORS] ?: emptySet()
        rawSet.mapNotNull { name ->
            try {
                SensorType.valueOf(name)
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[KEY_UNIT_SYSTEM] = unitSystem.name
        }
    }

    suspend fun setMockSimulatorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MOCK_SIMULATOR] = enabled
        }
    }

    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun pairSensor(type: SensorType) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PAIRED_SENSORS] ?: emptySet()
            preferences[KEY_PAIRED_SENSORS] = current + type.name
        }
    }

    suspend fun unpairSensor(type: SensorType) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PAIRED_SENSORS] ?: emptySet()
            preferences[KEY_PAIRED_SENSORS] = current - type.name
        }
    }
}
