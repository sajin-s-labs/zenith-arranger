package com.zenitharranger.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.core.audio.ArrangerState

/**
 * Master transport bar featuring Start/Stop and a continuous jog wheel for rapid tempo scrubbing.
 */
@Composable
fun TransportControlBar(
    state: ArrangerState,
    onPlayToggle: () -> Unit,
    onBpmDelta: (Int) -> Unit,
    onTapTempo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14161F))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Master START / STOP Button
        Button(
            onClick = onPlayToggle,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isPlaying) Color(0xFFFFB03A) else Color(0xFF222633)
            ),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = if (state.isPlaying) "■ STOP" else "▶ START",
                color = if (state.isPlaying) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tempo Module: [-], BPM Readout, [+], TAP, and Tactile Jog Wheel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallTransportButton(text = "−") { onBpmDelta(-1) }

            Text(
                text = "${state.bpm.toInt()} BPM",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            SmallTransportButton(text = "+") { onBpmDelta(1) }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF222633))
                    .clickable { onTapTempo() }
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Text("TAP", color = Color(0xFFFFB03A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Tactile Horizontal Tempo Jog Wheel
            TempoJogWheel(onScrubBpm = onBpmDelta)
        }
    }
}

/**
 * Ribbed horizontal jog wheel enabling fast tempo scrubbing (e.g., 120 -> 160 in one swipe).
 */
@Composable
private fun TempoJogWheel(
    onScrubBpm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var wheelOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(75.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF0F121A))
            .border(1.dp, Color(0xFF272F3E), RoundedCornerShape(5.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    accumulatedDrag += dragAmount
                    wheelOffset = (wheelOffset + dragAmount) % 16f

                    val step = (accumulatedDrag / 8f).toInt()
                    if (step != 0) {
                        onScrubBpm(step)
                        accumulatedDrag -= (step * 8f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ribSpacing = 10.dp.toPx()
            val totalRibs = (size.width / ribSpacing).toInt() + 2
            val startX = (wheelOffset % ribSpacing) - ribSpacing

            for (i in 0..totalRibs) {
                val x = startX + (i * ribSpacing)
                if (x in 0f..size.width) {
                    drawLine(
                        color = Color(0xFF384358),
                        start = Offset(x, 3.dp.toPx()),
                        end = Offset(x, size.height - 3.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallTransportButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF222633))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
