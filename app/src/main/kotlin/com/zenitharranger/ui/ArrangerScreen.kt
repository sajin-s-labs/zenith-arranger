package com.zenitharranger.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.zenitharranger.model.*
import com.zenitharranger.ui.dialogs.*

/**
 * Main Arranger Workstation Screen.
 * Adapts between Landscape (LCD left + Panel buttons top) and Portrait (LCD + Panel buttons right).
 */
@Composable
fun ArrangerScreen(
    viewModel: ArrangerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) { viewModel.refreshPresets(context) }

    var selectedPanel by remember { mutableStateOf("MAIN") }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var targetSectionForAssign by remember { mutableStateOf<ArrangerSection?>(null) }

    val wavPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val sec = targetSectionForAssign
        if (uri != null && sec != null) {
            val isCreator = selectedPanel == "STYLE" || selectedPanel == "STYLE CREATOR"
            val bars = if (sec.bars > 0) sec.bars else (if (sec.type == ArrangerSectionType.FILL || sec.type == ArrangerSectionType.BREAK) 1 else 2)
            if (isCreator) {
                viewModel.assignCreatorSample(context, uri, sec, bars)
            } else {
                viewModel.assignSample(context, uri, sec, uiState.mainBpm, bars)
            }
        }
        targetSectionForAssign = null
    }

    val onSelectPanel: (String) -> Unit = { panel ->
        if (panel != selectedPanel) {
            val prev = selectedPanel
            selectedPanel = panel
            val isEnteringCreator = (panel == "STYLE" || panel == "STYLE CREATOR") && (prev != "STYLE" && prev != "STYLE CREATOR")
            val isEnteringMain = panel == "MAIN" && (prev == "STYLE" || prev == "STYLE CREATOR")
            if (isEnteringCreator) {
                viewModel.onEnterStyleCreator()
            } else if (isEnteringMain) {
                viewModel.onEnterMainWorkstation()
            }
        }
    }

    val activeSection = uiState.sections.find { it.id == uiState.state.activeSlot }
    val queuedSection = uiState.sections.find { it.id == uiState.state.queuedSlot }
    val activeSectionName = activeSection?.name ?: if (uiState.state.activeSlot == "break_1") "Break" else uiState.state.activeSlot.replace("_", " ").uppercase()
    val queuedSectionName = queuedSection?.name ?: if (uiState.state.queuedSlot == "break_1") "Break" else if (uiState.state.queuedSlot.isNotBlank()) uiState.state.queuedSlot.replace("_", " ").uppercase() else null
    val currentPreset = uiState.savedPresets.find { it.name == uiState.currentPresetName }
    val styleGenre = currentPreset?.genre ?: "POP & ROCK"
    val accentColor = Color(currentPreset?.accentColorHex ?: 0xFFFF7043)

    Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF090A0E)) {
        if (isLandscape) {
            LandscapeWorkstationLayout(
                uiState = uiState,
                styleGenre = styleGenre,
                accentColor = accentColor,
                activeSectionName = activeSectionName,
                queuedSectionName = queuedSectionName,
                selectedPanel = selectedPanel,
                onSelectPanel = onSelectPanel,
                viewModel = viewModel,
                onSaveBeat = { showSaveDialog = true },
                onAddSection = { showAddSectionDialog = true },
                onAssignSample = { sec -> targetSectionForAssign = sec; wavPicker.launch("audio/*") }
            )
        } else {
            PortraitWorkstationLayout(
                uiState = uiState,
                styleGenre = styleGenre,
                accentColor = accentColor,
                activeSectionName = activeSectionName,
                queuedSectionName = queuedSectionName,
                selectedPanel = selectedPanel,
                onSelectPanel = onSelectPanel,
                viewModel = viewModel,
                onSaveBeat = { showSaveDialog = true },
                onAddSection = { showAddSectionDialog = true },
                onAssignSample = { sec -> targetSectionForAssign = sec; wavPicker.launch("audio/*") }
            )
        }
    }

    if (showAddSectionDialog) {
        AddSectionDialog(
            onDismiss = { showAddSectionDialog = false },
            onConfirm = { name, type, bars -> viewModel.addSection(name, type, bars); showAddSectionDialog = false }
        )
    }

    if (showSaveDialog) {
        SaveBeatDialog(
            defaultName = uiState.currentPresetName,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name -> viewModel.savePreset(context, name); showSaveDialog = false }
        )
    }
}
