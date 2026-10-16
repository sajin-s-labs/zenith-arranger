package com.zenitharranger.core.midi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.round

/**
 * Parses MIDI real-time timing messages (0xF8 Timing Clock at 24 PPQN, 0xFA Start, 0xFC Stop).
 * Smooths clock jitter using a 96-pulse (4-beat) rolling window, snaps to integer/half BPM,
 * deadbands small USB timing flutter (<0.5 BPM), and delivers direct pulse callbacks for phase locking.
 */
class MidiClockParser(
    private val onTempoChanged: (Float) -> Unit = {},
    private val onMidiStart: () -> Unit = {},
    private val onMidiStop: () -> Unit = {},
    private val onClockPulse: () -> Unit = {}
) {
    companion object {
        const val PPQN = 24
        const val WINDOW_SIZE = 96 // 4 quarter notes / 1 measure in 4/4
        const val CLOCK_BYTE: Byte = 0xF8.toByte()
        const val START_BYTE: Byte = 0xFA.toByte()
        const val CONTINUE_BYTE: Byte = 0xFB.toByte()
        const val STOP_BYTE: Byte = 0xFC.toByte()
        private const val TIMEOUT_NANOS = 350_000_000L // 350 ms without pulse = clock stopped
    }

    private val timestamps = LongArray(WINDOW_SIZE)
    private var pulseIndex = 0
    private var pulseCount = 0
    private var lastPulseTime = 0L

    private var pendingCandidateBpm = 0f
    private var candidateConfidence = 0

    private val _isClockLocked = MutableStateFlow(false)
    val isClockLocked: StateFlow<Boolean> = _isClockLocked.asStateFlow()

    private val _detectedBpm = MutableStateFlow(0f)
    val detectedBpm: StateFlow<Float> = _detectedBpm.asStateFlow()

    private val _pulseTick = MutableStateFlow(0L)
    val pulseTick: StateFlow<Long> = _pulseTick.asStateFlow()

    // 10-Second Timing Calibration Scan
    private val calibrationSamples = mutableListOf<Long>()
    private var isCalibratingScan = false
    private var calibrationStartTime = 0L

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
        val slot = pulseIndex % WINDOW_SIZE
        timestamps[slot] = now
        pulseIndex = (pulseIndex + 1) % WINDOW_SIZE
        pulseCount++

        _pulseTick.value = pulseCount.toLong()

        // Forward raw clock pulse to native engine for sub-sample phase locking
        onClockPulse()

        if (isCalibratingScan) {
            calibrationSamples.add(now)
        }

        // Only evaluate tempo updates once every 24 pulses (1 quarter note) to avoid CPU flutter
        if (pulseCount % PPQN == 0) {
            evaluateTempo(now)
        }
    }

    private fun evaluateTempo(now: Long) {
        if (pulseCount < PPQN) return

        val pulsesToMeasure = if (pulseCount >= WINDOW_SIZE) WINDOW_SIZE else PPQN
        val oldestIndex = (pulseIndex - pulsesToMeasure + WINDOW_SIZE) % WINDOW_SIZE
        val oldestTime = timestamps[oldestIndex]

        if (oldestTime <= 0L) return

        val durationNanos = now - oldestTime
        if (durationNanos <= 0L) return

        val durationSec = durationNanos.toDouble() / 1_000_000_000.0
        val beatsMeasured = pulsesToMeasure.toDouble() / PPQN.toDouble()
        val rawBpm = ((beatsMeasured * 60.0) / durationSec).toFloat()

        if (rawBpm !in 30.0f..300.0f) return

        // Hardware keyboards transmit whole integer BPMs (90, 120, 128) or half-steps (120.5).
        // Snap raw measured BPM to eliminate USB interrupt polling jitter.
        val nearestInt = round(rawBpm)
        val nearestHalf = round(rawBpm * 2f) / 2f

        val candidateBpm = when {
            abs(rawBpm - nearestInt) <= 0.35f -> nearestInt
            abs(rawBpm - nearestHalf) <= 0.15f -> nearestHalf
            else -> round(rawBpm * 10f) / 10f
        }

        val currentBpm = _detectedBpm.value
        if (currentBpm <= 0f || !_isClockLocked.value) {
            // Initial lock acquisition
            _detectedBpm.value = candidateBpm
            _isClockLocked.value = true
            onTempoChanged(candidateBpm)
            return
        }

        val diff = abs(candidateBpm - currentBpm)

        when {
            // User turned the tempo dial on their external keyboard (significant shift)
            diff >= 2.0f -> {
                _detectedBpm.value = candidateBpm
                _isClockLocked.value = true
                candidateConfidence = 0
                pendingCandidateBpm = candidateBpm
                onTempoChanged(candidateBpm)
            }
            // Small deliberate tempo change (0.5 .. 2.0 BPM) - confirm over 2 consecutive beats
            diff >= 0.5f -> {
                if (candidateBpm == pendingCandidateBpm) {
                    candidateConfidence++
                    if (candidateConfidence >= 2) {
                        _detectedBpm.value = candidateBpm
                        _isClockLocked.value = true
                        onTempoChanged(candidateBpm)
                        candidateConfidence = 0
                    }
                } else {
                    pendingCandidateBpm = candidateBpm
                    candidateConfidence = 1
                }
            }
            // Deadband: variation is < 0.5 BPM -> ignore USB jitter to prevent display jumping and speed-hunting
            else -> {
                candidateConfidence = 0
                pendingCandidateBpm = currentBpm
                _isClockLocked.value = true
            }
        }
    }

    fun recordManualTap(now: Long = System.nanoTime()) {
        if (isCalibratingScan) {
            calibrationSamples.add(now)
        }
    }

    fun startCalibrationScan() {
        calibrationSamples.clear()
        calibrationStartTime = System.nanoTime()
        isCalibratingScan = true
    }

    fun finishCalibrationScan(targetBpmIfManual: Float = 120.0f): com.zenitharranger.model.SavedMidiCalibration {
        isCalibratingScan = false
        if (calibrationSamples.size < 2) {
            return com.zenitharranger.model.SavedMidiCalibration(
                bpm = if (_detectedBpm.value > 30f) _detectedBpm.value else targetBpmIfManual,
                jitterMs = 0.4f,
                sampleCount = calibrationSamples.size,
                label = "Saved Reference (${_detectedBpm.value.toInt()} BPM)"
            )
        }

        val intervals = mutableListOf<Double>()
        for (i in 1 until calibrationSamples.size) {
            val deltaNanos = (calibrationSamples[i] - calibrationSamples[i - 1]).toDouble()
            if (deltaNanos > 0) {
                intervals.add(deltaNanos)
            }
        }

        if (intervals.isEmpty()) {
            return com.zenitharranger.model.SavedMidiCalibration(
                bpm = if (_detectedBpm.value > 30f) _detectedBpm.value else targetBpmIfManual,
                jitterMs = 0.5f,
                sampleCount = 0
            )
        }

        val meanNanos = intervals.average()
        val meanSec = meanNanos / 1_000_000_000.0

        // If pulses were 24 PPQN clock:
        val bpmFromPpqn = (60.0 / (meanSec * 24.0)).toFloat()
        // If pulses were beat taps (quarter notes):
        val bpmFromBeats = (60.0 / meanSec).toFloat()

        val finalBpm = when {
            bpmFromPpqn in 30.0f..300.0f -> (bpmFromPpqn * 10f).toInt() / 10f
            bpmFromBeats in 30.0f..300.0f -> (bpmFromBeats * 10f).toInt() / 10f
            _detectedBpm.value in 30.0f..300.0f -> _detectedBpm.value
            else -> targetBpmIfManual
        }

        val variance = intervals.map { (it - meanNanos) * (it - meanNanos) }.average()
        val stdDevMs = (kotlin.math.sqrt(variance) / 1_000_000.0).toFloat()
        val cleanJitter = ((stdDevMs * 100f).toInt() / 100f).coerceIn(0.01f, 50.0f)

        return com.zenitharranger.model.SavedMidiCalibration(
            bpm = finalBpm,
            jitterMs = cleanJitter,
            sampleCount = intervals.size,
            label = "Calibrated profile ($finalBpm BPM, ±${cleanJitter}ms jitter)"
        )
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
        pendingCandidateBpm = 0f
        candidateConfidence = 0
        _isClockLocked.value = false
    }
}
