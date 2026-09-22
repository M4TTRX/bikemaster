package com.velo.wear.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.velo.wear.data.WatchRideStatus
import com.velo.wear.theme.OledBlack
import com.velo.wear.theme.TimeWhite
import com.velo.wear.theme.getRobotoFlexFont
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CopilotScreen(
    status: WatchRideStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val timeFormat = remember(is24Hour) {
        if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("h:mm", Locale.getDefault())
    }

    var currentTimeString by remember {
        mutableStateOf(timeFormat.format(Date()))
    }

    LaunchedEffect(timeFormat) {
        while (true) {
            currentTimeString = timeFormat.format(Date())
            delay(1000L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status Dot: Green (Connected) | Yellow (Paused) | Red (Not Connected)
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(color = status.dotColor, shape = CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time: Wear OS 6 Roboto Flex
            Text(
                text = currentTimeString,
                color = TimeWhite,
                fontSize = 54.sp,
                fontFamily = getRobotoFlexFont(width = 100f, weight = 500),
                letterSpacing = (-1).sp
            )
        }
    }
}
