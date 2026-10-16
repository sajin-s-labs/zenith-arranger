package com.zenitharranger.ui.components.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.ui.ArrangerViewModel

/**
 * Main Workstation Area arranged into 3 Horizontal Rows:
 * - Top Row: Transport & Conductor Controls (Start, Sync, Tempo, Transpose)
 * - Middle Row: Style Sections Matrix (4 Intro | 4 Main, 4 Ending | 4 Fill, 7 Custom | 1 Break)
 * - Bottom Row: Registration Memory & Quick Controls (Set, 6 Reg Banks, Transpose, Tempo)
 */
@Composable
fun MainWorkstationRows(
    state: ArrangerState,
    sections: List<ArrangerSection>,
    viewModel: ArrangerViewModel,
    onAssignSample: (ArrangerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(3.5.dp)
    ) {
        // Top Row: Conductor & Transport
        ArrangerTransportRow(
            state = state,
            bpm = uiState.mainBpm.toInt(),
            onPlayToggle = { viewModel.togglePlay() },
            onBpmDelta = { viewModel.setBpmDelta(it) },
            onTapTempo = { viewModel.tapTempo() },
            transpose = uiState.transpose,
            onTransposeDelta = { viewModel.setTransposeDelta(it) },
            syncStart = uiState.isSyncStartArmed,
            onSyncStartToggle = { viewModel.toggleSyncStart() },
            syncStop = uiState.isSyncStopActive,
            onSyncStopToggle = { viewModel.toggleSyncStop() }
        )

        // Middle Row: Arranger Style Sections Matrix
        ArrangerSectionsMatrixRow(
            state = state,
            sections = sections,
            onTriggerSection = { viewModel.triggerSection(it) },
            onAssignSample = onAssignSample,
            modifier = Modifier.weight(1f)
        )

        // Bottom Row: Registration Memory
        ArrangerRegMemoryRow(
            transpose = uiState.transpose,
            bpm = uiState.mainBpm.toInt()
        )
    }
}
