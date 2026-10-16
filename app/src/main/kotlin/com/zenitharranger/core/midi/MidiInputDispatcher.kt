package com.zenitharranger.core.midi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Dispatches MIDI channel voice messages: Note On/Off, Pitch Bend, and Modulation CC.
 * Exposes live note sets to feed on-screen piano rolls and chord engines.
 */
class MidiInputDispatcher(
    private val onNoteTriggered: (note: Int, velocity: Int) -> Unit = { _, _ -> },
    private val onNoteReleased: (note: Int) -> Unit = {},
    private val onPitchBendChanged: (Float) -> Unit = {},
    private val onModulationChanged: (Float) -> Unit = {}
) {
    private val _activeNotes = MutableStateFlow<Set<Int>>(emptySet())
    val activeNotes: StateFlow<Set<Int>> = _activeNotes.asStateFlow()

    private val _pitchBend = MutableStateFlow(0f)
    val pitchBend: StateFlow<Float> = _pitchBend.asStateFlow()

    private val _modulation = MutableStateFlow(0f)
    val modulation: StateFlow<Float> = _modulation.asStateFlow()

    private var runningStatus: Int = 0
    private var dataByte1: Int = -1

    fun processMidiBytes(bytes: ByteArray, offset: Int, count: Int) {
        for (i in offset until (offset + count)) {
            val byte = bytes[i].toInt() and 0xFF
            if (byte >= 0xF8) {
                // Real-time messages are handled separately and do not alter running status
                continue
            }
            if (byte >= 0x80) {
                runningStatus = byte
                dataByte1 = -1
            } else {
                handleDataByte(byte)
            }
        }
    }

    private fun handleDataByte(data: Int) {
        if (dataByte1 == -1) {
            dataByte1 = data
            val cmd = runningStatus and 0xF0
            if (cmd == 0xC0 || cmd == 0xD0) {
                // 1-data-byte commands (Program Change, Channel Pressure)
                dataByte1 = -1
            }
        } else {
            val d1 = dataByte1
            val d2 = data
            dataByte1 = -1
            dispatchMessage(runningStatus, d1, d2)
        }
    }

    private fun dispatchMessage(status: Int, d1: Int, d2: Int) {
        val command = status and 0xF0
        when (command) {
            0x90 -> { // Note On
                if (d2 > 0) {
                    _activeNotes.update { it + d1 }
                    onNoteTriggered(d1, d2)
                } else {
                    _activeNotes.update { it - d1 }
                    onNoteReleased(d1)
                }
            }
            0x80 -> { // Note Off
                _activeNotes.update { it - d1 }
                onNoteReleased(d1)
            }
            0xB0 -> { // Control Change
                if (d1 == 1) { // CC 1 = Modulation Wheel
                    val mod = (d2.toFloat() / 127f).coerceIn(0f, 1f)
                    _modulation.value = mod
                    onModulationChanged(mod)
                }
            }
            0xE0 -> { // Pitch Bend (14-bit)
                val rawBend = (d2 shl 7) or d1
                val bend = ((rawBend - 8192) / 8192f).coerceIn(-1f, 1f)
                _pitchBend.value = bend
                onPitchBendChanged(bend)
            }
        }
    }

    fun clearNotes() {
        _activeNotes.value = emptySet()
        _pitchBend.value = 0f
        _modulation.value = 0f
    }
}
