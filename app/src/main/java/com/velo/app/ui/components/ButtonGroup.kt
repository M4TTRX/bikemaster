package com.velo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.ui.theme.getRobotoFlexFont
import com.velo.app.ui.util.performAppClickHaptic

/**
 * Material 3 Expressive Connected Button Group for single-select choices.
 * Implements Material You Expressive guidelines:
 * - Fluid shape morphing: selected button morphs its corners into a standalone pill (24dp),
 *   while unselected buttons maintain connected geometry with subtle inner corners (8dp).
 * - Animated width expansion: the selected item smoothly expands in width while sibling
 *   items contract using expressive spatial spring physics.
 * - High-contrast OLED/Sunlight colors: vibrant primary fill on selection, surfaceContainerHigh
 *   with high-contrast onSurface typography when unselected.
 */
@Composable
fun <T> SingleSelectButtonGroup(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    labelProvider: (T) -> String = { it.toString() },
    iconProvider: ((T) -> ImageVector?)? = null
) {
    val hasSelectionInRow = options.any { it == selectedOption }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selectedOption

            // Target corner radii per M3 Expressive shape morphing guidelines
            val (targetTopStart, targetBottomStart, targetTopEnd, targetBottomEnd) = when {
                options.size == 1 -> listOf(24.dp, 24.dp, 24.dp, 24.dp)
                isSelected -> listOf(24.dp, 24.dp, 24.dp, 24.dp)
                index == 0 -> listOf(24.dp, 24.dp, 8.dp, 8.dp)
                index == options.lastIndex -> listOf(8.dp, 8.dp, 24.dp, 24.dp)
                else -> listOf(8.dp, 8.dp, 8.dp, 8.dp)
            }

            val cornerSpring = spring<Dp>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )

            val animatedTopStart by animateDpAsState(targetValue = targetTopStart, animationSpec = cornerSpring, label = "topStart")
            val animatedBottomStart by animateDpAsState(targetValue = targetBottomStart, animationSpec = cornerSpring, label = "bottomStart")
            val animatedTopEnd by animateDpAsState(targetValue = targetTopEnd, animationSpec = cornerSpring, label = "topEnd")
            val animatedBottomEnd by animateDpAsState(targetValue = targetBottomEnd, animationSpec = cornerSpring, label = "bottomEnd")

            val animatedShape = RoundedCornerShape(
                topStart = animatedTopStart,
                bottomStart = animatedBottomStart,
                topEnd = animatedTopEnd,
                bottomEnd = animatedBottomEnd
            )

            // Target weight per M3 Expressive expansion guidelines
            val targetWeight = when {
                !hasSelectionInRow || options.size <= 1 -> 1f
                isSelected -> if (options.size == 2) 1.25f else 1.3f
                else -> (options.size - (if (options.size == 2) 1.25f else 1.3f)) / (options.size - 1)
            }

            val animatedWeight by animateFloatAsState(
                targetValue = targetWeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "buttonGroupWeight"
            )

            ButtonGroupItem(
                label = labelProvider(option),
                icon = iconProvider?.invoke(option),
                isSelected = isSelected,
                shape = animatedShape,
                weight = animatedWeight,
                fontSize = fontSize,
                onClick = { onOptionSelected(option) }
            )
        }
    }
}

@Composable
private fun RowScope.ButtonGroupItem(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    shape: RoundedCornerShape,
    weight: Float,
    fontSize: TextUnit = 13.sp,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ButtonGroupItemScale"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // High-contrast unselected styling against pure black (#000000) and cards (#1E2125 / #141416)
    val unselectedBgColor = if (isDark) Color(0xFF383C44) else Color(0xFFD6DBE2)
    val unselectedContentColor = if (isDark) Color(0xFFF1F3F7) else Color(0xFF1B1E22)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else unselectedBgColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ButtonGroupBgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else unselectedContentColor,
        label = "ButtonGroupContentColor"
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .height(48.dp)
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performAppClickHaptic()
                    onClick()
                }
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontFamily = getRobotoFlexFont(width = 85f, weight = if (isSelected) 700 else 500),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
