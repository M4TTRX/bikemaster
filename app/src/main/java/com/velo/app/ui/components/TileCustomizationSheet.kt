package com.velo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.data.PairedBleDevice
import com.velo.app.model.MetricType
import com.velo.app.model.SensorDataType
import com.velo.app.ui.theme.getRobotoFlexFont
import com.velo.app.ui.util.performAppClickHaptic

data class CompatibleSource(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileCustomizationSheet(
    slotIndex: Int,
    currentMetric: MetricType,
    currentSourceId: String?,
    pairedDevices: List<PairedBleDevice>,
    allCockpitTiles: List<MetricType> = emptyList(),
    onSave: (MetricType, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMetric by remember { mutableStateOf(currentMetric) }
    var selectedSourceId by remember { mutableStateOf(currentSourceId ?: "AUTO") }

    val compatibleSources = remember(selectedMetric, pairedDevices) {
        getCompatibleSources(selectedMetric, pairedDevices)
    }

    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customize Tile ${slotIndex + 1}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 700)
                    )
                    Text(
                        text = "Select metric and compatible data source",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                    )
                }

                IconButton(
                    onClick = {
                        view.performAppClickHaptic()
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 1: WHAT DATA IS DISPLAYED
            Text(
                text = "DISPLAY METRIC",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontFamily = getRobotoFlexFont(width = 100f, weight = 700),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val metricRows = remember {
                listOf(
                    listOf(MetricType.POWER, MetricType.AVG_POWER, MetricType.MAX_POWER),
                    listOf(MetricType.AVG_SPEED, MetricType.MAX_SPEED),
                    listOf(MetricType.CADENCE, MetricType.HEART_RATE),
                    listOf(MetricType.DISTANCE, MetricType.ELAPSED_TIME, MetricType.ELEVATION)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metricRows.forEach { rowMetrics ->
                    SingleSelectButtonGroup(
                        options = rowMetrics,
                        selectedOption = selectedMetric,
                        onOptionSelected = { metric ->
                            selectedMetric = metric
                            // Reset source if current source is incompatible with new metric
                            val newCompat = getCompatibleSources(metric, pairedDevices)
                            if (newCompat.none { it.id == selectedSourceId }) {
                                selectedSourceId = newCompat.firstOrNull()?.id ?: "AUTO"
                            }
                        },
                        labelProvider = { it.title },
                        iconProvider = { getMetricIcon(it) }
                    )
                }
            }

            // Warning if selected metric is already visible on another cockpit tile
            val duplicateTileIndices = remember(selectedMetric, allCockpitTiles, slotIndex) {
                allCockpitTiles.indices.filter { it != slotIndex && allCockpitTiles[it] == selectedMetric }
            }

            if (duplicateTileIndices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E2010))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    val tileDesc = if (duplicateTileIndices.size == 1) {
                        "Tile ${duplicateTileIndices.first() + 1}"
                    } else {
                        "Tiles ${duplicateTileIndices.map { it + 1 }.joinToString(", ")}"
                    }
                    Text(
                        text = "${selectedMetric.title} is already visible on $tileDesc",
                        color = Color(0xFFFFCC80),
                        fontSize = 13.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
                    )
                }
            }

            // SECTION 2: DATA SOURCE (ONLY WHEN MULTIPLE COMPATIBLE SOURCES EXIST)
            if (compatibleSources.size > 1) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "DATA SOURCE (${compatibleSources.size} Compatible)",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontFamily = getRobotoFlexFont(width = 100f, weight = 700),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    compatibleSources.forEachIndexed { index, source ->
                        val isSelected = selectedSourceId == source.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    view.performAppClickHaptic()
                                    selectedSourceId = source.id
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = source.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                                )
                                Text(
                                    text = source.subtitle,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (index < compatibleSources.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button: Apply / Save
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        view.performAppClickHaptic()
                        val effectiveSourceId = if (compatibleSources.size <= 1) {
                            compatibleSources.firstOrNull()?.id ?: "AUTO"
                        } else {
                            selectedSourceId
                        }
                        onSave(selectedMetric, effectiveSourceId)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Apply to Tile ${slotIndex + 1}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 16.sp,
                    fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun getCompatibleSources(
    metric: MetricType,
    pairedDevices: List<PairedBleDevice>
): List<CompatibleSource> {
    return when (metric) {
        MetricType.SPEED, MetricType.AVG_SPEED, MetricType.MAX_SPEED -> {
            val list = mutableListOf(
                CompatibleSource("AUTO", "Auto (Sensor Priority)", "Uses speed sensor if connected, falls back to GPS", Icons.Default.Speed),
                CompatibleSource("GPS", "Phone GPS", "Tracks velocity and location via phone GPS", Icons.Default.LocationOn)
            )
            pairedDevices.filter { it.enabledDataTypes.contains(SensorDataType.SPEED) }.forEach { dev ->
                list.add(CompatibleSource(dev.address, dev.name, "Physical Wheel Speed Sensor (${dev.address.takeLast(5)})", Icons.AutoMirrored.Filled.DirectionsBike))
            }
            list
        }
        MetricType.CADENCE -> {
            val list = mutableListOf<CompatibleSource>()
            list.add(CompatibleSource("AUTO", "Auto (Connected Sensor)", "Uses any connected cadence or power crank sensor", Icons.AutoMirrored.Filled.DirectionsBike))
            pairedDevices.filter { it.enabledDataTypes.contains(SensorDataType.CADENCE) }.forEach { dev ->
                list.add(CompatibleSource(dev.address, dev.name, "Cadence Provider (${dev.address.takeLast(5)})", Icons.AutoMirrored.Filled.DirectionsBike))
            }
            list
        }
        MetricType.POWER, MetricType.AVG_POWER, MetricType.MAX_POWER -> {
            val list = mutableListOf<CompatibleSource>()
            list.add(CompatibleSource("AUTO", "Auto (Connected Power Meter)", "Uses any connected Cycling Power sensor (0x1818)", Icons.Default.Bolt))
            pairedDevices.filter { it.enabledDataTypes.contains(SensorDataType.POWER) }.forEach { dev ->
                list.add(CompatibleSource(dev.address, dev.name, "Power Meter (${dev.address.takeLast(5)})", Icons.Default.Bolt))
            }
            list
        }
        MetricType.HEART_RATE -> {
            val list = mutableListOf<CompatibleSource>()
            list.add(CompatibleSource("AUTO", "Auto (HRM / Watch)", "Uses any connected BLE Heart Rate monitor or Smartwatch", Icons.Default.Favorite))
            pairedDevices.filter { it.enabledDataTypes.contains(SensorDataType.HEART_RATE) }.forEach { dev ->
                list.add(CompatibleSource(dev.address, dev.name, "BLE Heart Rate Strap (${dev.address.takeLast(5)})", Icons.Default.Favorite))
            }
            list
        }
        MetricType.DISTANCE -> listOf(
            CompatibleSource("CALCULATED", "Ride Distance", "Accumulated from active Speed / GPS source", Icons.Default.Route)
        )
        MetricType.ELAPSED_TIME -> listOf(
            CompatibleSource("TIMER", "Ride Stopwatch", "Active recording elapsed time", Icons.Default.HourglassBottom)
        )
        MetricType.ELEVATION -> listOf(
            CompatibleSource("ALTIMETER", "Barometer / GPS Altitude", "Cumulative elevation gain", Icons.Default.Terrain)
        )
    }
}

