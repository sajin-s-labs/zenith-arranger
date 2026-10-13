package com.zenitharranger.core.midi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Parses MIDI real-time timing messages (0xF8 Timing Clock at 24 PPQN, 0xFA Start, 0xFC Stop).
 * Smooths clock jitter using a 24-pulse rolling window and calculates tempo in real time.
 */
class MidiClockParser(
    private val onTempoChanged: (Float) -> Unit = {},
    private val onMidiStart: () -> Unit = {},
    private val onMidiStop: () -> Unit = {}
) {
    companion object {
        const val PPQN = 24
        const val CLOCK_BYTE: Byte = 0xF8.toByte()
        const val START_BYTE: Byte = 0xFA.toByte()
        const val CONTINUE_BYTE: Byte = 0xFB.toByte()
        const val STOP_BYTE: Byte = 0xFC.toByte()
        private const val TIMEOUT_NANOS = 350_000_000L // 350 ms without pulse = clock stopped
    }

    private val timestamps = LongArray(PPQN)
    private var pulseIndex = 0
    private var pulseCount = 0
    private var lastPulseTime = 0L

    private val _isClockLocked = MutableStateFlow(false)
    val isClockLocked: StateFlow<Boolean> = _isClockLocked.asStateFlow()

    private val _detectedBpm = MutableStateFlow(0f)
    val detectedBpm: StateFlow<Float> = _detectedBpm.asStateFlow()

    private val _pulseTick = MutableStateFlow(0L)
    val pulseTick: StateFlow<Long> = _pulseTick.asStateFlow()

    fun onByteReceived(byte: Byte, timestampNanos: Long = System.nanoTime()) {
        when (byte) {
            CLOCK_BYTE -> handleClockPulse(timestampNanos)
            START_BYTE -> {
                resetClock()
                onMidiStart()
            }
            CONTINUE_BYTE -> onMidiStart()
            STOP_BYTE -> {
                _isClockLocked.value = false
                onMidiStop()
            }
        }
    }

    private fun handleClockPulse(now: Long) {
        lastPulseTime = now
        val slot = pulseIndex % PPQN
        val prevOldest = timestamps[slot]
        timestamps[slot] = now
        pulseIndex = (pulseIndex + 1) % PPQN
        pulseCount++

        _pulseTick.value = pulseCount.toLong()

        if (pulseCount >= PPQN && prevOldest > 0L) {
            calculateBpm(now, prevOldest)
        }
    }

    private fun calculateBpm(newest: Long, oldest: Long) {
        val quarterNoteDurationNanos = newest - oldest
        if (quarterNoteDurationNanos > 0L) {
            val seconds = quarterNoteDurationNanos.toDouble() / 1_000_000_000.0
            val rawBpm = (60.0 / seconds).toFloat()
            if (rawBpm in 30.0f..300.0f) {
                val smoothed = if (_detectedBpm.value > 0f) {
                    (_detectedBpm.value * 0.7f) + (rawBpm * 0.3f)
                } else rawBpm

                val rounded = (smoothed * 10f).toInt() / 10f
                _detectedBpm.value = rounded
                _isClockLocked.value = true
                onTempoChanged(rounded)
            }
        }
    }

    fun checkTimeout(now: Long = System.nanoTime()) {
        if (_isClockLocked.value && lastPulseTime > 0L && (now - lastPulseTime) > TIMEOUT_NANOS) {
            _isClockLocked.value = false
        }
    }

    fun resetClock() {
        pulseCount = 0
        pulseIndex = 0
        lastPulseTime = 0L
        timestamps.fill(0L)
        _isClockLocked.value = false
    }
}
