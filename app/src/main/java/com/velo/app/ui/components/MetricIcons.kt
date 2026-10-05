package com.velo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import com.velo.app.model.MetricType

/**
 * Returns the recognizable icon associated with each telemetry metric.
 */
fun getMetricIcon(metricType: MetricType): ImageVector = when (metricType) {
    MetricType.SPEED, MetricType.AVG_SPEED, MetricType.MAX_SPEED -> Icons.Default.Speed
    MetricType.CADENCE -> Icons.AutoMirrored.Filled.DirectionsBike
    MetricType.HEART_RATE -> Icons.Default.Favorite
    MetricType.POWER, MetricType.AVG_POWER, MetricType.MAX_POWER -> Icons.Default.Bolt
    MetricType.DISTANCE -> Icons.Default.Route
    MetricType.ELAPSED_TIME -> Icons.Default.HourglassBottom
    MetricType.ELEVATION -> Icons.Default.Terrain
}
