package com.zenitharranger.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

@Composable
fun PianoKeyboardBed(
    keys: List<PianoKey>,
    whiteKeyWidth: Dp,
    pitchBend: Float,
    modulation: Float,
    externalActiveNotes: Set<Int> = emptySet(),
    transpose: Int = 0,
    isTouchKeysEnabled: Boolean = true,
    isMidiPianoEnabled: Boolean = true,
    onNoteDown: (Int) -> Unit = {},
    onAllNotesUp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeNotes by remember { mutableStateOf(setOf<Int>()) }
    val allActiveNotes = remember(activeNotes, externalActiveNotes, isMidiPianoEnabled) {
        activeNotes + (if (isMidiPianoEnabled) externalActiveNotes else emptySet())
    }
    val pitchBendFactor = 2.0.pow((pitchBend.toDouble() * 2.0) / 12.0).toFloat()

    val whiteKeys = remember(keys) { keys.filter { !it.isBlack } }
    val blackKeys = remember(keys) { keys.filter { it.isBlack } }

    val density = LocalDensity.current
    val whiteKeyWidthPx = with(density) { whiteKeyWidth.roundToPx() }
    val exactWhiteKeyWidth = with(density) { whiteKeyWidthPx.toDp() }
    val totalWidth = exactWhiteKeyWidth * whiteKeys.size
    val blackKeyWidthPx = (whiteKeyWidthPx * 0.58f).toInt()
    val blackKeyWidth = with(density) { blackKeyWidthPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B0E14))
            .border(1.dp, Color(0xFF1B222F), RoundedCornerShape(6.dp))
            .pointerInput(keys, whiteKeyWidthPx, blackKeyWidthPx, transpose, isTouchKeysEnabled) {
                if (!isTouchKeysEnabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val currentActive = mutableSetOf<Int>()
                        val blackH = size.height * 0.60f

                        for (change in event.changes) {
                            if (change.pressed) {
                                val n = findNoteAt(change.position.x, change.position.y, whiteKeys, blackKeys, whiteKeyWidthPx, blackKeyWidthPx, blackH)
                                if (n != null) currentActive.add(n)
                            }
                        }

                        val newlyPressed = currentActive - activeNotes
                        val newlyReleased = activeNotes - currentActive

                        for (note in newlyPressed) {
                            val transposedNote = (note + transpose).coerceIn(0, 127)
                            PianoSynthEngine.noteOn(transposedNote, 0.85f)
                            onNoteDown(transposedNote)
                        }
                        for (note in newlyReleased) {
                            val transposedNote = (note + transpose).coerceIn(0, 127)
                            PianoSynthEngine.noteOff(transposedNote)
                        }
                        if (currentActive.isEmpty() && activeNotes.isNotEmpty()) {
                            onAllNotesUp()
                        }
                        activeNotes = currentActive
                    }
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            whiteKeys.forEach { key ->
                WhiteKey(key = key, isPressed = allActiveNotes.contains(key.midiNote), width = exactWhiteKeyWidth)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            blackKeys.forEach { key ->
                val whiteIndexBefore = getWhiteIndexBeforeBlackKey(key.midiNote, whiteKeys)
                if (whiteIndexBefore >= 0) {
                    val seamPx = (whiteIndexBefore + 1) * whiteKeyWidthPx
                    val blackKeyStartPx = seamPx - (blackKeyWidthPx / 2)
                    val offsetX = with(density) { blackKeyStartPx.toDp() }
                    BlackKey(key = key, isPressed = allActiveNotes.contains(key.midiNote), width = blackKeyWidth, offsetX = offsetX)
                }
            }
        }
    }
}

@Composable
private fun WhiteKey(key: PianoKey, isPressed: Boolean, width: Dp) {
    val bgBrush = if (isPressed) {
        Brush.verticalGradient(listOf(Color(0xFFB4BAC6), Color(0xFFA2AAB8), Color(0xFF9098A6)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFAFBFD), Color(0xFFECEEF2), Color(0xFFD6DBE4)))
    }
    val borderColor = if (isPressed) Color(0xFF555D6B) else Color(0xFF88909E)

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 0.5.dp)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(bgBrush)
            .border(0.6.dp, borderColor, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .padding(bottom = 3.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (key.noteName.startsWith("C")) {
            Text(
                text = key.noteName,
                color = if (isPressed) Color(0xFF1E222A) else Color(0xFF4A5260),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BlackKey(key: PianoKey, isPressed: Boolean, width: Dp, offsetX: Dp) {
    val bgBrush = if (isPressed) {
        Brush.verticalGradient(listOf(Color(0xFF14171E), Color(0xFF0C0E13), Color(0xFF060709)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF2C323D), Color(0xFF181C24), Color(0xFF0C0E14)))
    }
    val borderColor = if (isPressed) Color(0xFF64748B) else Color(0xFF384050)

    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .width(width)
            .fillMaxHeight(0.60f)
            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(bgBrush)
            .border(if (isPressed) 1.2.dp else 0.8.dp, borderColor, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
    )
}

private fun findNoteAt(
    x: Float,
    y: Float,
    whiteKeys: List<PianoKey>,
    blackKeys: List<PianoKey>,
    whiteW: Int,
    blackW: Int,
    blackH: Float
): Int? {
    if (y <= blackH) {
        for (bk in blackKeys) {
            val wi = getWhiteIndexBeforeBlackKey(bk.midiNote, whiteKeys)
            if (wi >= 0) {
                val startX = (wi + 1) * whiteW - (blackW / 2)
                if (x >= startX && x <= startX + blackW) return bk.midiNote
            }
        }
    }
    val wIndex = (x / whiteW).toInt()
    return if (wIndex in whiteKeys.indices) whiteKeys[wIndex].midiNote else null
}

private fun getWhiteIndexBeforeBlackKey(blackMidiNote: Int, whiteKeys: List<PianoKey>): Int {
    for (i in whiteKeys.indices.reversed()) {
        if (whiteKeys[i].midiNote < blackMidiNote) return i
    }
    return -1
}
