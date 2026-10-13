package com.zenitharranger.ui.components.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import com.zenitharranger.core.audio.ArrangerState

/**
 * Top Row: Master Conductor & Transport Bar
 * Spans full width: [START/STOP] [SYNC ST] [SYNC SP] | [TEMPO - + JOG TAP] | [TRANSPOSE - +]
 */
@Composable
fun ArrangerTransportRow(
    state: ArrangerState,
    onPlayToggle: () -> Unit,
    onBpmDelta: (Int) -> Unit,
    onTapTempo: () -> Unit,
    transpose: Int,
    onTransposeDelta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var syncStart by remember { mutableStateOf(false) }
    var syncStop by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1018))
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playback Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(3.5.dp), verticalAlignment = Alignment.CenterVertically) {
            SoftGlowButton(
                label = if (state.isPlaying) "■ STOP" else "▶ START",
                isLit = state.isPlaying,
                litColor = Color(0xFF10B981),
                onClick = onPlayToggle,
                modifier = Modifier.width(66.dp).height(28.dp)
            )
            SoftGlowButton("SYNC ST", syncStart, Color(0xFFFFB03A), { syncStart = !syncStart }, Modifier.width(56.dp).height(28.dp))
            SoftGlowButton("SYNC SP", syncStop, Color(0xFFFFB03A), { syncStop = !syncStop }, Modifier.width(56.dp).height(28.dp))
        }

        // Tempo Controls
        TempoControlModule(state.bpm.toInt(), onBpmDelta, onTapTempo)

        // Transpose Controls
        TransposeControlModule(transpose, onTransposeDelta)
    }
}

@Composable
fun SoftGlowButton(
    label: String,
    isLit: Boolean,
    litColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isLit) {
        Brush.radialGradient(listOf(litColor.copy(alpha = 0.55f), litColor.copy(alpha = 0.20f), Color(0xFF181D2A)), radius = 60f)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1E2433), Color(0xFF131722)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgBrush)
            .clickable { onClick() }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
            Box(modifier = Modifier.size(4.5.dp).clip(CircleShape).background(if (isLit) litColor else Color(0xFF2C3548)))
            Text(label, color = if (isLit) Color.White else Color(0xFF8E9BAE), fontSize = 8.5.sp, fontWeight = if (isLit) FontWeight.ExtraBold else FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun TempoControlModule(bpm: Int, onBpmDelta: (Int) -> Unit, onTapTempo: () -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141824))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallTransportBox("−") { onBpmDelta(-1) }
        Text("$bpm BPM", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        SmallTransportBox("+") { onBpmDelta(1) }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF232B3C))
                .clickable { onTapTempo() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("TAP", color = Color(0xFFFFB03A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        CompactJogWheel(onScrubBpm = onBpmDelta)
    }
}

@Composable
private fun TransposeControlModule(transpose: Int, onTransposeDelta: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141824))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TRANSPOSE", color = Color(0xFF7B889B), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        SmallTransportBox("−") { onTransposeDelta(-1) }
        val sign = if (transpose > 0) "+$transpose" else "$transpose"
        Text(sign, color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        SmallTransportBox("+") { onTransposeDelta(1) }
    }
}

@Composable
private fun SmallTransportBox(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(17.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF222B3D))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompactJogWheel(onScrubBpm: (Int) -> Unit) {
    var dragAcc by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0A0D14))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    dragAcc += dragAmount
                    offset = (offset + dragAmount) % 10f
                    val step = (dragAcc / 6f).toInt()
                    if (step != 0) {
                        onScrubBpm(step)
                        dragAcc -= (step * 6f)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 7.dp.toPx()
            val total = (size.width / spacing).toInt() + 2
            val startX = (offset % spacing) - spacing
            for (i in 0..total) {
                val x = startX + (i * spacing)
                if (x in 0f..size.width) {
                    drawLine(Color(0xFF333E54), Offset(x, 2.dp.toPx()), Offset(x, size.height - 2.dp.toPx()), 1.5.dp.toPx())
                }
            }
        }
    }
}
