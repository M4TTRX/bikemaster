package com.velo.wear.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun VeloWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}
