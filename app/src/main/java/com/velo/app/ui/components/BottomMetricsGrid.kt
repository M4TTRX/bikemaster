package com.velo.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.MetricType
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.ui.theme.getRobotoFlexFont
import java.util.Locale

@Composable
fun BottomMetricsGrid(
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    tiles: List<MetricType>,
    onTileLongClick: (slotIndex: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectiveTiles = remember(tiles) {
        if (tiles.size >= 6) tiles else listOf(
            MetricType.DISTANCE,
            MetricType.ELAPSED_TIME,
            MetricType.CADENCE,
            MetricType.POWER,
            MetricType.HEART_RATE,
            MetricType.MAX_SPEED
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ROW 1: Slots 0 & 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ConfigurableMetricSlot(
                slotIndex = 0,
                metricType = effectiveTiles[0],
                telemetry = telemetry,
                unitSystem = unitSystem,
                onLongClick = { onTileLongClick(0) },
                modifier = Modifier.weight(1f)
            )
            ConfigurableMetricSlot(
                slotIndex = 1,
                metricType = effectiveTiles[1],
                telemetry = telemetry,
                unitSystem = unitSystem,
                onLongClick = { onTileLongClick(1) },
                modifier = Modifier.weight(1f)
            )
        }

        // ROW 2: Slots 2 & 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ConfigurableMetricSlot(
                slotIndex = 2,
                metricType = effectiveTiles[2],
                telemetry = telemetry,
                unitSystem = unitSystem,
                onLongClick = { onTileLongClick(2) },
                modifier = Modifier.weight(1f)
            )
            ConfigurableMetricSlot(
                slotIndex = 3,
                metricType = effectiveTiles[3],
                telemetry = telemetry,
                unitSystem = unitSystem,
                onLongClick = { onTileLongClick(3) },
                modifier = Modifier.weight(1f)
            )
        }

        // ROW 3: Slots 4 & 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ConfigurableMetricSlot(
                slotIndex = 4,
                metricType = effectiveTiles[4],
                telemetry = telemetry,
                unitSystem = unitSystem,
                onLongClick = { onTileLongClick(4) },
                modifier = Modifier.weight(1f)
            )
            ConfigurableMetricSlot(
                slotIndex = 5,
                metricType = effectiveTiles[5],
                telemetry = telemetry,
                unitSystem = unitSystem,
                onLongClick = { onTileLongClick(5) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConfigurableMetricSlot(
    slotIndex: Int,
    metricType: MetricType,
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slotScale"
    )

    val (formattedValue, unit) = formatMetric(metricType, telemetry, unitSystem)

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = {}
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = metricType.title.uppercase(Locale.US),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = getRobotoFlexFont(width = 100f, weight = 600),
            letterSpacing = 1.sp
        )

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formattedValue,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 44.sp,
                fontFamily = getRobotoFlexFont(width = 65f, weight = 700),
                lineHeight = 44.sp
            )

            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = getRobotoFlexFont(width = 100f, weight = 500),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

private fun formatMetric(
    metricType: MetricType,
    telemetry: RideTelemetry,
    unitSystem: UnitSystem
): Pair<String, String> {
    val isMetric = unitSystem == UnitSystem.METRIC
    return when (metricType) {
        MetricType.SPEED -> {
            val speed = if (isMetric) telemetry.speedKmh else telemetry.speedKmh * 0.621371f
            val formatted = String.format(Locale.US, "%.1f", speed)
            val unit = if (isMetric) "km/h" else "mph"
            Pair(formatted, unit)
        }
        MetricType.CADENCE -> {
            val formatted = if (telemetry.cadenceRpm > 0) telemetry.cadenceRpm.toString() else "--"
            Pair(formatted, "rpm")
        }
        MetricType.HEART_RATE -> {
            val formatted = if (telemetry.heartRateBpm > 0) telemetry.heartRateBpm.toString() else "--"
            Pair(formatted, "bpm")
        }
        MetricType.POWER -> {
            val formatted = if (telemetry.powerWatts > 0) telemetry.powerWatts.toString() else "--"
            Pair(formatted, "W")
        }
        MetricType.DISTANCE -> {
            val dist = if (isMetric) telemetry.distanceMeters / 1000.0 else (telemetry.distanceMeters / 1000.0) * 0.621371
            val formatted = String.format(Locale.US, "%.1f", dist)
            val unit = if (isMetric) "km" else "mi"
            Pair(formatted, unit)
        }
        MetricType.ELAPSED_TIME -> {
            val hours = telemetry.elapsedTimeSeconds / 3600
            val minutes = (telemetry.elapsedTimeSeconds % 3600) / 60
            val seconds = telemetry.elapsedTimeSeconds % 60
            val formatted = if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
            Pair(formatted, "")
        }
        MetricType.AVG_SPEED -> {
            val avg = if (isMetric) telemetry.avgSpeedKmh else telemetry.avgSpeedKmh * 0.621371f
            val formatted = String.format(Locale.US, "%.1f", avg)
            Pair(formatted, "avg")
        }
        MetricType.MAX_SPEED -> {
            val max = if (isMetric) telemetry.maxSpeedKmh else telemetry.maxSpeedKmh * 0.621371f
            val formatted = String.format(Locale.US, "%.1f", max)
            Pair(formatted, "max")
        }
        MetricType.AVG_POWER -> {
            val formatted = if (telemetry.avgPowerWatts > 0) telemetry.avgPowerWatts.toString() else "--"
            Pair(formatted, "avg W")
        }
        MetricType.MAX_POWER -> {
            val formatted = if (telemetry.maxPowerWatts > 0) telemetry.maxPowerWatts.toString() else "--"
            Pair(formatted, "max W")
        }
        MetricType.ELEVATION -> {
            val ele = if (isMetric) telemetry.elevationGainMeters.toInt() else (telemetry.elevationGainMeters * 3.28084f).toInt()
            val formatted = "+$ele"
            val unit = if (isMetric) "m" else "ft"
            Pair(formatted, unit)
        }
    }
}
