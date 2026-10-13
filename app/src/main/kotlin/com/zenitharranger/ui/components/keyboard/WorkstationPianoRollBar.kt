package com.zenitharranger.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom Workstation Piano Roll Bar:
 * - Arranger Performance Joystick Block
 * - Header controls for Octave shift (◀ OCT ▶) and Key density (KEYS − +)
 * - Stationary multi-touch glissando piano keyboard
 */
@Composable
fun WorkstationPianoRollBar(
    externalActiveNotes: Set<Int> = emptySet(),
    externalPitchBend: Float = 0f,
    externalModulation: Float = 0f,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var startOctave by remember { mutableIntStateOf(3) }
    var octaveSpan by remember(isLandscape) { mutableIntStateOf(if (isLandscape) 3 else 1) }
    var pitchBend by remember { mutableFloatStateOf(0f) }
    var modulation by remember { mutableFloatStateOf(0f) }

    val effectivePitchBend = if (pitchBend != 0f) pitchBend else externalPitchBend
    val effectiveModulation = if (modulation > 0f) modulation else externalModulation
    val keys = remember(startOctave, octaveSpan) { generatePianoKeys(startOctave, octaveSpan) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0F17))
            .border(1.dp, Color(0xFF1E2636), RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PitchModJoyStickModule(
            pitchBend = effectivePitchBend,
            modulation = effectiveModulation,
            onPitchBendChange = { pitchBend = it },
            onModulationChange = { modulation = it }
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyboardUtilityHeader(
                startOctave = startOctave,
                octaveSpan = octaveSpan,
                onOctaveDelta = { delta -> startOctave = (startOctave + delta).coerceIn(1, 5) },
                onSpanDelta = { delta -> octaveSpan = (octaveSpan + delta).coerceIn(1, 3) },
                pitchBend = effectivePitchBend,
                modulation = effectiveModulation
            )

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val whiteKeysCount = remember(keys) { keys.count { !it.isBlack } }
                val exactWhiteKeyWidth = maxWidth / whiteKeysCount.coerceAtLeast(1)

                PianoKeyboardBed(
                    keys = keys,
                    whiteKeyWidth = exactWhiteKeyWidth,
                    pitchBend = effectivePitchBend,
                    modulation = effectiveModulation,
                    externalActiveNotes = externalActiveNotes,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun KeyboardUtilityHeader(
    startOctave: Int,
    octaveSpan: Int,
    onOctaveDelta: (Int) -> Unit,
    onSpanDelta: (Int) -> Unit,
    pitchBend: Float,
    modulation: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF131822))
            .border(0.6.dp, Color(0xFF222C3E), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Octave Navigator
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            SmallKeyNavButton("◀") { onOctaveDelta(-1) }
            Text(
                text = "OCT: C$startOctave - C${startOctave + octaveSpan}",
                color = Color(0xFFFFB03A),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            SmallKeyNavButton("▶") { onOctaveDelta(1) }
        }

        // Pitch & Mod Readout
        if (pitchBend != 0f || modulation > 0f) {
            val pbPercent = (pitchBend * 100).toInt()
            val modPercent = (modulation * 100).toInt()
            Text("PB: $pbPercent% | MOD: $modPercent%", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        }

        // Key Count / Density Zoom
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("KEYS", color = Color(0xFF8895A5), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            SmallKeyNavButton("−") { onSpanDelta(-1) }
            SmallKeyNavButton("+") { onSpanDelta(1) }
        }
    }
}

@Composable
private fun SmallKeyNavButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF202838))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}
