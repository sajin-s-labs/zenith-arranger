package com.zenitharranger.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.core.audio.NativeAudioBridge
import com.zenitharranger.core.midi.MidiCoordinator
import com.zenitharranger.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Immutable UI state model for the Arranger Screen.
 */
data class ArrangerUiState(
    val state: ArrangerState = ArrangerState(false, 1, 1, 4, 4, 120f, "main_1", ""),
    val mainBpm: Float = 120f,
    val sections: List<ArrangerSection> = emptyList(),
    val savedPresets: List<BeatPreset> = BeatPresetManager.getFactoryPresets(),
    val currentPresetName: String = "Pop Standard (4/4)",
    val creator: StyleCreatorState = StyleCreatorState(),
    val midi: MidiState = MidiState(),
    val isSyncStartArmed: Boolean = false,
    val isSyncStopActive: Boolean = false,
    val transpose: Int = 0,
    val isTouchKeysEnabled: Boolean = true
)

/**
 * ViewModel managing Arranger playback state, section lists, background disk operations, and MIDI sync.
 */
class ArrangerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ArrangerUiState())
    val uiState: StateFlow<ArrangerUiState> = _uiState.asStateFlow()
    private val midiCoordinator = MidiCoordinator(
        context = application,
        scope = viewModelScope,
        onNoteTriggered = { note -> onNoteDown(note) },
        onAllNotesReleased = { onAllNotesUp() },
        onTransportStart = {
            NativeAudioBridge.play()
            _uiState.update { it.copy(state = it.state.copy(isPlaying = true), isSyncStartArmed = false) }
        },
        onTransportStop = {
            NativeAudioBridge.pause()
            _uiState.update { it.copy(state = it.state.copy(isPlaying = false)) }
        },
        onExternalTempo = { bpm ->
            _uiState.update { it.copy(mainBpm = bpm, state = it.state.copy(bpm = bpm)) }
        }
    )
    private val tapTimestamps = mutableListOf<Long>()
    private val creatorTapTimestamps = mutableListOf<Long>()

    init {
        try {
            NativeAudioBridge.start()
            val factoryPresets = BeatPresetManager.getFactoryPresets()
            loadPreset(factoryPresets.first())
            _uiState.update { it.copy(savedPresets = factoryPresets) }
            startStatePolling()
            viewModelScope.launch {
                midiCoordinator.midiState.collect { mState -> _uiState.update { it.copy(midi = mState) } }
            }
        } catch (t: Throwable) {
            android.util.Log.e("ArrangerViewModel", "Error during initialization", t)
        }
    }

    override fun onCleared() {
        super.onCleared()
        midiCoordinator.close()
        NativeAudioBridge.stop()
    }

    private fun startStatePolling() {
        var lastIsPlaying: Boolean? = null
        var lastBpm: Float? = null
        viewModelScope.launch {
            while (isActive) {
                val current = NativeAudioBridge.getArrangerState()
                _uiState.update { it.copy(state = current) }

                if (lastIsPlaying != current.isPlaying) {
                    lastIsPlaying = current.isPlaying
                    midiCoordinator.onMasterPlaybackChanged(current.isPlaying, current.bpm)
                }
                if (lastBpm != current.bpm) {
                    lastBpm = current.bpm
                    midiCoordinator.onMasterBpmChanged(current.bpm)
                }

                delay(30)
            }
        }
    }

    fun onEnterStyleCreator() {
        if (_uiState.value.state.isPlaying) {
            NativeAudioBridge.pause()
        }
        val cr = _uiState.value.creator
        NativeAudioBridge.clearAllSections()
        NativeAudioBridge.setBpm(cr.bpm)
        NativeAudioBridge.setTimeSignature(cr.timeSigNum, cr.timeSigDen)
        cr.sections.forEach { s ->
            NativeAudioBridge.addOrUpdateSection(s.id, s.name, s.type.rawValue, s.bars, s.samplePath, s.sampleBpm)
        }
        if (cr.sections.none { it.id == "break_1" || it.type == ArrangerSectionType.BREAK }) {
            NativeAudioBridge.addOrUpdateSection("break_1", "Break", ArrangerSectionType.BREAK.rawValue, 1, "", cr.bpm)
        }
    }

    fun onEnterMainWorkstation() {
        if (_uiState.value.creator.isPlaying) {
            NativeAudioBridge.pause()
            _uiState.update { it.copy(creator = it.creator.copy(isPlaying = false)) }
        }
        val bpm = _uiState.value.mainBpm
        NativeAudioBridge.clearAllSections()
        NativeAudioBridge.setBpm(bpm)
        NativeAudioBridge.setTimeSignature(_uiState.value.state.timeSigNum, _uiState.value.state.timeSigDen)
        _uiState.value.sections.forEach { s ->
            NativeAudioBridge.addOrUpdateSection(s.id, s.name, s.type.rawValue, s.bars, s.samplePath, s.sampleBpm)
        }
        if (_uiState.value.sections.none { it.id == "break_1" || it.type == ArrangerSectionType.BREAK }) {
            NativeAudioBridge.addOrUpdateSection("break_1", "Break", ArrangerSectionType.BREAK.rawValue, 1, "", bpm)
        }
    }

    fun togglePlay() {
        if (_uiState.value.state.isPlaying) NativeAudioBridge.pause() else NativeAudioBridge.play()
    }

    fun toggleSyncStart() {
        _uiState.update { it.copy(isSyncStartArmed = !it.isSyncStartArmed) }
    }

    fun toggleSyncStop() {
        _uiState.update { it.copy(isSyncStopActive = !it.isSyncStopActive) }
    }

    fun setTransposeDelta(delta: Int) {
        val newTrsp = (_uiState.value.transpose + delta).coerceIn(-12, 12)
        _uiState.update { it.copy(transpose = newTrsp) }
    }

    fun onNoteDown(note: Int) {
        if (_uiState.value.isSyncStartArmed && !_uiState.value.state.isPlaying) {
            NativeAudioBridge.play()
            _uiState.update { it.copy(isSyncStartArmed = false, state = it.state.copy(isPlaying = true)) }
        }
    }

    fun onAllNotesUp() {
        if (_uiState.value.isSyncStopActive && _uiState.value.state.isPlaying) {
            NativeAudioBridge.pause()
            _uiState.update { it.copy(state = it.state.copy(isPlaying = false)) }
        }
    }

    fun setBpmDelta(delta: Int) {
        val newBpm = (_uiState.value.mainBpm + delta).coerceIn(30f, 300f)
        _uiState.update { it.copy(mainBpm = newBpm) }
        NativeAudioBridge.setBpm(newBpm)
    }

    fun tapTempo() {
        val now = SystemClock.elapsedRealtime()
        tapTimestamps.removeAll { now - it > 2500 }
        tapTimestamps.add(now)
        if (tapTimestamps.size >= 2) {
            val intervals = (1 until tapTimestamps.size).map { i -> tapTimestamps[i] - tapTimestamps[i - 1] }
            val avg = intervals.average()
            if (avg > 0) {
                val calcBpm = (60_000.0 / avg).toFloat().coerceIn(40f, 280f)
                _uiState.update { it.copy(mainBpm = calcBpm) }
                NativeAudioBridge.setBpm(calcBpm)
            }
        }
    }

    fun setTimeSignature(n: Int, d: Int) {
        NativeAudioBridge.setTimeSignature(n, d)
        _uiState.update { it.copy(state = it.state.copy(timeSigNum = n, timeSigDen = d)) }
    }
    fun triggerSection(id: String) = NativeAudioBridge.triggerSection(id)

    fun addSection(name: String, type: ArrangerSectionType, bars: Int) {
        val id = "sec_${System.currentTimeMillis()}"
        NativeAudioBridge.addOrUpdateSection(id, name, type.rawValue, bars, "", _uiState.value.mainBpm)
        _uiState.update { it.copy(sections = it.sections + ArrangerSection(id, name, type, bars)) }
    }

    fun deleteSection(id: String) {
        NativeAudioBridge.removeSection(id)
        _uiState.update { it.copy(sections = it.sections.filterNot { s -> s.id == id }) }
    }

    fun assignSample(
        context: Context,
        uri: Uri,
        section: ArrangerSection,
        sampleBpm: Float,
        sampleBars: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val styleClean = BeatPresetManager.sanitizeName(_uiState.value.currentPresetName.ifBlank { "User_Style" })
            val styleDir = File(context.filesDir, "styles/$styleClean")
            styleDir.mkdirs()
            val cacheFile = File(styleDir, "${section.id}.wav")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                }
                val actualBars = if (sampleBars > 0) sampleBars else (if (section.type == ArrangerSectionType.FILL || section.type == ArrangerSectionType.BREAK) 1 else 2)
                val updated = section.copy(
                    samplePath = cacheFile.absolutePath,
                    sampleName = cacheFile.name,
                    sampleBpm = sampleBpm,
                    bars = actualBars
                )
                NativeAudioBridge.addOrUpdateSection(
                    section.id, section.name, section.type.rawValue, actualBars, cacheFile.absolutePath, sampleBpm
                )
                withContext(Dispatchers.Main) {
                    _uiState.update { s ->
                        val exists = s.sections.any { it.id == section.id }
                        val next = if (exists) s.sections.map { if (it.id == section.id) updated else it } else s.sections + updated
                        s.copy(sections = next)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ==================== STYLE CREATOR INDEPENDENT CONTROLS ====================

    fun setCreatorTitle(title: String) = _uiState.update { it.copy(creator = it.creator.copy(title = title)) }
    fun setCreatorFolder(folder: String) = _uiState.update { it.copy(creator = it.creator.copy(folder = folder)) }
    fun setCreatorTimeSig(n: Int, d: Int) {
        _uiState.update { it.copy(creator = it.creator.copy(timeSigNum = n, timeSigDen = d)) }
        NativeAudioBridge.setTimeSignature(n, d)
    }

    fun setCreatorBpmDelta(delta: Int) {
        val newBpm = (_uiState.value.creator.bpm + delta).coerceIn(30f, 300f)
        _uiState.update { it.copy(creator = it.creator.copy(bpm = newBpm)) }
        NativeAudioBridge.setBpm(newBpm)
    }

    fun tapCreatorTempo() {
        val now = SystemClock.elapsedRealtime()
        creatorTapTimestamps.removeAll { now - it > 2500 }
        creatorTapTimestamps.add(now)
        if (creatorTapTimestamps.size >= 2) {
            val intervals = (1 until creatorTapTimestamps.size).map { i -> creatorTapTimestamps[i] - creatorTapTimestamps[i - 1] }
            val avg = intervals.average()
            if (avg > 0) {
                val calcBpm = (60_000.0 / avg).toFloat().coerceIn(30f, 300f)
                _uiState.update { it.copy(creator = it.creator.copy(bpm = calcBpm)) }
                NativeAudioBridge.setBpm(calcBpm)
            }
        }
    }

    fun toggleCreatorPlay() {
        val cr = _uiState.value.creator
        if (cr.isPlaying) {
            NativeAudioBridge.pause()
            _uiState.update { it.copy(creator = it.creator.copy(isPlaying = false)) }
        } else {
            NativeAudioBridge.clearAllSections()
            NativeAudioBridge.setBpm(cr.bpm)
            NativeAudioBridge.setTimeSignature(cr.timeSigNum, cr.timeSigDen)
            cr.sections.forEach { s ->
                NativeAudioBridge.addOrUpdateSection(s.id, s.name, s.type.rawValue, s.bars, s.samplePath, s.sampleBpm)
            }
            if (cr.sections.none { it.id == "break_1" || it.type == ArrangerSectionType.BREAK }) {
                NativeAudioBridge.addOrUpdateSection("break_1", "Break", ArrangerSectionType.BREAK.rawValue, 1, "", cr.bpm)
            }
            val chosenSlot = if (cr.activeSlot.isNotBlank() && cr.sections.any { it.id == cr.activeSlot }) {
                cr.activeSlot
            } else {
                cr.sections.firstOrNull { it.samplePath.isNotBlank() }?.id ?: "main_1"
            }
            NativeAudioBridge.triggerSection(chosenSlot)
            NativeAudioBridge.play()
            _uiState.update { it.copy(creator = it.creator.copy(isPlaying = true, activeSlot = chosenSlot)) }
        }
    }

    fun triggerCreatorSection(slotId: String) {
        val cr = _uiState.value.creator
        val sec = cr.sections.firstOrNull { it.id == slotId }
        val isBreak = slotId == "break_1" || sec?.type == ArrangerSectionType.BREAK
        if (sec != null && sec.samplePath.isNotBlank()) {
            NativeAudioBridge.addOrUpdateSection(sec.id, sec.name, sec.type.rawValue, sec.bars, sec.samplePath, sec.sampleBpm)
            NativeAudioBridge.triggerSection(sec.id)
        } else if (isBreak) {
            NativeAudioBridge.triggerSection("break_1")
        }
        if (!cr.isPlaying) {
            NativeAudioBridge.setBpm(cr.bpm)
            NativeAudioBridge.setTimeSignature(cr.timeSigNum, cr.timeSigDen)
            NativeAudioBridge.play()
        }
        _uiState.update { it.copy(creator = it.creator.copy(isPlaying = true, activeSlot = slotId)) }
    }

    fun toggleCreatorLoadMode() = _uiState.update { it.copy(creator = it.creator.copy(isLoadMode = !it.creator.isLoadMode, isClearMode = false)) }
    fun toggleCreatorClearMode() = _uiState.update { it.copy(creator = it.creator.copy(isClearMode = !it.creator.isClearMode, isLoadMode = false)) }

    fun assignCreatorSample(context: Context, uri: Uri, section: ArrangerSection, bars: Int = 2) {
        viewModelScope.launch(Dispatchers.IO) {
            val styleClean = BeatPresetManager.sanitizeName(_uiState.value.creator.title.ifBlank { "User_Style" })
            val styleDir = File(context.filesDir, "styles/$styleClean")
            styleDir.mkdirs()
            val cacheFile = File(styleDir, "${section.id}.wav")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                }
                val actualBars = if (bars > 0) bars else (if (section.type == ArrangerSectionType.FILL || section.type == ArrangerSectionType.BREAK) 1 else 2)
                val bpm = _uiState.value.creator.bpm
                val updated = section.copy(samplePath = cacheFile.absolutePath, sampleName = cacheFile.name, sampleBpm = bpm, bars = actualBars)
                withContext(Dispatchers.Main) {
                    NativeAudioBridge.addOrUpdateSection(updated.id, updated.name, updated.type.rawValue, updated.bars, updated.samplePath, updated.sampleBpm)
                    _uiState.update { s ->
                        val exists = s.creator.sections.any { it.id == section.id }
                        val next = if (exists) s.creator.sections.map { if (it.id == section.id) updated else it } else s.creator.sections + updated
                        s.copy(creator = s.creator.copy(sections = next, isLoadMode = false))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun removeCreatorSample(sectionId: String) {
        NativeAudioBridge.removeSection(sectionId)
        _uiState.update { s ->
            val next = s.creator.sections.filterNot { it.id == sectionId }
            s.copy(creator = s.creator.copy(sections = next, isClearMode = false))
        }
    }

    fun saveCreatorStyle(context: Context): Boolean {
        val cr = _uiState.value.creator
        val cleanFolder = BeatPresetManager.sanitizeName(cr.folder.ifBlank { "My_Beats" })
        val cleanId = BeatPresetManager.sanitizeName(cr.title.ifBlank { "User_Style" })
        val preset = BeatPreset(
            id = cleanId, name = cr.title, bpm = cr.bpm,
            timeSigNum = cr.timeSigNum, timeSigDen = cr.timeSigDen,
            sections = cr.sections, folder = cleanFolder, category = "User"
        )
        val success = BeatPresetManager.saveUserPreset(context, preset, cleanFolder)
        if (success) refreshPresets(context)
        return success
    }

    fun refreshPresets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val presets = BeatPresetManager.listPresets(context)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(savedPresets = presets) }
            }
        }
    }

    fun savePreset(context: Context, name: String) = savePresetToFolder(context, name, "My_Beats")

    fun setMidiClockSource(source: MidiClockSource) = midiCoordinator.setClockSource(source, _uiState.value.state.bpm)
    fun setMidiControlsStyle(enabled: Boolean) = midiCoordinator.setMidiControlsStyle(enabled)
    fun setMidiControlsPiano(enabled: Boolean) = midiCoordinator.setMidiControlsPiano(enabled)
    fun setSyncLatencyMs(ms: Int) = midiCoordinator.setSyncLatencyMs(ms)
    fun setMidiClockOutEnabled(enabled: Boolean) = midiCoordinator.setMidiClockOutEnabled(enabled, _uiState.value.state.bpm)
    fun toggleMidiClockOut() {
        val next = !_uiState.value.midi.isMidiClockOutEnabled
        setMidiClockOutEnabled(next)
    }
    fun toggleMidiControlsPiano() {
        val next = !_uiState.value.midi.isMidiControlsPiano
        setMidiControlsPiano(next)
    }
    fun toggleTouchKeysEnabled() {
        val next = !_uiState.value.isTouchKeysEnabled
        if (!next) {
            NativeAudioBridge.allNotesOff()
        }
        _uiState.update { it.copy(isTouchKeysEnabled = next) }
    }
    fun startMidiCalibrationScan() = midiCoordinator.start10SecondCalibrationScan(_uiState.value.state.bpm)
    fun cancelMidiCalibrationScan() = midiCoordinator.cancelCalibrationScan()
    fun setMidiClockSync(enabled: Boolean) = midiCoordinator.setClockSync(enabled)
    fun setMidiTransportSync(enabled: Boolean) = midiCoordinator.setTransportSync(enabled)
    fun rescanMidi() = midiCoordinator.rescan()

    fun savePresetToFolder(context: Context, name: String, folder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanId = BeatPresetManager.sanitizeName(name.ifBlank { "Untitled_Style" })
            val cleanFolder = BeatPresetManager.sanitizeName(folder.ifBlank { "My_Beats" })
            val preset = BeatPreset(
                id = cleanId,
                name = name,
                bpm = _uiState.value.mainBpm,
                timeSigNum = _uiState.value.state.timeSigNum,
                timeSigDen = _uiState.value.state.timeSigDen,
                sections = _uiState.value.sections,
                folder = cleanFolder,
                category = "User"
            )
            BeatPresetManager.saveUserPreset(context, preset, cleanFolder)
            val updated = BeatPresetManager.listPresets(context)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(savedPresets = updated, currentPresetName = name) }
            }
        }
    }

    fun loadPreset(preset: BeatPreset) {
        NativeAudioBridge.clearAllSections()
        NativeAudioBridge.setBpm(preset.bpm)
        NativeAudioBridge.setTimeSignature(preset.timeSigNum, preset.timeSigDen)
        preset.sections.forEach { s ->
            NativeAudioBridge.addOrUpdateSection(s.id, s.name, s.type.rawValue, s.bars, s.samplePath, s.sampleBpm)
        }
        if (preset.sections.none { it.id == "break_1" || it.type == ArrangerSectionType.BREAK }) {
            NativeAudioBridge.addOrUpdateSection("break_1", "Break", ArrangerSectionType.BREAK.rawValue, 1, "", preset.bpm)
        }
        _uiState.update { it.copy(sections = preset.sections, currentPresetName = preset.name, mainBpm = preset.bpm) }
    }

    fun deletePreset(context: Context, presetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            BeatPresetManager.deletePreset(context, presetId)
            val updated = BeatPresetManager.listPresets(context)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(savedPresets = updated) }
            }
        }
    }
}
