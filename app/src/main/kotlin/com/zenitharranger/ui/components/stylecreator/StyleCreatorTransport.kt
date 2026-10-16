package com.zenitharranger.ui.components.stylecreator

import androidx.compose.animation.core.*
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
import com.zenitharranger.ui.components.main.SoftGlowButton

@Composable
fun StyleCreatorTransport(
    isPlaying: Boolean,
    isLoadMode: Boolean,
    isClearMode: Boolean,
    bpm: Int,
    onPlayToggle: () -> Unit,
    onToggleLoadMode: () -> Unit,
    onToggleClearMode: () -> Unit,
    onBpmDelta: (Int) -> Unit,
    onTapTempo: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            SoftGlowButton(if (isPlaying) "■ STOP" else "▶ START", isPlaying, Color(0xFF10B981), onPlayToggle, Modifier.width(68.dp).height(28.dp))
            LoadSampleArmButton(isLoadMode, onToggleLoadMode, Modifier.width(105.dp).height(28.dp))
            ClearSampleArmButton(isClearMode, onToggleClearMode, Modifier.width(105.dp).height(28.dp))
        }
        StyleCreatorTempoControls(bpm, onBpmDelta, onTapTempo)
    }
}

@Composable
fun LoadSampleArmButton(
    isArmed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadArm")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val bgBrush = if (isArmed) {
        Brush.radialGradient(listOf(Color(0xFFFFB03A).copy(alpha = 0.65f * pulseAlpha), Color(0xFFFFB03A).copy(alpha = 0.22f), Color(0xFF1B1914)), radius = 70f)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF222838), Color(0xFF141924)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgBrush)
            .border(1.dp, if (isArmed) Color(0xFFFFB03A) else Color(0xFF27344A), RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val lampAlpha = if (isArmed) pulseAlpha else 0.4f
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFFFB03A).copy(alpha = lampAlpha)))
            val label = if (isArmed) "LOAD ARMED: TAP SLOT" else "LOAD SAMPLE"
            Text(label, color = if (isArmed) Color.White else Color(0xFFD1D5DB), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun ClearSampleArmButton(
    isArmed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clearArm")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val bgBrush = if (isArmed) {
        Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.65f * pulseAlpha), Color(0xFFEF4444).copy(alpha = 0.22f), Color(0xFF1B1414)), radius = 70f)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF222838), Color(0xFF141924)))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgBrush)
            .border(1.dp, if (isArmed) Color(0xFFEF4444) else Color(0xFF27344A), RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val lampAlpha = if (isArmed) pulseAlpha else 0.4f
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(alpha = lampAlpha)))
            val label = if (isArmed) "CLEAR ARMED" else "CLEAR SAMPLE"
            Text(label, color = if (isArmed) Color.White else Color(0xFFD1D5DB), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun PortraitStyleCreatorTransport(
    isPlaying: Boolean,
    isLoadMode: Boolean,
    isClearMode: Boolean,
    bpm: Int,
    onPlayToggle: () -> Unit,
    onToggleLoadMode: () -> Unit,
    onToggleClearMode: () -> Unit,
    onBpmDelta: (Int) -> Unit,
    onTapTempo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1018))
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Row 1: Playback & Sample Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth().height(30.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoftGlowButton(
                label = if (isPlaying) "■ STOP" else "▶ START",
                isLit = isPlaying,
                litColor = Color(0xFF10B981),
                onClick = onPlayToggle,
                modifier = Modifier.weight(1.1f).fillMaxHeight()
            )
            LoadSampleArmButton(
                isArmed = isLoadMode,
                onToggle = onToggleLoadMode,
                modifier = Modifier.weight(1.4f).fillMaxHeight()
            )
            ClearSampleArmButton(
                isArmed = isClearMode,
                onToggle = onToggleClearMode,
                modifier = Modifier.weight(1.4f).fillMaxHeight()
            )
        }

        // Row 2: Fully visible Tempo Bar with prominent Scrub Jog Wheel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF141824))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportStepBtn("−") { onBpmDelta(-1) }
            Text("$bpm BPM", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            TransportStepBtn("+") { onBpmDelta(1) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF232B3C))
                    .clickable { onTapTempo() }
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Text("TAP", color = Color(0xFFFFB03A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            StyleJogWheel(
                onScrub = onBpmDelta,
                modifier = Modifier.weight(1f).height(20.dp)
            )
        }
    }
}

@Composable
private fun StyleCreatorTempoControls(bpm: Int, onBpmDelta: (Int) -> Unit, onTapTempo: () -> Unit) {
    Row(
        modifier = Modifier.height(28.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF141824)).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        TransportStepBtn("−") { onBpmDelta(-1) }
        Text("$bpm BPM", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        TransportStepBtn("+") { onBpmDelta(1) }
        Box(Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF232B3C)).clickable { onTapTempo() }.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text("TAP", color = Color(0xFFFFB03A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        StyleJogWheel(onScrub = onBpmDelta, modifier = Modifier.width(44.dp).height(20.dp))
    }
}

@Composable
private fun TransportStepBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(19.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF222B3D)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StyleJogWheel(onScrub: (Int) -> Unit, modifier: Modifier = Modifier) {
    var dragAcc by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0A0D14))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    dragAcc += dragAmount
                    offset = (offset + dragAmount) % 10f
                    val step = (dragAcc / 6f).toInt()
                    if (step != 0) {
                        onScrub(step)
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
