package com.zenitharranger.ui.components.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.model.MidiState
import com.zenitharranger.ui.ArrangerViewModel

@Composable
fun SettingsView(
    midiState: MidiState,
    viewModel: ArrangerViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090D15))
            .border(1.dp, Color(0xFF192233), RoundedCornerShape(8.dp))
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsHeader(
            onRescan = { viewModel.rescanMidi() }
        )

        MidiTimingCard(
            midiState = midiState,
            onSetClockSync = { viewModel.setMidiClockSync(it) },
            onSetTransportSync = { viewModel.setMidiTransportSync(it) }
        )

        MidiPianoRollInputCard(midiState = midiState)
    }
}

@Composable
private fun SettingsHeader(onRescan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SETTINGS • HARDWARE & TIMING SYNC",
            color = Color(0xFFFFB03A),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E2838))
                .border(0.8.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                .clickable { onRescan() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("RESCAN USB MIDI", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MidiTimingCard(
    midiState: MidiState,
    onSetClockSync: (Boolean) -> Unit,
    onSetTransportSync: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111724))
            .border(1.dp, Color(0xFF1F2B40), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("MIDI TIMING SIGNAL (PREVENT TEMPO DRIFT)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

        // Device Connection Banner
        DeviceConnectionBanner(midiState)

        // Clock Source Toggle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ClockModeButton(
                label = "INTERNAL MASTER",
                sublabel = "App controls tempo",
                isSelected = !midiState.isClockSyncEnabled,
                onClick = { onSetClockSync(false) },
                modifier = Modifier.weight(1f)
            )
            ClockModeButton(
                label = "EXTERNAL KEYBOARD",
                sublabel = "Slave to keyboard (24 PPQN)",
                isSelected = midiState.isClockSyncEnabled,
                onClick = { onSetClockSync(true) },
                modifier = Modifier.weight(1f)
            )
        }

        // Live Telemetry Readout
        LiveClockStatusRow(midiState)

        // Transport sync checkbox
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSetTransportSync(!midiState.isTransportSyncEnabled) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Checkbox(
                checked = midiState.isTransportSyncEnabled,
                onCheckedChange = { onSetTransportSync(it) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
            )
            Text(
                text = "Sync START / STOP with connected keyboard",
                color = Color(0xFFE2E8F0),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeviceConnectionBanner(midiState: MidiState) {
    val isConn = midiState.isDeviceConnected
    val devName = midiState.connectedDeviceName ?: "No USB MIDI Keyboard Detected"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isConn) Color(0xFF0F261E) else Color(0xFF221A15))
            .border(0.8.dp, if (isConn) Color(0xFF10B981) else Color(0xFFF97316), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(RoundedCornerShape(4.dp))
                .background(if (isConn) Color(0xFF10B981) else Color(0xFFF97316))
        )
        Text(
            text = if (isConn) "CONNECTED: $devName" else devName,
            color = if (isConn) Color(0xFF34D399) else Color(0xFFFB923C),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ClockModeButton(
    label: String, sublabel: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF1E3250) else Color(0xFF0C101A))
            .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E2838), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Column {
            Text(label, color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(sublabel, color = Color(0xFF64748B), fontSize = 7.5.sp)
        }
    }
}

@Composable
private fun LiveClockStatusRow(midiState: MidiState) {
    val isLocked = midiState.isClockLocked
    val statusText = when {
        !midiState.isClockSyncEnabled -> "○ INTERNAL CLOCK MASTER (External sync inactive)"
        isLocked -> "● SYNC LOCKED: ${midiState.detectedBpm} BPM (Zero Drift Active)"
        else -> "○ WAITING FOR KEYBOARD CLOCK (Press START on keyboard)"
    }
    val textColor = when {
        isLocked -> Color(0xFF10B981)
        midiState.isClockSyncEnabled -> Color(0xFFFFB03A)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B0F18))
            .border(0.6.dp, Color(0xFF1E283A), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text = statusText, color = textColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MidiPianoRollInputCard(midiState: MidiState) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF111724))
            .border(1.dp, Color(0xFF1F2B40), RoundedCornerShape(6.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("PIANO ROLL INPUT (USB HARDWARE KEYS)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        val notes = midiState.activeNotes.sorted()
        val notesText = if (notes.isEmpty()) "NO KEYS PRESSED" else notes.joinToString(", ") { noteNumToName(it) }

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF0C101A))
                .border(0.6.dp, Color(0xFF1E2838), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column {
                Text("ACTIVE NOTES (LIGHTS UP BOTTOM PIANO ROLL):", color = Color(0xFF64748B), fontSize = 7.5.sp)
                Text(
                    text = notesText,
                    color = if (notes.isNotEmpty()) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun noteNumToName(midiNote: Int): String {
    val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val oct = (midiNote / 12) - 1
    return "${names[midiNote % 12]}$oct ($midiNote)"
}
