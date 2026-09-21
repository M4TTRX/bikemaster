package com.velo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.ui.theme.MetricLabelGrey
import com.velo.app.ui.theme.MetricUnitGrey
import com.velo.app.ui.theme.MetricValueWhite
import com.velo.app.ui.theme.getRobotoFlexFont
import java.util.Locale

@Composable
fun BottomMetricsGrid(
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val distanceFormatted = if (unitSystem == UnitSystem.METRIC) {
        String.format(Locale.US, "%.1f", telemetry.distanceMeters / 1000.0)
    } else {
        String.format(Locale.US, "%.1f", (telemetry.distanceMeters / 1000.0) * 0.621371)
    }
    val distanceUnit = if (unitSystem == UnitSystem.METRIC) "km" else "mi"

    val hours = telemetry.elapsedTimeSeconds / 3600
    val minutes = (telemetry.elapsedTimeSeconds % 3600) / 60
    val seconds = telemetry.elapsedTimeSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    val avgSpeedFormatted = if (unitSystem == UnitSystem.METRIC) {
        String.format(Locale.US, "%.1f", telemetry.avgSpeedKmh)
    } else {
        String.format(Locale.US, "%.1f", telemetry.avgSpeedKmh * 0.621371f)
    }

    val elevationGainFormatted = if (unitSystem == UnitSystem.METRIC) {
        "+${telemetry.elevationGainMeters.toInt()}"
    } else {
        "+${(telemetry.elevationGainMeters * 3.28084f).toInt()}"
    }
    val elevationUnit = if (unitSystem == UnitSystem.METRIC) "m" else "ft"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ROW 1: DISTANCE & TIME
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricSlot(
                label = "DISTANCE",
                value = distanceFormatted,
                unit = distanceUnit,
                modifier = Modifier.weight(1f)
            )
            MetricSlot(
                label = "TIME",
                value = timeFormatted,
                unit = "",
                modifier = Modifier.weight(1f)
            )
        }

        // ROW 2: CADENCE & AVG SPEED
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricSlot(
                label = "CADENCE",
                value = if (telemetry.cadenceRpm > 0) telemetry.cadenceRpm.toString() else "--",
                unit = "rpm",
                modifier = Modifier.weight(1f)
            )
            MetricSlot(
                label = "AVG SPEED",
                value = avgSpeedFormatted,
                unit = "avg",
                modifier = Modifier.weight(1f)
            )
        }

        // ROW 3: ELEVATION & POWER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricSlot(
                label = "ELEVATION",
                value = elevationGainFormatted,
                unit = elevationUnit,
                modifier = Modifier.weight(1f)
            )
            MetricSlot(
                label = "POWER",
                value = if (telemetry.powerWatts > 0) telemetry.powerWatts.toString() else "--",
                unit = "W",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricSlot(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            color = MetricLabelGrey,
            fontSize = 11.sp,
            fontFamily = getRobotoFlexFont(width = 100f, weight = 600),
            letterSpacing = 1.sp
        )

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = MetricValueWhite,
                fontSize = 44.sp,
                fontFamily = getRobotoFlexFont(width = 65f, weight = 700),
                lineHeight = 44.sp
            )

            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = MetricUnitGrey,
                    fontSize = 14.sp,
                    fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
                )
            }
        }
    }
}
