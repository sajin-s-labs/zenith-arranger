package com.zenitharranger.ui.components.keyboard

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

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
 * Lightweight real-time synthesizer for instantaneous piano key touch playback.
 */
object PianoSynthEngine {
    private const val SAMPLE_RATE = 24000
    private var audioTrack: AudioTrack? = null
    private var isInitialized = false

    @Synchronized
    fun playNote(midiNote: Int, pitchBend: Float = 1.0f, modulation: Float = 0.0f) {
        try {
            val freq = midiToFrequency(midiNote, pitchBend)
            val durationSec = 0.35f
            val numSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = ShortArray(numSamples)

            val vibRate = 5.0f
            val vibDepth = modulation * 0.03f

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val lfo = 1.0 + vibDepth * sin(2.0 * PI * vibRate * t)
                val currentFreq = freq * lfo

                // Add fundamental + soft harmonics (Yamaha EPiano feel)
                val fundamental = sin(2.0 * PI * currentFreq * t)
                val harmonic = 0.35 * sin(4.0 * PI * currentFreq * t)
                val envelope = (1.0 - (i.toDouble() / numSamples)).pow(1.8)
                val sample = ((fundamental + harmonic) * envelope * 0.65 * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
        } catch (_: Exception) {
            // AudioTrack allocation fallback
        }
    }
}
