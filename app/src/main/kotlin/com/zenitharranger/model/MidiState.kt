package com.zenitharranger.model

/**
 * Arranger timing reference clock mode:
 * - DEFAULT_APP: Rock-solid internal C++ frame clock (external MIDI pulses ignored).
 * - EXTERNAL_MIDI: Live 24 PPQN external MIDI hardware clock slave.
 * - SAVED_CALIBRATION: Reference timing calibrated from a 10s keyboard profile scan.
 * - TRANSMIT_MASTER_OUT: Phone is Master Conductor; transmits 24 PPQN clock & Start/Stop out to keyboard.
 */
enum class MidiClockSource {
    DEFAULT_APP,
    EXTERNAL_MIDI,
    SAVED_CALIBRATION,
    TRANSMIT_MASTER_OUT
}

/**
 * Saved timing profile from 10-second MIDI scan or user calibration.
 */
data class SavedMidiCalibration(
    val bpm: Float = 120.0f,
    val jitterMs: Float = 0.5f,
    val sampleCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String = "10s Calibrated Profile"
)

/**
 * Immutable state representing connected MIDI keyboard, timing sync, and piano roll note activity.
 */
data class MidiState(
    val isDeviceConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val clockSource: MidiClockSource = MidiClockSource.DEFAULT_APP,
    val isMidiControlsStyle: Boolean = false, // When enabled, external MIDI clock & transport control Arranger Style
    val isMidiControlsPiano: Boolean = true,  // When enabled, external MIDI notes play the native Piano synth
    val isTransportSyncEnabled: Boolean = true,
    val isClockLocked: Boolean = false,
    val detectedBpm: Float = 0f,
    val activeNotes: Set<Int> = emptySet(),
    val pitchBend: Float = 0f,
    val modulation: Float = 0f,
    val clockPulseTick: Long = 0L,
    val isCalibrating: Boolean = false,
    val calibrationProgress: Float = 0f,
    val savedCalibration: SavedMidiCalibration? = null,
    val syncLatencyMs: Int = 25,
    val isMidiClockOutEnabled: Boolean = false,
    val hasMidiOutSupport: Boolean = false
) {
    // Backward compatibility helper
    val isClockSyncEnabled: Boolean
        get() = clockSource == MidiClockSource.EXTERNAL_MIDI
}
