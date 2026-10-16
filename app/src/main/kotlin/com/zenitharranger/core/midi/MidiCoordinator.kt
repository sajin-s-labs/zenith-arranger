package com.zenitharranger.core.midi

import android.content.Context
import com.zenitharranger.core.audio.NativeAudioBridge
import com.zenitharranger.model.MidiClockSource
import com.zenitharranger.model.MidiState
import com.zenitharranger.model.SavedMidiCalibration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Orchestrates MIDI clock synchronization, USB device connections, calibration profiling,
 * and independent hardware routing switches for Arranger Style and Native Piano Synth.
 */
class MidiCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onNoteTriggered: (Int) -> Unit = {},
    private val onAllNotesReleased: () -> Unit = {},
    private val onTransportStart: () -> Unit = {},
    private val onTransportStop: () -> Unit = {},
    private val onExternalTempo: (Float) -> Unit = {}
) {
    private val prefs = context.getSharedPreferences("zenith_midi_prefs", Context.MODE_PRIVATE)

    private val _midiState = MutableStateFlow(loadInitialState())
    val midiState: StateFlow<MidiState> = _midiState.asStateFlow()

    private var calibrationJob: Job? = null

    val clockParser: MidiClockParser = MidiClockParser(
        onTempoChanged = { bpm ->
            val state = _midiState.value
            // Only update engine BPM when explicitly configured to slave to external clock
            if (state.clockSource == MidiClockSource.EXTERNAL_MIDI && state.isMidiControlsStyle) {
                NativeAudioBridge.setBpm(bpm)
                onExternalTempo(bpm)
            }
        },
        onMidiStart = {
            val state = _midiState.value
            if (state.isTransportSyncEnabled) {
                onTransportStart()
            }
        },
        onMidiStop = {
            val state = _midiState.value
            if (state.isTransportSyncEnabled) {
                onTransportStop()
            }
        },
        onClockPulse = {
            val state = _midiState.value
            if (state.clockSource == MidiClockSource.EXTERNAL_MIDI && state.isMidiControlsStyle) {
                NativeAudioBridge.onMidiClockPulse()
            }
        }
    )

    val inputDispatcher: MidiInputDispatcher = MidiInputDispatcher(
        onNoteTriggered = { note, velocity ->
            onNoteTriggered(note)
            if (_midiState.value.isMidiControlsPiano) {
                NativeAudioBridge.noteOn(note, velocity / 127.0f)
            }
        },
        onNoteReleased = { note ->
            if (_midiState.value.isMidiControlsPiano) {
                NativeAudioBridge.noteOff(note)
            }
            if (inputDispatcher.activeNotes.value.isEmpty()) {
                onAllNotesReleased()
            }
        },
        onPitchBendChanged = { bend ->
            if (_midiState.value.isMidiControlsPiano) {
                NativeAudioBridge.setPitchBend(bend * 2.0f)
            }
        },
        onModulationChanged = { mod ->
            if (_midiState.value.isMidiControlsPiano) {
                NativeAudioBridge.setModulation(mod)
            }
        }
    )

    private val deviceManager = MidiDeviceManager(context, clockParser, inputDispatcher)

    val clockTransmitter: MidiClockTransmitter = MidiClockTransmitter(
        sendByte = { byte ->
            deviceManager.sendMidiByte(byte)
        }
    )

    init {
        val s = _midiState.value
        clockTransmitter.isEnabled = s.isMidiClockOutEnabled || (s.clockSource == MidiClockSource.TRANSMIT_MASTER_OUT)
        deviceManager.start()
        startObservationLoop()
    }

    private fun loadInitialState(): MidiState {
        val clockModeStr = prefs.getString("clock_source", MidiClockSource.DEFAULT_APP.name)
        val clockSource = try {
            MidiClockSource.valueOf(clockModeStr ?: MidiClockSource.DEFAULT_APP.name)
        } catch (_: Exception) {
            MidiClockSource.DEFAULT_APP
        }

        val controlsStyle = prefs.getBoolean("controls_style", true)
        val controlsPiano = prefs.getBoolean("controls_piano", true)
        val transportSync = prefs.getBoolean("transport_sync", true)
        val syncLatency = prefs.getInt("sync_latency_ms", 25)
        val clockOutEnabled = prefs.getBoolean("clock_out_enabled", false)

        // Initialize native audio engine sync offset and lock mode
        try {
            NativeAudioBridge.setSyncLatencyOffset(syncLatency)
            NativeAudioBridge.setMidiClockLocked(controlsStyle && clockSource == MidiClockSource.EXTERNAL_MIDI)
        } catch (_: Throwable) {}

        val savedBpm = prefs.getFloat("saved_bpm", 0f)
        val savedCalibration = if (savedBpm > 0f) {
            SavedMidiCalibration(
                bpm = savedBpm,
                jitterMs = prefs.getFloat("saved_jitter", 0.5f),
                sampleCount = prefs.getInt("saved_count", 0),
                timestamp = prefs.getLong("saved_timestamp", 0L),
                label = prefs.getString("saved_label", "Saved Reference Profile") ?: "Saved Reference Profile"
            )
        } else null

        return MidiState(
            clockSource = clockSource,
            isMidiControlsStyle = controlsStyle,
            isMidiControlsPiano = controlsPiano,
            isTransportSyncEnabled = transportSync,
            savedCalibration = savedCalibration,
            syncLatencyMs = syncLatency,
            isMidiClockOutEnabled = clockOutEnabled
        )
    }

    private fun persistSettings() {
        val s = _midiState.value
        prefs.edit()
            .putString("clock_source", s.clockSource.name)
            .putBoolean("controls_style", s.isMidiControlsStyle)
            .putBoolean("controls_piano", s.isMidiControlsPiano)
            .putBoolean("transport_sync", s.isTransportSyncEnabled)
            .putInt("sync_latency_ms", s.syncLatencyMs)
            .putBoolean("clock_out_enabled", s.isMidiClockOutEnabled)
            .apply()

        s.savedCalibration?.let { cal ->
            prefs.edit()
                .putFloat("saved_bpm", cal.bpm)
                .putFloat("saved_jitter", cal.jitterMs)
                .putInt("saved_count", cal.sampleCount)
                .putLong("saved_timestamp", cal.timestamp)
                .putString("saved_label", cal.label)
                .apply()
        }
    }

    private fun startObservationLoop() {
        scope.launch {
            while (isActive) {
                clockParser.checkTimeout()
                _midiState.update {
                    it.copy(
                        isDeviceConnected = deviceManager.isDeviceConnected.value,
                        connectedDeviceName = deviceManager.connectedDeviceName.value,
                        hasMidiOutSupport = deviceManager.hasMidiOutSupport.value,
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

    fun setClockSource(source: MidiClockSource, currentAppBpm: Float = 120f) {
        _midiState.update { it.copy(clockSource = source) }
        val isLocked = _midiState.value.isMidiControlsStyle && (source == MidiClockSource.EXTERNAL_MIDI)
        NativeAudioBridge.setMidiClockLocked(isLocked)

        val isClockOut = (source == MidiClockSource.TRANSMIT_MASTER_OUT) || _midiState.value.isMidiClockOutEnabled
        clockTransmitter.bpm = currentAppBpm
        clockTransmitter.isEnabled = isClockOut

        when (source) {
            MidiClockSource.DEFAULT_APP -> {
                // Rock-solid internal C++ frame clock; external clock ignored
            }
            MidiClockSource.TRANSMIT_MASTER_OUT -> {
                // Internal frame clock is Master; MIDI pulses actively transmitted out to keyboard
            }
            MidiClockSource.EXTERNAL_MIDI -> {
                if (_midiState.value.isMidiControlsStyle && clockParser.isClockLocked.value) {
                    NativeAudioBridge.setBpm(clockParser.detectedBpm.value)
                }
            }
            MidiClockSource.SAVED_CALIBRATION -> {
                val cal = _midiState.value.savedCalibration
                if (cal != null && cal.bpm in 30f..300f) {
                    NativeAudioBridge.setBpm(cal.bpm)
                }
            }
        }
        persistSettings()
    }

    fun setMidiControlsStyle(enabled: Boolean) {
        _midiState.update { it.copy(isMidiControlsStyle = enabled) }
        val isLocked = enabled && (_midiState.value.clockSource == MidiClockSource.EXTERNAL_MIDI)
        NativeAudioBridge.setMidiClockLocked(isLocked)

        if (isLocked && clockParser.isClockLocked.value) {
            NativeAudioBridge.setBpm(clockParser.detectedBpm.value)
        }
        persistSettings()
    }

    fun setSyncLatencyMs(ms: Int) {
        val clamped = ms.coerceIn(-100, 100)
        _midiState.update { it.copy(syncLatencyMs = clamped) }
        NativeAudioBridge.setSyncLatencyOffset(clamped)
        persistSettings()
    }

    fun setMidiControlsPiano(enabled: Boolean) {
        _midiState.update { it.copy(isMidiControlsPiano = enabled) }
        if (!enabled) {
            NativeAudioBridge.allNotesOff()
        }
        persistSettings()
    }

    fun setTransportSync(enabled: Boolean) {
        _midiState.update { it.copy(isTransportSyncEnabled = enabled) }
        persistSettings()
    }

    /**
     * Executes a 10-second timing calibration scan.
     * Listens to incoming 24 PPQN clock pulses or rhythmic taps to calculate average BPM & microtiming jitter.
     */
    fun start10SecondCalibrationScan(currentAppBpm: Float) {
        calibrationJob?.cancel()
        calibrationJob = scope.launch {
            _midiState.update { it.copy(isCalibrating = true, calibrationProgress = 0f) }
            clockParser.startCalibrationScan()

            val scanDurationMs = 10_000L
            val stepMs = 100L
            var elapsedMs = 0L

            while (elapsedMs < scanDurationMs && isActive) {
                delay(stepMs)
                elapsedMs += stepMs
                val progress = (elapsedMs.toFloat() / scanDurationMs).coerceIn(0f, 1f)
                _midiState.update { it.copy(calibrationProgress = progress) }
            }

            val calibration = clockParser.finishCalibrationScan(currentAppBpm)
            _midiState.update {
                it.copy(
                    isCalibrating = false,
                    calibrationProgress = 1f,
                    savedCalibration = calibration,
                    clockSource = MidiClockSource.SAVED_CALIBRATION
                )
            }
            NativeAudioBridge.setBpm(calibration.bpm)
            persistSettings()
        }
    }

    fun cancelCalibrationScan() {
        calibrationJob?.cancel()
        calibrationJob = null
        _midiState.update { it.copy(isCalibrating = false, calibrationProgress = 0f) }
    }

    fun setMidiClockOutEnabled(enabled: Boolean, currentBpm: Float = 120f) {
        _midiState.update { it.copy(isMidiClockOutEnabled = enabled) }
        clockTransmitter.bpm = currentBpm
        clockTransmitter.isEnabled = enabled || (_midiState.value.clockSource == MidiClockSource.TRANSMIT_MASTER_OUT)
        persistSettings()
    }

    fun onMasterPlaybackChanged(isPlaying: Boolean, currentBpm: Float) {
        clockTransmitter.bpm = currentBpm
        if (isPlaying) {
            clockTransmitter.onTransportStart()
        } else {
            clockTransmitter.onTransportStop()
        }
    }

    fun onMasterBpmChanged(bpm: Float) {
        clockTransmitter.bpm = bpm
    }

    // For backward compatibility
    fun setClockSync(enabled: Boolean) {
        setClockSource(if (enabled) MidiClockSource.EXTERNAL_MIDI else MidiClockSource.DEFAULT_APP)
    }

    fun rescan() {
        deviceManager.scanAndConnect()
    }

    fun close() {
        clockTransmitter.stop()
        calibrationJob?.cancel()
        deviceManager.stop()
    }
}
