package com.zenitharranger.ui.components.main

import androidx.compose.animation.core.*
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
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.model.ArrangerSectionType

/**
 * Middle Row: Arranger Style Sections Matrix
 * - Row 1: 4 INTRO | 4 MAIN
 * - Row 2: 4 ENDING | 4 FILL
 * - Row 3: 7 USER | 1 BREAK
 */
@Composable
fun ArrangerSectionsMatrixRow(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    onTriggerSection: (String) -> Unit,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val beatDurationMs = (60_000f / state.bpm.coerceAtLeast(30f)).toInt()
    val infiniteTransition = rememberInfiniteTransition(label = "armedBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(beatDurationMs / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1018))
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        IntroMainSubRow(state, sections, blinkAlpha, onTriggerSection, onAssignSample)
        EndingFillSubRow(state, sections, blinkAlpha, onTriggerSection, onAssignSample)
        CustomBreakSubRow(state, sections, blinkAlpha, onTriggerSection, onAssignSample)
    }
}

@Composable
private fun IntroMainSubRow(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    blinkAlpha: Float,
    onTrigger: (String) -> Unit,
    onAssign: (ArrangerSection) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        listOf("INTRO 1", "INTRO 2", "INTRO 3", "INTRO 4").forEachIndexed { i, label ->
            val sec = findSection(sections, ArrangerSectionType.INTRO, i)
            MatrixHardwareButton(label, sec, state, blinkAlpha, Color(0xFFA78BFA), ArrangerSectionType.INTRO, i, onTrigger, onAssign, Modifier.weight(1f).height(34.dp))
        }
        listOf("MAIN A", "MAIN B", "MAIN C", "MAIN D").forEachIndexed { i, label ->
            val sec = findSection(sections, ArrangerSectionType.MAIN, i)
            MatrixHardwareButton(label, sec, state, blinkAlpha, Color(0xFF60A5FA), ArrangerSectionType.MAIN, i, onTrigger, onAssign, Modifier.weight(1f).height(34.dp))
        }
    }
}

@Composable
private fun EndingFillSubRow(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    blinkAlpha: Float,
    onTrigger: (String) -> Unit,
    onAssign: (ArrangerSection) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        listOf("END 1", "END 2", "END 3", "END 4").forEachIndexed { i, label ->
            val sec = findSection(sections, ArrangerSectionType.ENDING, i)
            MatrixHardwareButton(label, sec, state, blinkAlpha, Color(0xFFFB923C), ArrangerSectionType.ENDING, i, onTrigger, onAssign, Modifier.weight(1f).height(34.dp))
        }
        listOf("FILL A", "FILL B", "FILL C", "FILL D").forEachIndexed { i, label ->
            val sec = findSection(sections, ArrangerSectionType.FILL, i)
            MatrixHardwareButton(label, sec, state, blinkAlpha, Color(0xFFF472B6), ArrangerSectionType.FILL, i, onTrigger, onAssign, Modifier.weight(1f).height(34.dp))
        }
    }
}

@Composable
private fun CustomBreakSubRow(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    blinkAlpha: Float,
    onTrigger: (String) -> Unit,
    onAssign: (ArrangerSection) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        listOf("USER 1", "USER 2", "USER 3", "USER 4", "USER 5", "USER 6", "USER 7").forEachIndexed { i, label ->
            val sec = findSection(sections, ArrangerSectionType.CUSTOM, i)
            MatrixHardwareButton(label, sec, state, blinkAlpha, Color(0xFF34D399), ArrangerSectionType.CUSTOM, i, onTrigger, onAssign, Modifier.weight(1f).height(34.dp))
        }
        val breakSec = findSection(sections, ArrangerSectionType.BREAK, 0)
        MatrixHardwareButton("BREAK", breakSec, state, blinkAlpha, Color(0xFFFCD34D), ArrangerSectionType.BREAK, 0, onTrigger, onAssign, Modifier.weight(1f).height(34.dp))
    }
}

@Composable
fun MatrixHardwareButton(
    label: String,
    section: ArrangerSection?,
    state: ArrangerState,
    blinkAlpha: Float,
    themeColor: Color,
    slotType: ArrangerSectionType = ArrangerSectionType.CUSTOM,
    slotIndex: Int = 0,
    onTrigger: (String) -> Unit,
    onAssign: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBreak = label == "BREAK"
    val isAssigned = section != null || isBreak
    val isActive = (section != null && state.activeSlot == section.id) || (isBreak && (state.activeSlot == "break_1" || state.activeSlot == "sec_break"))
    val isQueued = (section != null && state.queuedSlot == section.id) || (isBreak && (state.queuedSlot == "break_1" || state.queuedSlot == "sec_break"))

    val bgBrush = when {
        isActive -> Brush.radialGradient(listOf(themeColor.copy(alpha = 0.65f), themeColor.copy(alpha = 0.22f), Color(0xFF131724)), radius = 55f)
        isQueued -> Brush.radialGradient(listOf(themeColor.copy(alpha = 0.65f * blinkAlpha), themeColor.copy(alpha = 0.20f * blinkAlpha), Color(0xFF131724)), radius = 55f)
        isAssigned -> Brush.verticalGradient(listOf(Color(0xFF222838), Color(0xFF151924)))
        else -> Brush.verticalGradient(listOf(Color(0xFF141720), Color(0xFF0F1118)))
    }

    val textColor = when {
        isActive || isQueued -> Color.White
        isAssigned -> Color(0xFFD1D5DB)
        else -> Color(0xFF4B5563)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.5.dp))
            .background(bgBrush)
            .clickable {
                if (section != null) onTrigger(section.id)
                else if (isBreak) onTrigger("break_1")
                else {
                    val bars = if (slotType == ArrangerSectionType.FILL || slotType == ArrangerSectionType.BREAK) 1 else 2
                    val id = "${slotType.name.lowercase()}_${slotIndex + 1}"
                    onAssign(ArrangerSection(id = id, name = label, type = slotType, bars = bars))
                }
            }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (isAssigned) {
                val ledAlpha = if (isQueued) blinkAlpha else if (isActive) 1f else 0.45f
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(themeColor.copy(alpha = ledAlpha)))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 8.sp,
                fontWeight = if (isActive || isQueued) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

fun findSection(sections: List<ArrangerSection>, type: ArrangerSectionType, index: Int): ArrangerSection? {
    val targetIndexNum = index + 1 // 1-based index (1, 2, 3, 4...)
    val targetLetter = ('A' + index).lowercaseChar() // 'a', 'b', 'c', 'd'

    // 1. Precise Match by canonical slot ID or section name
    val match = sections.firstOrNull { s ->
        val id = s.id.lowercase()
        val name = s.name.lowercase().trim()

        when (type) {
            ArrangerSectionType.MAIN -> {
                id == "main_$targetIndexNum" || id == "main_$targetLetter" ||
                id == "sec_main_$targetIndexNum" || id == "sec_main_$targetLetter" ||
                name == "main $targetLetter" || name == "main $targetIndexNum"
            }
            ArrangerSectionType.FILL -> {
                id == "fill_$targetIndexNum" || id == "fill_$targetLetter" ||
                id == "sec_fill_$targetIndexNum" || id == "sec_fill_$targetLetter" ||
                name == "fill $targetLetter" || name == "fill $targetIndexNum"
            }
            ArrangerSectionType.INTRO -> {
                id == "intro_$targetIndexNum" || id == "sec_intro_$targetIndexNum" ||
                name == "intro $targetIndexNum" || (index == 0 && name == "intro")
            }
            ArrangerSectionType.ENDING -> {
                id == "ending_$targetIndexNum" || id == "sec_ending_$targetIndexNum" || id == "sec_end_$targetIndexNum" ||
                name == "end $targetIndexNum" || name == "ending $targetIndexNum" || (index == 0 && name == "ending")
            }
            ArrangerSectionType.CUSTOM -> {
                id == "custom_$targetIndexNum" || id == "sec_user_$targetIndexNum" ||
                name == "user $targetIndexNum" || name == "custom $targetIndexNum"
            }
            ArrangerSectionType.BREAK -> {
                s.type == ArrangerSectionType.BREAK || id == "break_1" || id == "sec_break" || name == "break"
            }
        }
    }
    if (match != null) return match

    // 2. Strict ID suffix or name suffix match if type matches
    val ofType = sections.filter { it.type == type }
    val suffixMatch = ofType.firstOrNull { s ->
        val id = s.id.lowercase()
        val name = s.name.lowercase().trim()
        id.endsWith("_$targetIndexNum") || id.endsWith("_$targetLetter") ||
        name.endsWith(" $targetLetter") || name.endsWith(" $targetIndexNum")
    }
    if (suffixMatch != null) return suffixMatch

    // 3. Fallback: only if sections were saved without standard names and ofType size matches full set
    return if (ofType.size > index && ofType.none { it.id.contains("_") }) ofType.getOrNull(index) else null
}
