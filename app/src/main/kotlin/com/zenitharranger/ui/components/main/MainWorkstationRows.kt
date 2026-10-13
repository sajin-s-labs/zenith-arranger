package com.zenitharranger.ui.components.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.ui.ArrangerViewModel

/**
 * Main Workstation Area arranged into 3 Fixed Horizontal Rows:
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
    var transpose by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row: Conductor & Transport
        ArrangerTransportRow(
            state = state,
            onPlayToggle = { viewModel.togglePlay() },
            onBpmDelta = { viewModel.setBpmDelta(it) },
            onTapTempo = { viewModel.tapTempo() },
            transpose = transpose,
            onTransposeDelta = { transpose = (transpose + it).coerceIn(-12, 12) }
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
            transpose = transpose,
            bpm = state.bpm.toInt()
        )
    }
}
