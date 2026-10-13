package com.zenitharranger.model

/**
 * Immutable state representing connected MIDI keyboard, timing sync, and piano roll note activity.
 */
data class MidiState(
    val isDeviceConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val isClockSyncEnabled: Boolean = false,
    val isTransportSyncEnabled: Boolean = true,
    val isClockLocked: Boolean = false,
    val detectedBpm: Float = 0f,
    val activeNotes: Set<Int> = emptySet(),
    val pitchBend: Float = 0f,
    val modulation: Float = 0f,
    val clockPulseTick: Long = 0L
)
