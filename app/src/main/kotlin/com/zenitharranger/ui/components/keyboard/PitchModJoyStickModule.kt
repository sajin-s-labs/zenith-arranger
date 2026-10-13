package com.zenitharranger.ui.components.keyboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Arranger Left Performance Control Block:
 * - 3 Top Assignable Buttons: [ASSIGN 1], [ASSIGN 2], [HOLD] with LED indicators.
 * - 2D Tactile Spring-Return Joystick (Pitch Bend X + Modulation Y).
 */
@Composable
fun PitchModJoyStickModule(
    pitchBend: Float,
    modulation: Float,
    onPitchBendChange: (Float) -> Unit,
    onModulationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var assign1Active by remember { mutableStateOf(false) }
    var assign2Active by remember { mutableStateOf(false) }
    var holdActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F121A))
            .border(1.dp, Color(0xFF222938), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 3 Top Assignable Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AssignButton("ASS 1", assign1Active, { assign1Active = !assign1Active }, Modifier.weight(1f))
            AssignButton("ASS 2", assign2Active, { assign2Active = !assign2Active }, Modifier.weight(1f))
            AssignButton("HOLD", holdActive, { holdActive = !holdActive }, Modifier.weight(1f))
        }

        // 4-way Performance Joystick
        PitchModJoystickPad(
            pitchBend = pitchBend,
            modulation = modulation,
            holdModulation = holdActive,
            onPitchBendChange = onPitchBendChange,
            onModulationChange = onModulationChange,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 2.dp)
        )
    }
}

@Composable
private fun AssignButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledColor = if (isActive) Color(0xFFFFB03A) else Color(0xFF2A3345)
    val bgColor = if (isActive) Color(0xFF1E2838) else Color(0xFF141923)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(0.6.dp, if (isActive) Color(0xFFFFB03A) else Color(0xFF283446), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(vertical = 2.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(ledColor)
        )
        Text(
            text = label,
            color = if (isActive) Color(0xFFFFB03A) else Color(0xFF8895A5),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun PitchModJoystickPad(
    pitchBend: Float,
    modulation: Float,
    holdModulation: Boolean,
    onPitchBendChange: (Float) -> Unit,
    onModulationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val stickX = remember { Animatable(0f) }
    val stickY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0A0C12))
            .border(1.dp, Color(0xFF1B2230), RoundedCornerShape(6.dp))
            .pointerInput(holdModulation) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            stickX.animateTo(0f, tween(180))
                            onPitchBendChange(0f)
                            if (!holdModulation) {
                                stickY.animateTo(0f, tween(180))
                                onModulationChange(0f)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = (stickX.value + dragAmount.x / 40f).coerceIn(-1f, 1f)
                        val newY = (stickY.value - dragAmount.y / 40f).coerceIn(0f, 1f)
                        coroutineScope.launch {
                            stickX.snapTo(newX)
                            stickY.snapTo(newY)
                        }
                        onPitchBendChange(newX)
                        onModulationChange(newY)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 6.dp.toPx()

            // Outer recessed joystick guide ring
            drawCircle(color = Color(0xFF141924), radius = radius, center = center)
            drawCircle(color = Color(0xFF283446), radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

            // Crosshair guide axes
            drawLine(Color(0xFF222C3D), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.5f)
            drawLine(Color(0xFF222C3D), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.5f)

            // Tactile Joystick Nub position
            val nubX = center.x + stickX.value * (radius * 0.7f)
            val nubY = center.y - stickY.value * (radius * 0.7f)
            val nubRadius = 11.dp.toPx()

            // Stick stem connection
            drawLine(Color(0xFF48566E), center, Offset(nubX, nubY), strokeWidth = 3.dp.toPx())

            // Textured Arranger Nub Cap
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF404E64), Color(0xFF1B2330)), center = Offset(nubX, nubY), radius = nubRadius),
                radius = nubRadius,
                center = Offset(nubX, nubY)
            )
            drawCircle(color = Color(0xFFFFB03A), radius = 2.5.dp.toPx(), center = Offset(nubX, nubY))
        }
    }
}
