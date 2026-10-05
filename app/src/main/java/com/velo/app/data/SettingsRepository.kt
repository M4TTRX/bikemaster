package com.velo.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.velo.app.model.MetricType
import com.velo.app.model.SensorDataType
import com.velo.app.model.SpeedSource
import com.velo.app.model.ThemeMode
import com.velo.app.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "velo_settings")

data class PairedBleDevice(
    val address: String,
    val name: String,
    val detectedCapabilities: Set<SensorDataType> = emptySet(),
    val enabledDataTypes: Set<SensorDataType> = emptySet()
)

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val KEY_MOCK_SIMULATOR = booleanPreferencesKey("mock_simulator_enabled")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_PAIRED_BLE_DEVICES = stringSetPreferencesKey("paired_ble_devices")
        val KEY_SPEED_SOURCE = stringPreferencesKey("speed_source")
        val KEY_WHEEL_CIRCUMFERENCE_MM = intPreferencesKey("wheel_circumference_mm")
        val KEY_COCKPIT_TILES = stringPreferencesKey("cockpit_tiles")
        val KEY_COCKPIT_TILE_SOURCES = stringPreferencesKey("cockpit_tile_sources")

        val DEFAULT_TILES = listOf(
            MetricType.DISTANCE,
            MetricType.ELAPSED_TIME,
            MetricType.CADENCE,
            MetricType.POWER,
            MetricType.HEART_RATE,
            MetricType.MAX_SPEED
        )

        /**
         * Determines the device default unit system using Android ICU LocaleData if available,
         * falling back to country code detection (US/LR/MM/GB -> IMPERIAL), and defaulting to METRIC.
         */
        fun getDefaultDeviceUnitSystem(): UnitSystem {
            return try {
                val uLocale = android.icu.util.ULocale.getDefault()
                val measurementSystem = android.icu.util.LocaleData.getMeasurementSystem(uLocale)
                if (measurementSystem == android.icu.util.LocaleData.MeasurementSystem.US ||
                    measurementSystem == android.icu.util.LocaleData.MeasurementSystem.UK
                ) {
                    UnitSystem.IMPERIAL
                } else {
                    UnitSystem.METRIC
                }
            } catch (e: Throwable) {
                try {
                    val country = java.util.Locale.getDefault().country.uppercase(java.util.Locale.US)
                    if (country in listOf("US", "LR", "MM", "GB")) {
                        UnitSystem.IMPERIAL
                    } else {
                        UnitSystem.METRIC
                    }
                } catch (e2: Throwable) {
                    UnitSystem.METRIC
                }
            }
        }
    }

    val defaultUnitSystem: UnitSystem = getDefaultDeviceUnitSystem()

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_THEME_MODE] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    val speedSourceFlow: Flow<SpeedSource> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_SPEED_SOURCE] ?: SpeedSource.AUTO.name
        try {
            SpeedSource.valueOf(name)
        } catch (e: Exception) {
            SpeedSource.AUTO
        }
    }

    val wheelCircumferenceFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_WHEEL_CIRCUMFERENCE_MM] ?: 2105
    }

    val unitSystemFlow: Flow<UnitSystem> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_UNIT_SYSTEM] ?: defaultUnitSystem.name
        try {
            UnitSystem.valueOf(name)
        } catch (e: Exception) {
            defaultUnitSystem
        }
    }

    val isMockSimulatorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_MOCK_SIMULATOR] ?: false
    }

    val isKeepScreenOnEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_KEEP_SCREEN_ON] ?: true
    }

    val pairedBleDevicesFlow: Flow<List<PairedBleDevice>> = context.dataStore.data.map { preferences ->
        val rawSet = preferences[KEY_PAIRED_BLE_DEVICES] ?: emptySet()
        rawSet.mapNotNull { encoded ->
            try {
                val parts = encoded.split("|")
                if (parts.size >= 4) {
                    // New format: address|name|detectedCaps|enabledCaps
                    val address = parts[0]
                    val name = parts[1]
                    val detected = parts[2].split(",").mapNotNull {
                        try { SensorDataType.valueOf(it.trim()) } catch (e: Exception) { null }
                    }.toSet()
                    val enabled = parts[3].split(",").mapNotNull {
                        try { SensorDataType.valueOf(it.trim()) } catch (e: Exception) { null }
                    }.toSet()
                    PairedBleDevice(
                        address = address,
                        name = name,
                        detectedCapabilities = detected,
                        enabledDataTypes = enabled
                    )
                } else if (parts.size == 3) {
                    // Legacy format: type|address|name
                    val oldType = parts[0]
                    val address = parts[1]
                    val name = parts[2]
                    val caps = when (oldType) {
                        "HEART_RATE" -> setOf(SensorDataType.HEART_RATE)
                        "CADENCE_SPEED" -> setOf(SensorDataType.SPEED, SensorDataType.CADENCE)
                        "POWER" -> setOf(SensorDataType.POWER, SensorDataType.CADENCE)
                        else -> setOf(SensorDataType.SPEED, SensorDataType.CADENCE)
                    }
                    PairedBleDevice(
                        address = address,
                        name = name,
                        detectedCapabilities = caps,
                        enabledDataTypes = caps
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val cockpitTilesFlow: Flow<List<MetricType>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_COCKPIT_TILES]
        if (raw.isNullOrBlank()) {
            DEFAULT_TILES
        } else {
            val list = raw.split(",").mapNotNull {
                try { MetricType.valueOf(it.trim()) } catch (e: Exception) { null }
            }
            if (list.size >= 6) list.take(6) else DEFAULT_TILES
        }
    }

    val cockpitTileSourcesFlow: Flow<Map<Int, String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_COCKPIT_TILE_SOURCES] ?: ""
        if (raw.isBlank()) {
            emptyMap()
        } else {
            raw.split(";").mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val idx = parts[0].toIntOrNull()
                    if (idx != null) idx to parts[1] else null
                } else null
            }.toMap()
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = themeMode.name
        }
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[KEY_UNIT_SYSTEM] = unitSystem.name
        }
    }

    suspend fun setSpeedSource(speedSource: SpeedSource) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEED_SOURCE] = speedSource.name
        }
    }

    suspend fun setWheelCircumference(circumferenceMm: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WHEEL_CIRCUMFERENCE_MM] = circumferenceMm
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

    suspend fun savePairedDevice(device: PairedBleDevice) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PAIRED_BLE_DEVICES] ?: emptySet()
            // Filter out existing entries with matching address
            val filtered = current.filterNot { it.contains(device.address) }
            val detectedStr = device.detectedCapabilities.joinToString(",") { it.name }
            val enabledStr = device.enabledDataTypes.joinToString(",") { it.name }
            val encoded = "${device.address}|${device.name}|$detectedStr|$enabledStr"
            preferences[KEY_PAIRED_BLE_DEVICES] = (filtered + encoded).toSet()
        }
    }

    suspend fun updateSensorEnabledTypes(address: String, enabledTypes: Set<SensorDataType>) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PAIRED_BLE_DEVICES] ?: emptySet()
            val updated = current.map { encoded ->
                if (encoded.contains(address)) {
                    val parts = encoded.split("|")
                    if (parts.size >= 4) {
                        val addr = parts[0]
                        val name = parts[1]
                        val detected = parts[2]
                        val newEnabledStr = enabledTypes.joinToString(",") { it.name }
                        "$addr|$name|$detected|$newEnabledStr"
                    } else if (parts.size == 3) {
                        val addr = parts[1]
                        val name = parts[2]
                        val newEnabledStr = enabledTypes.joinToString(",") { it.name }
                        "$addr|$name|$newEnabledStr|$newEnabledStr"
                    } else encoded
                } else encoded
            }.toSet()
            preferences[KEY_PAIRED_BLE_DEVICES] = updated
        }
    }

    suspend fun updateSensorCapabilities(address: String, detectedCapabilities: Set<SensorDataType>, enabledTypes: Set<SensorDataType>) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PAIRED_BLE_DEVICES] ?: emptySet()
            val updated = current.map { encoded ->
                if (encoded.contains(address)) {
                    val parts = encoded.split("|")
                    if (parts.size >= 4) {
                        val addr = parts[0]
                        val name = parts[1]
                        val detectedStr = detectedCapabilities.joinToString(",") { it.name }
                        val newEnabledStr = enabledTypes.joinToString(",") { it.name }
                        "$addr|$name|$detectedStr|$newEnabledStr"
                    } else if (parts.size == 3) {
                        val addr = parts[1]
                        val name = parts[2]
                        val detectedStr = detectedCapabilities.joinToString(",") { it.name }
                        val newEnabledStr = enabledTypes.joinToString(",") { it.name }
                        "$addr|$name|$detectedStr|$newEnabledStr"
                    } else encoded
                } else encoded
            }.toSet()
            preferences[KEY_PAIRED_BLE_DEVICES] = updated
        }
    }

    suspend fun removePairedDevice(address: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PAIRED_BLE_DEVICES] ?: emptySet()
            preferences[KEY_PAIRED_BLE_DEVICES] = current.filterNot { it.contains(address) }.toSet()
        }
    }

    suspend fun setCockpitTile(slotIndex: Int, metricType: MetricType, sourceId: String? = null) {
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_COCKPIT_TILES]
            val currentList = if (raw.isNullOrBlank()) {
                DEFAULT_TILES.toMutableList()
            } else {
                raw.split(",").mapNotNull {
                    try { MetricType.valueOf(it.trim()) } catch (e: Exception) { null }
                }.toMutableList()
            }
            while (currentList.size < 6) {
                currentList.add(DEFAULT_TILES.getOrElse(currentList.size) { MetricType.DISTANCE })
            }
            if (slotIndex in 0 until currentList.size) {
                currentList[slotIndex] = metricType
                preferences[KEY_COCKPIT_TILES] = currentList.joinToString(",") { it.name }
            }

            if (sourceId != null) {
                val rawSources = preferences[KEY_COCKPIT_TILE_SOURCES] ?: ""
                val currentSources = if (rawSources.isBlank()) {
                    mutableMapOf()
                } else {
                    rawSources.split(";").mapNotNull { entry ->
                        val parts = entry.split("=")
                        if (parts.size == 2) {
                            val idx = parts[0].toIntOrNull()
                            if (idx != null) idx to parts[1] else null
                        } else null
                    }.toMap().toMutableMap()
                }
                currentSources[slotIndex] = sourceId
                preferences[KEY_COCKPIT_TILE_SOURCES] = currentSources.entries.joinToString(";") { "${it.key}=${it.value}" }
            }
        }
    }

    suspend fun setCockpitTileSource(slotIndex: Int, sourceId: String) {
        context.dataStore.edit { preferences ->
            val rawSources = preferences[KEY_COCKPIT_TILE_SOURCES] ?: ""
            val currentSources = if (rawSources.isBlank()) {
                mutableMapOf()
            } else {
                rawSources.split(";").mapNotNull { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) {
                        val idx = parts[0].toIntOrNull()
                        if (idx != null) idx to parts[1] else null
                    } else null
                }.toMap().toMutableMap()
            }
            currentSources[slotIndex] = sourceId
            preferences[KEY_COCKPIT_TILE_SOURCES] = currentSources.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }
}
