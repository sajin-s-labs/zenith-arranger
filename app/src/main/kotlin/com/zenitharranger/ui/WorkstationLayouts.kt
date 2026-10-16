package com.zenitharranger.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.ui.components.*

import com.zenitharranger.ui.components.keyboard.WorkstationPianoRollBar
import com.zenitharranger.ui.components.main.MainWorkstationRows
import com.zenitharranger.ui.components.main.PortraitMainWorkstationRows
import com.zenitharranger.ui.components.settings.SettingsView
import com.zenitharranger.ui.components.stylecreator.LandscapeStyleCreatorView
import com.zenitharranger.ui.components.stylecreator.PortraitStyleCreatorView

@Composable
fun LandscapeWorkstationLayout(
    uiState: ArrangerUiState,
    styleGenre: String,
    accentColor: Color,
    activeSectionName: String,
    queuedSectionName: String?,
    selectedPanel: String,
    onSelectPanel: (String) -> Unit,
    viewModel: ArrangerViewModel,
    onSaveBeat: () -> Unit,
    onAddSection: () -> Unit,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val pianoHeight = when {
        screenHeightDp < 380 -> 76.dp
        screenHeightDp < 500 -> 88.dp
        else -> 105.dp
    }

    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        LandscapeUpperRow(
            uiState, styleGenre, accentColor, activeSectionName, queuedSectionName,
            selectedPanel, onSelectPanel, viewModel, onSaveBeat, onAddSection, onAssignSample,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        WorkstationPianoRollBar(
            externalActiveNotes = uiState.midi.activeNotes,
            externalPitchBend = uiState.midi.pitchBend,
            externalModulation = uiState.midi.modulation,
            transpose = uiState.transpose,
            isTouchKeysEnabled = uiState.isTouchKeysEnabled,
            isMidiPianoEnabled = uiState.midi.isMidiControlsPiano,
            onToggleTouchKeys = { viewModel.toggleTouchKeysEnabled() },
            onToggleMidiPiano = { viewModel.toggleMidiControlsPiano() },
            onNoteDown = { viewModel.onNoteDown(it) },
            onAllNotesUp = { viewModel.onAllNotesUp() },
            modifier = Modifier.fillMaxWidth().height(pianoHeight)
        )
    }
}

@Composable
private fun LandscapeUpperRow(
    uiState: ArrangerUiState,
    styleGenre: String,
    accentColor: Color,
    activeSectionName: String,
    queuedSectionName: String?,
    selectedPanel: String,
    onSelectPanel: (String) -> Unit,
    viewModel: ArrangerViewModel,
    onSaveBeat: () -> Unit,
    onAddSection: () -> Unit,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val lcdWeight = if (configuration.screenWidthDp < 700) 0.38f else 0.42f
    val controlWeight = 1f - lcdWeight

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        LcdDisplayBar(
            styleName = uiState.currentPresetName,
            styleGenre = styleGenre,
            accentColor = accentColor,
            state = uiState.state.copy(bpm = uiState.mainBpm),
            activeSectionName = activeSectionName,
            queuedSectionName = queuedSectionName,
            presets = uiState.savedPresets,
            onSelectPreset = { viewModel.loadPreset(it) },
            onSelectTimeSig = { n, d -> viewModel.setTimeSignature(n, d) },
            onSaveBeat = onSaveBeat,
            modifier = Modifier.weight(lcdWeight).fillMaxHeight()
        )

        Column(modifier = Modifier.weight(controlWeight).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            HorizontalPanelButtonsBar(selectedPanel = selectedPanel, onSelectPanel = onSelectPanel)
            when (selectedPanel) {
                "MAIN" -> MainWorkstationRows(uiState.state, uiState.sections, viewModel, onAssignSample, Modifier.weight(1f))
                "STYLE CREATOR", "STYLE" -> LandscapeStyleCreatorView(uiState.state, viewModel, onAssignSample, Modifier.weight(1f))
                "SETTINGS" -> SettingsView(uiState.midi, viewModel, Modifier.weight(1f))
                else -> PanelPlaceholder(panel = selectedPanel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PortraitWorkstationLayout(
    uiState: ArrangerUiState,
    styleGenre: String,
    accentColor: Color,
    activeSectionName: String,
    queuedSectionName: String?,
    selectedPanel: String,
    onSelectPanel: (String) -> Unit,
    viewModel: ArrangerViewModel,
    onSaveBeat: () -> Unit,
    onAddSection: () -> Unit,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PortraitTopRow(
            uiState, styleGenre, accentColor, activeSectionName, queuedSectionName,
            selectedPanel, onSelectPanel, viewModel, onSaveBeat
        )

        when (selectedPanel) {
            "MAIN" -> PortraitMainWorkstationRows(uiState.state, uiState.sections, viewModel, onAssignSample, Modifier.weight(1f))
            "STYLE CREATOR", "STYLE" -> PortraitStyleCreatorView(uiState.state, viewModel, onAssignSample, Modifier.weight(1f))
            "SETTINGS" -> SettingsView(uiState.midi, viewModel, Modifier.weight(1f))
            else -> PanelPlaceholder(panel = selectedPanel, modifier = Modifier.weight(1f))
        }

        WorkstationPianoRollBar(
            externalActiveNotes = uiState.midi.activeNotes,
            externalPitchBend = uiState.midi.pitchBend,
            externalModulation = uiState.midi.modulation,
            transpose = uiState.transpose,
            isTouchKeysEnabled = uiState.isTouchKeysEnabled,
            isMidiPianoEnabled = uiState.midi.isMidiControlsPiano,
            onToggleTouchKeys = { viewModel.toggleTouchKeysEnabled() },
            onToggleMidiPiano = { viewModel.toggleMidiControlsPiano() },
            onNoteDown = { viewModel.onNoteDown(it) },
            onAllNotesUp = { viewModel.onAllNotesUp() },
            modifier = Modifier.fillMaxWidth().height(105.dp)
        )
    }
}

@Composable
private fun PortraitTopRow(
    uiState: ArrangerUiState,
    styleGenre: String,
    accentColor: Color,
    activeSectionName: String,
    queuedSectionName: String?,
    selectedPanel: String,
    onSelectPanel: (String) -> Unit,
    viewModel: ArrangerViewModel,
    onSaveBeat: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(165.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        LcdDisplayBar(
            styleName = uiState.currentPresetName,
            styleGenre = styleGenre,
            accentColor = accentColor,
            state = uiState.state.copy(bpm = uiState.mainBpm),
            activeSectionName = activeSectionName,
            queuedSectionName = queuedSectionName,
            presets = uiState.savedPresets,
            onSelectPreset = { viewModel.loadPreset(it) },
            onSelectTimeSig = { n, d -> viewModel.setTimeSignature(n, d) },
            onSaveBeat = onSaveBeat,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        VerticalPanelButtonsStrip(
            selectedPanel = selectedPanel,
            onSelectPanel = onSelectPanel,
            modifier = Modifier.width(52.dp).fillMaxHeight()
        )
    }
}


@Composable
fun PanelPlaceholder(panel: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101724))
            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2838)), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(panel, color = Color(0xFFFFB03A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Workstation view placeholder", color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}
