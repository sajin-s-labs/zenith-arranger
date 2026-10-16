package com.zenitharranger.ui.components.stylecreator

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

data class CreatorSlotDef(val label: String, val slotId: String, val type: ArrangerSectionType, val bars: Int)

@Composable
fun StyleCreatorSectionsMatrix(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    isLoadMode: Boolean,
    isClearMode: Boolean,
    onTriggerSection: (String) -> Unit,
    onAssignSample: (ArrangerSection) -> Unit,
    onRemoveSample: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val beatDurationMs = (60_000f / state.bpm.coerceAtLeast(30f)).toInt()
    val infiniteTransition = rememberInfiniteTransition(label = "styleArmedBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        0.2f, 1.0f, infiniteRepeatable(tween(beatDurationMs / 2, easing = LinearEasing), RepeatMode.Reverse), "blink"
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
        val intros = listOf(
            CreatorSlotDef("INTRO 1", "intro_1", ArrangerSectionType.INTRO, 2),
            CreatorSlotDef("INTRO 2", "intro_2", ArrangerSectionType.INTRO, 2),
            CreatorSlotDef("INTRO 3", "intro_3", ArrangerSectionType.INTRO, 2),
            CreatorSlotDef("INTRO 4", "intro_4", ArrangerSectionType.INTRO, 2)
        )
        val mains = listOf(
            CreatorSlotDef("MAIN A", "main_1", ArrangerSectionType.MAIN, 2),
            CreatorSlotDef("MAIN B", "main_2", ArrangerSectionType.MAIN, 2),
            CreatorSlotDef("MAIN C", "main_3", ArrangerSectionType.MAIN, 2),
            CreatorSlotDef("MAIN D", "main_4", ArrangerSectionType.MAIN, 2)
        )
        MatrixSubRow(intros, mains, sections, state, isLoadMode, isClearMode, blinkAlpha, Color(0xFFA78BFA), Color(0xFF60A5FA), onTriggerSection, onAssignSample, onRemoveSample)

        val endings = listOf(
            CreatorSlotDef("END 1", "ending_1", ArrangerSectionType.ENDING, 2),
            CreatorSlotDef("END 2", "ending_2", ArrangerSectionType.ENDING, 2),
            CreatorSlotDef("END 3", "ending_3", ArrangerSectionType.ENDING, 2),
            CreatorSlotDef("END 4", "ending_4", ArrangerSectionType.ENDING, 2)
        )
        val fills = listOf(
            CreatorSlotDef("FILL A", "fill_1", ArrangerSectionType.FILL, 1),
            CreatorSlotDef("FILL B", "fill_2", ArrangerSectionType.FILL, 1),
            CreatorSlotDef("FILL C", "fill_3", ArrangerSectionType.FILL, 1),
            CreatorSlotDef("FILL D", "fill_4", ArrangerSectionType.FILL, 1)
        )
        MatrixSubRow(endings, fills, sections, state, isLoadMode, isClearMode, blinkAlpha, Color(0xFFFB923C), Color(0xFFF472B6), onTriggerSection, onAssignSample, onRemoveSample)

        UserBreakSubRow(sections, state, isLoadMode, isClearMode, blinkAlpha, onTriggerSection, onAssignSample, onRemoveSample)
    }
}

@Composable
private fun MatrixSubRow(
    leftSlots: List<CreatorSlotDef>, rightSlots: List<CreatorSlotDef>,
    sections: List<ArrangerSection>, state: ArrangerState, isLoad: Boolean, isClear: Boolean,
    blinkAlpha: Float, leftColor: Color, rightColor: Color,
    onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit, onRemove: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        leftSlots.forEach { def ->
            val sec = findSlotSection(sections, def)
            StyleMatrixBtn(def.label, sec, state, isLoad, isClear, blinkAlpha, leftColor, onTrigger, onAssign, onRemove, Modifier.weight(1f).height(34.dp))
        }
        rightSlots.forEach { def ->
            val sec = findSlotSection(sections, def)
            StyleMatrixBtn(def.label, sec, state, isLoad, isClear, blinkAlpha, rightColor, onTrigger, onAssign, onRemove, Modifier.weight(1f).height(34.dp))
        }
    }
}

@Composable
private fun UserBreakSubRow(
    sections: List<ArrangerSection>, state: ArrangerState, isLoad: Boolean, isClear: Boolean,
    blinkAlpha: Float, onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit, onRemove: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        (1..7).forEach { num ->
            val def = CreatorSlotDef("USER $num", "custom_$num", ArrangerSectionType.CUSTOM, 2)
            val sec = findSlotSection(sections, def)
            StyleMatrixBtn(def.label, sec, state, isLoad, isClear, blinkAlpha, Color(0xFF34D399), onTrigger, onAssign, onRemove, Modifier.weight(1f).height(34.dp))
        }
        val breakDef = CreatorSlotDef("BREAK", "break_1", ArrangerSectionType.BREAK, 1)
        val breakSec = findSlotSection(sections, breakDef)
        StyleMatrixBtn("BREAK", breakSec, state, isLoad, isClear, blinkAlpha, Color(0xFFFCD34D), onTrigger, onAssign, onRemove, Modifier.weight(1f).height(34.dp))
    }
}

@Composable
fun StyleMatrixBtn(
    label: String, section: ArrangerSection, state: ArrangerState,
    isLoadMode: Boolean, isClearMode: Boolean, blinkAlpha: Float, themeColor: Color,
    onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit, onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBreak = section.type == ArrangerSectionType.BREAK || section.id == "break_1"
    val isAssigned = section.samplePath.isNotBlank() || isBreak
    val isActive = (section.samplePath.isNotBlank() && state.activeSlot == section.id) || (isBreak && (state.activeSlot == "break_1" || state.activeSlot == section.id))
    val isQueued = (section.samplePath.isNotBlank() && state.queuedSlot == section.id) || (isBreak && (state.queuedSlot == "break_1" || state.queuedSlot == section.id))

    val bgBrush = when {
        isClearMode && isAssigned -> Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.5f), Color(0xFF261215)), radius = 55f)
        isLoadMode -> Brush.verticalGradient(listOf(Color(0xFF2E2416), Color(0xFF1F1810)))
        isActive -> Brush.radialGradient(listOf(themeColor.copy(alpha = 0.65f), themeColor.copy(alpha = 0.22f), Color(0xFF131724)), radius = 55f)
        isQueued -> Brush.radialGradient(listOf(themeColor.copy(alpha = 0.65f * blinkAlpha), themeColor.copy(alpha = 0.20f * blinkAlpha), Color(0xFF131724)), radius = 55f)
        isAssigned -> Brush.verticalGradient(listOf(Color(0xFF222838), Color(0xFF151924)))
        else -> Brush.verticalGradient(listOf(Color(0xFF141720), Color(0xFF0F1118)))
    }

    val borderColor = when {
        isClearMode && isAssigned -> Color(0xFFEF4444).copy(alpha = 0.7f)
        isLoadMode -> Color(0xFFFFB03A).copy(alpha = 0.7f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.5.dp))
            .background(bgBrush)
            .border(if (isLoadMode || (isClearMode && isAssigned)) 0.8.dp else 0.dp, borderColor, RoundedCornerShape(3.5.dp))
            .clickable {
                if (isClearMode) {
                    if (isAssigned) onRemove(section.id)
                } else if (isLoadMode) {
                    onAssign(section)
                } else if (isAssigned) {
                    onTrigger(section.id)
                }
            }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (isAssigned) {
                val ledAlpha = if (isQueued) blinkAlpha else if (isActive) 1f else 0.5f
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(themeColor.copy(alpha = ledAlpha)))
            }
            val textColor = when {
                isClearMode && isAssigned -> Color(0xFFEF4444)
                isLoadMode -> Color(0xFFFFB03A)
                isAssigned -> Color.White
                else -> Color(0xFF4B5563)
            }
            Text(label, color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

fun findSlotSection(sections: List<ArrangerSection>, def: CreatorSlotDef): ArrangerSection {
    return sections.firstOrNull { it.id == def.slotId || it.name.equals(def.label, ignoreCase = true) }
        ?: ArrangerSection(def.slotId, def.label, def.type, def.bars)
}
