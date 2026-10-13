package com.zenitharranger.core.midi

import android.content.Context
import com.zenitharranger.core.audio.NativeAudioBridge
import com.zenitharranger.model.MidiState
import com.zenitharranger.ui.components.keyboard.PianoSynthEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Orchestrates MIDI clock synchronization, USB device connections, and piano roll note streams.
 */
class MidiCoordinator(
    context: Context,
    private val scope: CoroutineScope
) {
    private val _midiState = MutableStateFlow(MidiState())
    val midiState: StateFlow<MidiState> = _midiState.asStateFlow()

    val clockParser = MidiClockParser(
        onTempoChanged = { bpm ->
            if (_midiState.value.isClockSyncEnabled) {
                NativeAudioBridge.setBpm(bpm)
            }
        },
        onMidiStart = {
            if (_midiState.value.isClockSyncEnabled && _midiState.value.isTransportSyncEnabled) {
                NativeAudioBridge.play()
            }
        },
        onMidiStop = {
            if (_midiState.value.isClockSyncEnabled && _midiState.value.isTransportSyncEnabled) {
                NativeAudioBridge.pause()
            }
        }
    )

    val inputDispatcher = MidiInputDispatcher(
        onNoteTriggered = { note, _ ->
            PianoSynthEngine.playNote(note)
        }
    )

    private val deviceManager = MidiDeviceManager(context, clockParser, inputDispatcher)

    init {
        deviceManager.start()
        startObservationLoop()
    }

    private fun startObservationLoop() {
        scope.launch {
            while (isActive) {
                clockParser.checkTimeout()
                _midiState.update {
                    it.copy(
                        isDeviceConnected = deviceManager.isDeviceConnected.value,
                        connectedDeviceName = deviceManager.connectedDeviceName.value,
                        isClockLocked = clockParser.isClockLocked.value,
                        detectedBpm = clockParser.detectedBpm.value,
                        clockPulseTick = clockParser.pulseTick.value,
                        activeNotes = inputDispatcher.activeNotes.value,
                        pitchBend = inputDispatcher.pitchBend.value,
                        modulation = inputDispatcher.modulation.value
                    )
                }
                delay(80)
            }
        }
    }

    fun setClockSync(enabled: Boolean) {
        _midiState.update { it.copy(isClockSyncEnabled = enabled) }
        if (enabled && clockParser.isClockLocked.value) {
            NativeAudioBridge.setBpm(clockParser.detectedBpm.value)
        }
    }

    fun setTransportSync(enabled: Boolean) {
        _midiState.update { it.copy(isTransportSyncEnabled = enabled) }
    }

    fun rescan() {
        deviceManager.scanAndConnect()
    }

    fun close() {
        deviceManager.stop()
    }
}
