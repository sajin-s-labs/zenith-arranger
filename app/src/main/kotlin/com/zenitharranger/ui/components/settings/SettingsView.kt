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
import com.zenitharranger.model.MidiClockSource
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

        // Device Connection Banner
        DeviceConnectionBanner(midiState)

        // Independent Hardware Routing Card (Style vs Piano vs Clock Out)
        MidiHardwareRoutingCard(
            midiState = midiState,
            onToggleStyle = { viewModel.setMidiControlsStyle(it) },
            onTogglePiano = { viewModel.setMidiControlsPiano(it) },
            onToggleClockOut = { viewModel.setMidiClockOutEnabled(it) }
        )

        // 3 Clock Timing Options Card
        MidiTimingCard(
            midiState = midiState,
            onSelectClockSource = { viewModel.setMidiClockSource(it) },
            onSetTransportSync = { viewModel.setMidiTransportSync(it) },
            onSetSyncLatency = { viewModel.setSyncLatencyMs(it) }
        )

        // 10-Second Timing Calibration Profile Card
        MidiCalibrationCard(
            midiState = midiState,
            onStartScan = { viewModel.startMidiCalibrationScan() },
            onCancelScan = { viewModel.cancelMidiCalibrationScan() },
            onApplySaved = { viewModel.setMidiClockSource(MidiClockSource.SAVED_CALIBRATION) }
        )

        // Live Telemetry / Piano Roll Monitor
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
private fun DeviceConnectionBanner(midiState: MidiState) {
    val isConn = midiState.isDeviceConnected
    val devName = midiState.connectedDeviceName ?: "No USB MIDI Keyboard Detected"
    val outInfo = if (midiState.hasMidiOutSupport) " • MIDI OUT: READY" else ""
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
            text = if (isConn) "CONNECTED: $devName$outInfo" else devName,
            color = if (isConn) Color(0xFF34D399) else Color(0xFFFB923C),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MidiHardwareRoutingCard(
    midiState: MidiState,
    onToggleStyle: (Boolean) -> Unit,
    onTogglePiano: (Boolean) -> Unit,
    onToggleClockOut: (Boolean) -> Unit
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
        Text("HARDWARE MIDI ROUTING (INDEPENDENT SWITCHES)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoutingToggleButton(
                title = "MIDI CONTROLS STYLE",
                subtitle = if (midiState.isMidiControlsStyle) "Style slaves to MIDI Start/Stop & Clock" else "Style independent of external MIDI",
                isActive = midiState.isMidiControlsStyle,
                onToggle = { onToggleStyle(!midiState.isMidiControlsStyle) },
                modifier = Modifier.weight(1f)
            )

            RoutingToggleButton(
                title = "MIDI CONTROLS PIANO",
                subtitle = if (midiState.isMidiControlsPiano) "Hardware keys play Native Piano Synth" else "Hardware keys muted on internal synth",
                isActive = midiState.isMidiControlsPiano,
                onToggle = { onTogglePiano(!midiState.isMidiControlsPiano) },
                modifier = Modifier.weight(1f)
            )
        }

        val isOutActive = midiState.isMidiClockOutEnabled || midiState.clockSource == MidiClockSource.TRANSMIT_MASTER_OUT
        RoutingToggleButton(
            title = "TRANSMIT MIDI CLOCK OUT (PHONE IS MASTER)",
            subtitle = if (isOutActive) "Phone actively transmits 24 PPQN tempo & Start/Stop to slave the connected keyboard"
                       else "Disabled (Tap to slave external keyboard to phone tempo)",
            isActive = isOutActive,
            onToggle = { onToggleClockOut(!isOutActive) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoutingToggleButton(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF132A3B) else Color(0xFF0C101A))
            .border(1.dp, if (isActive) Color(0xFF38BDF8) else Color(0xFF1E2838), RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .padding(7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (isActive) Color(0xFF38BDF8) else Color(0xFF475569))
                )
                Text(
                    text = title,
                    color = if (isActive) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = subtitle, color = Color(0xFF64748B), fontSize = 7.5.sp)
        }
    }
}

@Composable
private fun MidiTimingCard(
    midiState: MidiState,
    onSelectClockSource: (MidiClockSource) -> Unit,
    onSetTransportSync: (Boolean) -> Unit,
    onSetSyncLatency: (Int) -> Unit
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
        Text("CLOCK & TEMPO REFERENCE (4 MODES)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

        // 4 Clock Options: Default App Values, External MIDI Values, Saved MIDI Values, Send Clock Out
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ClockModeButton(
                label = "1. DEFAULT APP",
                sublabel = "Internal master",
                isSelected = midiState.clockSource == MidiClockSource.DEFAULT_APP,
                onClick = { onSelectClockSource(MidiClockSource.DEFAULT_APP) },
                modifier = Modifier.weight(1f)
            )

            ClockModeButton(
                label = "2. EXTERNAL MIDI",
                sublabel = "Slave to keyboard",
                isSelected = midiState.clockSource == MidiClockSource.EXTERNAL_MIDI,
                onClick = { onSelectClockSource(MidiClockSource.EXTERNAL_MIDI) },
                modifier = Modifier.weight(1f)
            )

            ClockModeButton(
                label = "3. SAVED VALUES",
                sublabel = if (midiState.savedCalibration != null) "${midiState.savedCalibration.bpm.toInt()} BPM ref" else "No profile yet",
                isSelected = midiState.clockSource == MidiClockSource.SAVED_CALIBRATION,
                onClick = { onSelectClockSource(MidiClockSource.SAVED_CALIBRATION) },
                modifier = Modifier.weight(1f)
            )

            ClockModeButton(
                label = "4. SEND CLOCK OUT",
                sublabel = "Keyboard slaves",
                isSelected = midiState.clockSource == MidiClockSource.TRANSMIT_MASTER_OUT,
                onClick = { onSelectClockSource(MidiClockSource.TRANSMIT_MASTER_OUT) },
                modifier = Modifier.weight(1.05f)
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
                text = "Sync START / STOP transport with connected keyboard (When Style Routing is Active)",
                color = Color(0xFFE2E8F0),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Hardware Audio Sync Delay Compensation (Output Buffer Latency Offset)
        HorizontalDivider(color = Color(0xFF1E2838), thickness = 0.8.dp)
        LatencyCompensationSection(
            currentOffsetMs = midiState.syncLatencyMs,
            onSetOffsetMs = onSetSyncLatency
        )
    }
}

@Composable
private fun LatencyCompensationSection(
    currentOffsetMs: Int,
    onSetOffsetMs: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = "HARDWARE AUDIO SYNC DELAY COMPENSATION",
                    color = Color(0xFFFFB03A),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compensates for Android speaker latency (~25ms) so phone & keyboard downbeats hit in phase",
                    color = Color(0xFF64748B),
                    fontSize = 7.5.sp,
                    lineHeight = 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F261E))
                    .border(0.8.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${if (currentOffsetMs >= 0) "+" else ""}${currentOffsetMs} ms",
                    color = Color(0xFF34D399),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LatencyNudgeButton(label = "-10ms", onClick = { onSetOffsetMs(currentOffsetMs - 10) }, modifier = Modifier.weight(1f))
            LatencyNudgeButton(label = "-2ms", onClick = { onSetOffsetMs(currentOffsetMs - 2) }, modifier = Modifier.weight(1f))
            LatencyNudgeButton(label = "RESET 25ms", onClick = { onSetOffsetMs(25) }, modifier = Modifier.weight(1.3f), isHighlight = true)
            LatencyNudgeButton(label = "+2ms", onClick = { onSetOffsetMs(currentOffsetMs + 2) }, modifier = Modifier.weight(1f))
            LatencyNudgeButton(label = "+10ms", onClick = { onSetOffsetMs(currentOffsetMs + 10) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LatencyNudgeButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isHighlight) Color(0xFF1E2838) else Color(0xFF0F1522))
            .border(0.8.dp, if (isHighlight) Color(0xFF38BDF8) else Color(0xFF1E2838), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isHighlight) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MidiCalibrationCard(
    midiState: MidiState,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onApplySaved: () -> Unit
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("10-SECOND TIMING CALIBRATION PROFILE", color = Color(0xFFFFB03A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (midiState.isCalibrating) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF451A1A))
                        .border(0.8.dp, Color(0xFFEF4444), RoundedCornerShape(3.dp))
                        .clickable { onCancelScan() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("CANCEL SCAN", color = Color(0xFFEF4444), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Scans a 10-second stream of MIDI clock or rhythm pulses from your keyboard, measuring precise tempo and clock jitter. Saves reference profile so the arranger plays perfectly locked even without live external cables.",
            color = Color(0xFF94A3B8),
            fontSize = 7.5.sp,
            lineHeight = 11.sp
        )

        if (midiState.isCalibrating) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LinearProgressIndicator(
                    progress = { midiState.calibrationProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFB03A),
                    trackColor = Color(0xFF1E2838)
                )
                val percent = (midiState.calibrationProgress * 100).toInt()
                val secRemaining = (10.0f * (1f - midiState.calibrationProgress)).coerceAtLeast(0f)
                Text(
                    text = "CALIBRATING... $percent% (${String.format("%.1f", secRemaining)}s remaining) — Play or send clock now",
                    color = Color(0xFFFFB03A),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2838))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                        .clickable { onStartScan() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("START 10-SEC CALIBRATION SCAN", color = Color(0xFF38BDF8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }

                if (midiState.savedCalibration != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F261E))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .clickable { onApplySaved() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("USE SAVED (${midiState.savedCalibration.bpm.toInt()} BPM)", color = Color(0xFF34D399), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Saved Profile Info
            midiState.savedCalibration?.let { cal ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0B0F18))
                        .border(0.6.dp, Color(0xFF1E283A), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "SAVED PROFILE: ${cal.bpm} BPM | Jitter: ±${cal.jitterMs} ms | ${cal.sampleCount} pulses recorded",
                        color = Color(0xFF38BDF8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
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
            Text(label, color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Text(sublabel, color = Color(0xFF64748B), fontSize = 7.sp)
        }
    }
}

@Composable
private fun LiveClockStatusRow(midiState: MidiState) {
    val statusText = when (midiState.clockSource) {
        MidiClockSource.DEFAULT_APP -> {
            if (midiState.isMidiClockOutEnabled) "● DEFAULT APP (MIDI OUT ACTIVE): Internal frame clock active • Transmitting 24 PPQN to keyboard"
            else "● DEFAULT APP VALUES: Rock-solid internal C++ audio frame clock active"
        }
        MidiClockSource.TRANSMIT_MASTER_OUT -> "● MIDI CLOCK OUT (MASTER): Transmitting 24 PPQN tempo & Start/Stop to slave external keyboard"
        MidiClockSource.EXTERNAL_MIDI -> {
            if (midiState.isClockLocked) "● EXTERNAL SYNC LOCKED: ${midiState.detectedBpm} BPM (Zero Drift Active)"
            else "○ WAITING FOR KEYBOARD CLOCK (Press START on keyboard)"
        }
        MidiClockSource.SAVED_CALIBRATION -> {
            val bpm = midiState.savedCalibration?.bpm ?: 120f
            val jitter = midiState.savedCalibration?.jitterMs ?: 0.5f
            "● SAVED MIDI VALUES: Calibrated reference at $bpm BPM (±${jitter}ms jitter profile)"
        }
    }

    val textColor = when (midiState.clockSource) {
        MidiClockSource.DEFAULT_APP -> Color(0xFF38BDF8)
        MidiClockSource.TRANSMIT_MASTER_OUT -> Color(0xFF34D399)
        MidiClockSource.EXTERNAL_MIDI -> if (midiState.isClockLocked) Color(0xFF10B981) else Color(0xFFFFB03A)
        MidiClockSource.SAVED_CALIBRATION -> Color(0xFF34D399)
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B0F18))
            .border(0.6.dp, Color(0xFF1E283A), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text = statusText, color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("ACTIVE HARDWARE NOTES (REAL-TIME INPUT STREAM):", color = Color(0xFF64748B), fontSize = 7.5.sp)
                Text(
                    text = notesText,
                    color = if (notes.isNotEmpty()) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
                if (midiState.pitchBend != 0f || midiState.modulation > 0f) {
                    val pb = (midiState.pitchBend * 100).toInt()
                    val mod = (midiState.modulation * 100).toInt()
                    Text("PITCH BEND: $pb% | MOD WHEEL: $mod%", color = Color(0xFF38BDF8), fontSize = 7.5.sp)
                }
            }
        }
    }
}

private fun noteNumToName(midiNote: Int): String {
    val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val oct = (midiNote / 12) - 1
    return "${names[midiNote % 12]}$oct ($midiNote)"
}
