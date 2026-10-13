package com.zenitharranger.ui.components.stylecreator

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.core.audio.NativeAudioBridge
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.model.BeatPresetManager
import com.zenitharranger.model.StyleCreatorState
import com.zenitharranger.ui.ArrangerViewModel
import kotlinx.coroutines.delay

@Composable
fun LandscapeStyleCreatorView(
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

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StyleCreatorHeader(
            styleTitle = creator.title,
            onTitleChange = { viewModel.setCreatorTitle(it) },
            folder = creator.folder,
            onFolderChange = { viewModel.setCreatorFolder(it) },
            availableFolders = availableFolders,
            timeSigNum = creator.timeSigNum,
            timeSigDen = creator.timeSigDen,
            onSetTimeSig = { num, den -> viewModel.setCreatorTimeSig(num, den) },
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

        StyleCreatorSectionsMatrix(
            state = state,
            sections = creator.sections,
            isLoadMode = creator.isLoadMode,
            isClearMode = creator.isClearMode,
            onTriggerSection = { secId ->
                val sec = creator.sections.firstOrNull { it.id == secId }
                if (sec != null && sec.samplePath.isNotBlank()) {
                    NativeAudioBridge.addOrUpdateSection(sec.id, sec.name, sec.type.rawValue, sec.bars, sec.samplePath, sec.sampleBpm)
                    NativeAudioBridge.triggerSection(sec.id)
                }
            },
            onAssignSample = { sec -> onAssignSample(sec) },
            onRemoveSample = { secId -> viewModel.removeCreatorSample(secId) },
            modifier = Modifier.weight(1f)
        )
    }
}
