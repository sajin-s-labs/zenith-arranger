package com.zenitharranger.ui.components.stylecreator

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.core.audio.NativeAudioBridge
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.model.ArrangerSectionType
import com.zenitharranger.model.BeatPresetManager
import com.zenitharranger.model.StyleCreatorState
import com.zenitharranger.ui.ArrangerViewModel
import kotlinx.coroutines.delay

@Composable
fun PortraitStyleCreatorView(
    state: ArrangerState,
    viewModel: ArrangerViewModel,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val creator = uiState.creator
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val availableFolders = remember { BeatPresetManager.listSubfolders(context, "User") }

    LaunchedEffect(savedMessage) {
        if (savedMessage != null) {
            delay(2500)
            savedMessage = null
        }
    }

    val beatDurationMs = (60_000f / state.bpm.coerceAtLeast(30f)).toInt()
    val transition = rememberInfiniteTransition(label = "pStyleBlink")
    val blinkAlpha by transition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(beatDurationMs / 2, easing = LinearEasing), RepeatMode.Reverse), "blink")

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        StyleCreatorHeader(
            styleTitle = creator.title,
            onTitleChange = { viewModel.setCreatorTitle(it) },
            folder = creator.folder,
            onFolderChange = { viewModel.setCreatorFolder(it) },
            availableFolders = availableFolders,
            timeSigNum = creator.timeSigNum,
            timeSigDen = creator.timeSigDen,
            onSetTimeSig = { n, d -> viewModel.setCreatorTimeSig(n, d) },
            onSaveStyle = {
                val ok = viewModel.saveCreatorStyle(context)
                savedMessage = if (ok) "SAVED" else "ERROR"
            },
            savedMessage = savedMessage
        )

        StyleCreatorTransport(
            isPlaying = creator.isPlaying,
            isLoadMode = creator.isLoadMode,
            isClearMode = creator.isClearMode,
            bpm = creator.bpm.toInt(),
            onPlayToggle = { viewModel.togglePlay() },
            onToggleLoadMode = { viewModel.toggleCreatorLoadMode() },
            onToggleClearMode = { viewModel.toggleCreatorClearMode() },
            onBpmDelta = { viewModel.setCreatorBpmDelta(it) },
            onTapTempo = { viewModel.tapTempo() }
        )

        PortraitStyleSectionsContainer(
            sections = creator.sections,
            state = state,
            isLoad = creator.isLoadMode,
            isClear = creator.isClearMode,
            blinkAlpha = blinkAlpha,
            onTrigger = { secId ->
                val sec = creator.sections.firstOrNull { it.id == secId }
                if (sec != null && sec.samplePath.isNotBlank()) {
                    NativeAudioBridge.addOrUpdateSection(sec.id, sec.name, sec.type.rawValue, sec.bars, sec.samplePath, sec.sampleBpm)
                    NativeAudioBridge.triggerSection(sec.id)
                }
            },
            onAssign = onAssignSample,
            onRemove = { viewModel.removeCreatorSample(it) }
        )
    }
}

@Composable
private fun PortraitStyleSectionsContainer(
    sections: List<ArrangerSection>, state: ArrangerState, isLoad: Boolean, isClear: Boolean,
    blinkAlpha: Float, onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit, onRemove: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF0C1018)).border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp)).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val intros = (1..4).map { CreatorSlotDef("INTRO $it", "sec_intro_$it", ArrangerSectionType.INTRO, 2) }
        val ends = (1..4).map { CreatorSlotDef("END $it", "sec_end_$it", ArrangerSectionType.ENDING, 2) }
        val mains = listOf("A", "B", "C", "D").map { CreatorSlotDef("MAIN $it", "sec_main_${it.lowercase()}", ArrangerSectionType.MAIN, 2) }
        val fills = listOf("A", "B", "C", "D").map { CreatorSlotDef("FILL $it", "sec_fill_${it.lowercase()}", ArrangerSectionType.FILL, 1) }

        PortraitRow4(intros, sections, state, isLoad, isClear, blinkAlpha, Color(0xFFA78BFA), onTrigger, onAssign, onRemove)
        PortraitRow4(ends, sections, state, isLoad, isClear, blinkAlpha, Color(0xFFFB923C), onTrigger, onAssign, onRemove)
        PortraitRow4(mains, sections, state, isLoad, isClear, blinkAlpha, Color(0xFF60A5FA), onTrigger, onAssign, onRemove)
        PortraitRow4(fills, sections, state, isLoad, isClear, blinkAlpha, Color(0xFFF472B6), onTrigger, onAssign, onRemove)

        val u14 = (1..4).map { CreatorSlotDef("USER $it", "sec_user_$it", ArrangerSectionType.CUSTOM, 2) }
        val u57Break = (5..7).map { CreatorSlotDef("USER $it", "sec_user_$it", ArrangerSectionType.CUSTOM, 2) } +
            listOf(CreatorSlotDef("BREAK", "sec_break", ArrangerSectionType.BREAK, 1))

        PortraitRow4(u14, sections, state, isLoad, isClear, blinkAlpha, Color(0xFF34D399), onTrigger, onAssign, onRemove)
        PortraitRow4(u57Break, sections, state, isLoad, isClear, blinkAlpha, Color(0xFF34D399), onTrigger, onAssign, onRemove)
    }
}

@Composable
private fun PortraitRow4(
    slots: List<CreatorSlotDef>, sections: List<ArrangerSection>, state: ArrangerState,
    isLoad: Boolean, isClear: Boolean, blinkAlpha: Float, themeColor: Color,
    onTrigger: (String) -> Unit, onAssign: (ArrangerSection) -> Unit, onRemove: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        slots.forEach { def ->
            val sec = findSlotSection(sections, def)
            val color = if (def.type == ArrangerSectionType.BREAK) Color(0xFFFCD34D) else themeColor
            StyleMatrixBtn(def.label, sec, state, isLoad, isClear, blinkAlpha, color, onTrigger, onAssign, onRemove, Modifier.weight(1f).fillMaxHeight())
        }
    }
}
