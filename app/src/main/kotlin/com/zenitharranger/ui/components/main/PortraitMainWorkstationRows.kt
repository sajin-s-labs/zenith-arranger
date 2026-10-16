package com.zenitharranger.ui.components.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.model.ArrangerSectionType
import com.zenitharranger.ui.ArrangerViewModel

@Composable
fun PortraitMainWorkstationRows(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    viewModel: ArrangerViewModel,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val beatDurationMs = (60_000f / state.bpm.coerceAtLeast(30f)).toInt()
    val transition = rememberInfiniteTransition(label = "portraitBlink")
    val blinkAlpha by transition.animateFloat(
        0.2f, 1.0f, infiniteRepeatable(tween(beatDurationMs / 2, easing = LinearEasing), RepeatMode.Reverse), "blink"
    )

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PortraitTransportContainer(
            state = state,
            bpm = uiState.mainBpm.toInt(),
            syncStart = uiState.isSyncStartArmed,
            syncStop = uiState.isSyncStopActive,
            transpose = uiState.transpose,
            onPlay = { viewModel.togglePlay() },
            onSyncStart = { viewModel.toggleSyncStart() },
            onSyncStop = { viewModel.toggleSyncStop() },
            onBpm = { viewModel.setBpmDelta(it) },
            onTap = { viewModel.tapTempo() },
            onTrsp = { viewModel.setTransposeDelta(it) }
        )
        PortraitSectionsContainer(sections, state, blinkAlpha, { viewModel.triggerSection(it) }, onAssignSample)
        PortraitRegContainer(uiState.transpose, uiState.mainBpm.toInt())
    }
}

@Composable
private fun PortraitTransportContainer(
    state: ArrangerState, bpm: Int, syncStart: Boolean, syncStop: Boolean, transpose: Int,
    onPlay: () -> Unit, onSyncStart: () -> Unit, onSyncStop: () -> Unit,
    onBpm: (Int) -> Unit, onTap: () -> Unit, onTrsp: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF0C1018)).border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp)).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            SoftGlowButton(if (state.isPlaying) "■ STOP" else "▶ START", state.isPlaying, Color(0xFF10B981), onPlay, Modifier.weight(1.3f).fillMaxHeight())
            SoftGlowButton("SYNC ST", syncStart, Color(0xFFFFB03A), onSyncStart, Modifier.weight(1f).fillMaxHeight())
            SoftGlowButton("SYNC SP", syncStop, Color(0xFFFFB03A), onSyncStop, Modifier.weight(1f).fillMaxHeight())
        }
        PortraitTransportRow2(bpm, onBpm, onTap, transpose, onTrsp)
    }
}

@Composable
private fun PortraitTransportRow2(
    bpm: Int, onBpm: (Int) -> Unit, onTap: () -> Unit,
    transpose: Int, onTrsp: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.weight(1.5f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFF141824)).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            PortraitStepBtn("−") { onBpm(-1) }
            Text("$bpm BPM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            PortraitStepBtn("+") { onBpm(1) }
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF232B3C)).clickable { onTap() }.padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("TAP", color = Color(0xFFFFB03A), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            }
            CompactJogWheel(onScrubBpm = onBpm, modifier = Modifier.weight(1f).height(18.dp))
        }
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFF141824)).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TRSP", color = Color(0xFF7B889B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            PortraitStepBtn("−") { onTrsp(-1) }
            val sign = if (transpose > 0) "+$transpose" else "$transpose"
            Text(sign, color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            PortraitStepBtn("+") { onTrsp(1) }
        }
    }
}

@Composable
private fun PortraitSectionsContainer(
    sections: List<ArrangerSection>, state: ArrangerState, blinkAlpha: Float,
    onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF0C1018)).border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp)).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Portrait4SectionRow(listOf("INTRO 1", "INTRO 2", "INTRO 3", "INTRO 4"), ArrangerSectionType.INTRO, sections, state, blinkAlpha, Color(0xFFA78BFA), onTrigger, onAssign)
        Portrait4SectionRow(listOf("END 1", "END 2", "END 3", "END 4"), ArrangerSectionType.ENDING, sections, state, blinkAlpha, Color(0xFFFB923C), onTrigger, onAssign)
        Portrait4SectionRow(listOf("MAIN A", "MAIN B", "MAIN C", "MAIN D"), ArrangerSectionType.MAIN, sections, state, blinkAlpha, Color(0xFF60A5FA), onTrigger, onAssign)
        Portrait4SectionRow(listOf("FILL A", "FILL B", "FILL C", "FILL D"), ArrangerSectionType.FILL, sections, state, blinkAlpha, Color(0xFFF472B6), onTrigger, onAssign)
        Portrait4SectionRow(listOf("USER 1", "USER 2", "USER 3", "USER 4"), ArrangerSectionType.CUSTOM, sections, state, blinkAlpha, Color(0xFF34D399), onTrigger, onAssign)
        PortraitUserBreakRow(sections, state, blinkAlpha, onTrigger, onAssign)
    }
}

@Composable
private fun Portrait4SectionRow(
    labels: List<String>, type: ArrangerSectionType, sections: List<ArrangerSection>,
    state: ArrangerState, blinkAlpha: Float, themeColor: Color,
    onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        labels.forEachIndexed { i, label ->
            val sec = findSection(sections, type, i)
            MatrixHardwareButton(label, sec, state, blinkAlpha, themeColor, type, i, onTrigger, onAssign, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun PortraitUserBreakRow(
    sections: List<ArrangerSection>, state: ArrangerState, blinkAlpha: Float,
    onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        listOf("USER 5", "USER 6", "USER 7").forEachIndexed { i, label ->
            val sec = findSection(sections, ArrangerSectionType.CUSTOM, i + 4)
            MatrixHardwareButton(label, sec, state, blinkAlpha, Color(0xFF34D399), ArrangerSectionType.CUSTOM, i + 4, onTrigger, onAssign, Modifier.weight(1f).fillMaxHeight())
        }
        val breakSec = findSection(sections, ArrangerSectionType.BREAK, 0)
        MatrixHardwareButton("BREAK", breakSec, state, blinkAlpha, Color(0xFFFCD34D), ArrangerSectionType.BREAK, 0, onTrigger, onAssign, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun PortraitRegContainer(transpose: Int, bpm: Int) {
    var voiceSlot by remember { mutableIntStateOf(1) }
    var styleSlot by remember { mutableIntStateOf(1) }
    var isLinked by remember { mutableStateOf(false) }
    var setMode by remember { mutableStateOf(false) }
    var savedTranspose by remember { mutableStateOf<Int?>(null) }
    var savedTempo by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF0C1018)).border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp)).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PortraitRegRow(
            lead = "SET", isLeadLit = setMode, leadColor = Color(0xFFEF4444), onLead = { setMode = !setMode },
            slots = listOf(1, 2, 3), activeSlot = voiceSlot, slotColor = Color(0xFF38BDF8),
            onSlot = { s -> voiceSlot = s; if (isLinked) styleSlot = s; if (setMode) setMode = false }
        )
        PortraitRegTailRow(
            slots = listOf(4, 5, 6), activeSlot = voiceSlot, slotColor = Color(0xFF38BDF8),
            tailLabel = if (savedTranspose != null) "TRSP $savedTranspose" else "TRANSPOSE",
            isTailLit = setMode || savedTranspose != null,
            tailColor = if (setMode) Color(0xFFEF4444) else Color(0xFF38BDF8),
            onSlot = { s -> voiceSlot = s; if (isLinked) styleSlot = s; if (setMode) setMode = false },
            onTail = { if (setMode) { savedTranspose = transpose; setMode = false } }
        )
        PortraitRegRow(
            lead = "LINK", isLeadLit = isLinked, leadColor = Color(0xFFFFB03A), onLead = { isLinked = !isLinked },
            slots = listOf(1, 2, 3), activeSlot = styleSlot, slotColor = Color(0xFF10B981),
            onSlot = { s -> styleSlot = s; if (isLinked) voiceSlot = s; if (setMode) setMode = false }
        )
        PortraitRegTailRow(
            slots = listOf(4, 5, 6), activeSlot = styleSlot, slotColor = Color(0xFF10B981),
            tailLabel = if (savedTempo != null) "$savedTempo BPM" else "TEMPO",
            isTailLit = savedTempo != null, tailColor = Color(0xFF10B981),
            onSlot = { s -> styleSlot = s; if (isLinked) voiceSlot = s; if (setMode) setMode = false },
            onTail = { if (setMode) { savedTempo = bpm; setMode = false } }
        )
    }
}

@Composable
private fun PortraitRegRow(
    lead: String, isLeadLit: Boolean, leadColor: Color, onLead: () -> Unit,
    slots: List<Int>, activeSlot: Int, slotColor: Color, onSlot: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        SoftRegButton(lead, isLeadLit, leadColor, onLead, Modifier.weight(1f).fillMaxHeight())
        slots.forEach { s ->
            SoftRegButton("$s", s == activeSlot, slotColor, { onSlot(s) }, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun PortraitRegTailRow(
    slots: List<Int>, activeSlot: Int, slotColor: Color, tailLabel: String,
    isTailLit: Boolean, tailColor: Color, onSlot: (Int) -> Unit, onTail: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        slots.forEach { s ->
            SoftRegButton("$s", s == activeSlot, slotColor, { onSlot(s) }, Modifier.weight(1f).fillMaxHeight())
        }
        SoftRegButton(tailLabel, isTailLit, tailColor, onTail, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun PortraitStepBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF222B3D)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
