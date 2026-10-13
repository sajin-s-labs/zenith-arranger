package com.zenitharranger.ui.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom Container: Registration Memory Bar
 * Line 1 (Voice): [SET] | [1] [2] [3] [4] [5] [6] | [TRANSPOSE] (All 8 equal size)
 * Line 2 (Style): [LINK] | [1] [2] [3] [4] [5] [6] | [TEMPO] (All 8 equal size)
 */
@Composable
fun ArrangerRegMemoryRow(
    transpose: Int,
    bpm: Int,
    modifier: Modifier = Modifier
) {
    var voiceSlot by remember { mutableIntStateOf(1) }
    var styleSlot by remember { mutableIntStateOf(1) }
    var isLinked by remember { mutableStateOf(false) }
    var setMode by remember { mutableStateOf(false) }
    var savedTranspose by remember { mutableStateOf<Int?>(null) }
    var savedTempo by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1018))
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        VoiceRegLine(
            setMode = setMode,
            onToggleSet = { setMode = !setMode },
            voiceSlot = voiceSlot,
            onVoiceSlot = { slot ->
                voiceSlot = slot
                if (isLinked) styleSlot = slot
                if (setMode) setMode = false
            },
            transpose = transpose,
            savedTranspose = savedTranspose,
            onSaveTranspose = {
                if (setMode) {
                    savedTranspose = transpose
                    setMode = false
                }
            }
        )

        StyleRegLine(
            isLinked = isLinked,
            onToggleLink = { isLinked = !isLinked },
            styleSlot = styleSlot,
            onStyleSlot = { slot ->
                styleSlot = slot
                if (isLinked) voiceSlot = slot
                if (setMode) setMode = false
            },
            bpm = bpm,
            savedTempo = savedTempo,
            onSaveTempo = {
                if (setMode) {
                    savedTempo = bpm
                    setMode = false
                }
            }
        )
    }
}

@Composable
private fun VoiceRegLine(
    setMode: Boolean,
    onToggleSet: () -> Unit,
    voiceSlot: Int,
    onVoiceSlot: (Int) -> Unit,
    transpose: Int,
    savedTranspose: Int?,
    onSaveTranspose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftRegButton("SET", setMode, Color(0xFFEF4444), onToggleSet, Modifier.weight(1f).fillMaxHeight())

        (1..6).forEach { slot ->
            SoftRegButton(
                label = "$slot",
                isLit = slot == voiceSlot,
                litColor = Color(0xFF38BDF8),
                onClick = { onVoiceSlot(slot) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        val trspLabel = if (savedTranspose != null) "TRSP $savedTranspose" else "TRANSPOSE"
        SoftRegButton(
            label = trspLabel,
            isLit = setMode || savedTranspose != null,
            litColor = if (setMode) Color(0xFFEF4444) else Color(0xFF38BDF8),
            onClick = onSaveTranspose,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun StyleRegLine(
    isLinked: Boolean,
    onToggleLink: () -> Unit,
    styleSlot: Int,
    onStyleSlot: (Int) -> Unit,
    bpm: Int,
    savedTempo: Int?,
    onSaveTempo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftRegButton("LINK", isLinked, Color(0xFFFFB03A), onToggleLink, Modifier.weight(1f).fillMaxHeight())

        (1..6).forEach { slot ->
            SoftRegButton(
                label = "$slot",
                isLit = slot == styleSlot,
                litColor = Color(0xFF10B981),
                onClick = { onStyleSlot(slot) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        val tempoLabel = if (savedTempo != null) "$savedTempo BPM" else "TEMPO"
        SoftRegButton(
            label = tempoLabel,
            isLit = savedTempo != null,
            litColor = Color(0xFF10B981),
            onClick = onSaveTempo,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
fun SoftRegButton(
    label: String,
    isLit: Boolean,
    litColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isLit) {
        Brush.radialGradient(listOf(litColor.copy(alpha = 0.65f), litColor.copy(alpha = 0.20f), Color(0xFF141824)), radius = 50f)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF22293A), Color(0xFF131722)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.5.dp))
            .background(bgBrush)
            .clickable { onClick() }
            .padding(horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(if (isLit) litColor else Color(0xFF2B364A)))
            val fontSize = if (label.length > 5) 6.8.sp else 8.5.sp
            Text(label, color = if (isLit) Color.White else Color(0xFF94A3B8), fontSize = fontSize, fontWeight = if (isLit) FontWeight.ExtraBold else FontWeight.Bold, maxLines = 1)
        }
    }
}
