package com.zenitharranger.ui.components.keyboard

import com.zenitharranger.core.audio.NativeAudioBridge
import kotlin.math.pow

/**
 * Represents a musical piano key in the arranger keyboard bed.
 */
data class PianoKey(
    val midiNote: Int,
    val noteName: String,
    val isBlack: Boolean,
    val octave: Int
)

val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
val IS_BLACK_KEY = listOf(false, true, false, true, false, false, true, false, true, false, true, false)

fun generatePianoKeys(startOctave: Int, octaveSpan: Int): List<PianoKey> {
    val keys = mutableListOf<PianoKey>()
    val totalNotes = octaveSpan * 12 + 1 // Include top C
    val startNote = (startOctave + 1) * 12

    for (i in 0 until totalNotes) {
        val note = startNote + i
        val noteIndex = note % 12
        val octave = (note / 12) - 1
        keys.add(
            PianoKey(
                midiNote = note,
                noteName = "${NOTE_NAMES[noteIndex]}$octave",
                isBlack = IS_BLACK_KEY[noteIndex],
                octave = octave
            )
        )
    }
    return keys
}

fun generateFullArrangerKeys(): List<PianoKey> = generatePianoKeys(startOctave = 1, octaveSpan = 6)

fun midiToFrequency(midiNote: Int, pitchBendFactor: Float = 1.0f): Float {
    return (440.0 * 2.0.pow((midiNote - 69.0) / 12.0)).toFloat() * pitchBendFactor
}

/**
 * Real-time native piano engine interface routing directly to the C++ Oboe stream.
 * Zero-allocation, sub-10ms latency, zero GC spikes or audio clicks.
 */
object PianoSynthEngine {
    fun noteOn(midiNote: Int, velocity: Float = 0.85f) {
        NativeAudioBridge.noteOn(midiNote, velocity)
    }

    fun noteOff(midiNote: Int) {
        NativeAudioBridge.noteOff(midiNote)
    }

    fun allNotesOff() {
        NativeAudioBridge.allNotesOff()
    }

    fun setPitchBend(bendSemitones: Float) {
        NativeAudioBridge.setPitchBend(bendSemitones)
    }

    fun setModulation(amount: Float) {
        NativeAudioBridge.setModulation(amount)
    }

    // Backward compatibility helper
    fun playNote(midiNote: Int, pitchBend: Float = 0.0f, modulation: Float = 0.0f) {
        NativeAudioBridge.setPitchBend(pitchBend)
        NativeAudioBridge.setModulation(modulation)
        NativeAudioBridge.noteOn(midiNote, 0.85f)
    }
}
