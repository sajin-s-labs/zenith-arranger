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
    val state: ArrangerState = ArrangerState(false, 1, 1, 4, 4, 120f, "main_a", ""),
    val sections: List<ArrangerSection> = emptyList(),
    val savedPresets: List<BeatPreset> = BeatPresetManager.getFactoryPresets(),
    val currentPresetName: String = "Pop Standard (4/4)",
    val creator: StyleCreatorState = StyleCreatorState(),
    val midi: MidiState = MidiState()
)

/**
 * ViewModel managing Arranger playback state, section lists, background disk operations, and MIDI sync.
 */
class ArrangerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ArrangerUiState())
    val uiState: StateFlow<ArrangerUiState> = _uiState.asStateFlow()
    private val midiCoordinator = MidiCoordinator(application, viewModelScope)
    private val tapTimestamps = mutableListOf<Long>()

    init {
        NativeAudioBridge.start()
        val factoryPresets = BeatPresetManager.getFactoryPresets()
        loadPreset(factoryPresets.first())
        _uiState.update { it.copy(savedPresets = factoryPresets) }
        startStatePolling()
        viewModelScope.launch {
            midiCoordinator.midiState.collect { mState -> _uiState.update { it.copy(midi = mState) } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        midiCoordinator.close()
        NativeAudioBridge.stop()
    }

    private fun startStatePolling() {
        viewModelScope.launch {
            while (isActive) {
                val current = NativeAudioBridge.getArrangerState()
                _uiState.update { it.copy(state = current) }
                delay(30)
            }
        }
    }

    fun togglePlay() {
        if (_uiState.value.state.isPlaying) NativeAudioBridge.pause() else NativeAudioBridge.play()
    }

    fun setBpmDelta(delta: Int) =
        NativeAudioBridge.setBpm((_uiState.value.state.bpm + delta).coerceIn(30f, 300f))

    fun tapTempo() {
        val now = SystemClock.elapsedRealtime()
        tapTimestamps.removeAll { now - it > 2500 }
        tapTimestamps.add(now)
        if (tapTimestamps.size >= 2) {
            val intervals = (1 until tapTimestamps.size).map { i -> tapTimestamps[i] - tapTimestamps[i - 1] }
            val avg = intervals.average()
            if (avg > 0) NativeAudioBridge.setBpm((60_000.0 / avg).toFloat().coerceIn(40f, 280f))
        }
    }

    fun setTimeSignature(n: Int, d: Int) = NativeAudioBridge.setTimeSignature(n, d)
    fun triggerSection(id: String) = NativeAudioBridge.triggerSection(id)

    fun addSection(name: String, type: ArrangerSectionType, bars: Int) {
        val id = "sec_${System.currentTimeMillis()}"
        NativeAudioBridge.addOrUpdateSection(id, name, type.rawValue, bars, "", _uiState.value.state.bpm)
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
            val cacheFile = File(context.filesDir, "slot_${section.id}.wav")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                }
                val updated = section.copy(
                    samplePath = cacheFile.absolutePath,
                    sampleName = cacheFile.name,
                    sampleBpm = sampleBpm,
                    bars = sampleBars
                )
                NativeAudioBridge.addOrUpdateSection(
                    section.id, section.name, section.type.rawValue, sampleBars, cacheFile.absolutePath, sampleBpm
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

    fun setCreatorTitle(title: String) = _uiState.update { it.copy(creator = it.creator.copy(title = title)) }
    fun setCreatorFolder(folder: String) = _uiState.update { it.copy(creator = it.creator.copy(folder = folder)) }
    fun setCreatorTimeSig(n: Int, d: Int) = _uiState.update { it.copy(creator = it.creator.copy(timeSigNum = n, timeSigDen = d)) }
    fun setCreatorBpmDelta(delta: Int) = _uiState.update {
        it.copy(creator = it.creator.copy(bpm = (it.creator.bpm + delta).coerceIn(30f, 300f)))
    }
    fun toggleCreatorLoadMode() = _uiState.update { it.copy(creator = it.creator.copy(isLoadMode = !it.creator.isLoadMode, isClearMode = false)) }
    fun toggleCreatorClearMode() = _uiState.update { it.copy(creator = it.creator.copy(isClearMode = !it.creator.isClearMode, isLoadMode = false)) }

    fun assignCreatorSample(context: Context, uri: Uri, section: ArrangerSection) {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheFile = File(context.filesDir, "creator_${section.id}.wav")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                }
                val bars = if (section.type == ArrangerSectionType.FILL || section.type == ArrangerSectionType.BREAK) 1 else 2
                val bpm = _uiState.value.creator.bpm
                val updated = section.copy(samplePath = cacheFile.absolutePath, sampleName = cacheFile.name, sampleBpm = bpm, bars = bars)
                withContext(Dispatchers.Main) {
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
                bpm = _uiState.value.state.bpm,
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
        _uiState.update { it.copy(sections = preset.sections, currentPresetName = preset.name) }
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
