package com.zenitharranger.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Yamaha-style genre emblem icon rendered via Canvas vectors.
 */
@Composable
fun GenreBadgeIcon(
    genre: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.18f))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            when {
                genre.contains("POP") -> {
                    // Stylized Electric Guitar / Wave emblem
                    drawLine(accentColor, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.2f), strokeWidth = 3f)
                    drawCircle(accentColor, radius = w * 0.25f, center = Offset(w * 0.35f, h * 0.65f))
                    drawCircle(Color.Black, radius = w * 0.1f, center = Offset(w * 0.35f, h * 0.65f))
                }
                genre.contains("WALTZ") || genre.contains("BALLAD") -> {
                    // Piano keys emblem
                    drawRect(accentColor, Offset(0f, 0f), Size(w, h))
                    val keyW = w / 4f
                    for (i in 0..3) {
                        drawRect(Color(0xFF1E293B), Offset(i * keyW + 1f, h * 0.5f), Size(keyW - 2f, h * 0.5f))
                    }
                    drawRect(Color.Black, Offset(keyW * 0.7f, 0f), Size(keyW * 0.6f, h * 0.5f))
                    drawRect(Color.Black, Offset(keyW * 1.7f, 0f), Size(keyW * 0.6f, h * 0.5f))
                }
                genre.contains("BLUES") || genre.contains("JAZZ") -> {
                    // Eighth notes emblem
                    drawCircle(accentColor, radius = w * 0.18f, center = Offset(w * 0.3f, h * 0.75f))
                    drawCircle(accentColor, radius = w * 0.18f, center = Offset(w * 0.75f, h * 0.65f))
                    drawLine(accentColor, Offset(w * 0.42f, h * 0.75f), Offset(w * 0.42f, h * 0.2f), strokeWidth = 2.5f)
                    drawLine(accentColor, Offset(w * 0.87f, h * 0.65f), Offset(w * 0.87f, h * 0.1f), strokeWidth = 2.5f)
                    drawLine(accentColor, Offset(w * 0.42f, h * 0.2f), Offset(w * 0.87f, h * 0.1f), strokeWidth = 3.5f)
                }
                else -> {
                    // Pulse / Synth beat icon
                    val midY = h * 0.5f
                    drawLine(accentColor, Offset(0f, midY), Offset(w * 0.25f, midY), strokeWidth = 2.5f)
                    drawLine(accentColor, Offset(w * 0.25f, midY), Offset(w * 0.45f, h * 0.15f), strokeWidth = 2.5f)
                    drawLine(accentColor, Offset(w * 0.45f, h * 0.15f), Offset(w * 0.65f, h * 0.85f), strokeWidth = 2.5f)
                    drawLine(accentColor, Offset(w * 0.65f, h * 0.85f), Offset(w * 0.8f, midY), strokeWidth = 2.5f)
                    drawLine(accentColor, Offset(w * 0.8f, midY), Offset(w, midY), strokeWidth = 2.5f)
                }
            }
        }
    }
}

data class LedColSpec(
    val lineTick: Int,
    val dotTicks: List<Int>,
    val isEnabled: Boolean
)

fun getLedColumns(num: Int, den: Int): List<LedColSpec> = when {
    num == 3 -> listOf(
        LedColSpec(0, listOf(1), true), LedColSpec(2, listOf(3), true),
        LedColSpec(4, listOf(5), true), LedColSpec(-1, emptyList(), false)
    )
    num == 6 -> listOf(
        LedColSpec(0, listOf(1, 2), true), LedColSpec(3, listOf(4, 5), true),
        LedColSpec(6, listOf(7, 8), true), LedColSpec(9, listOf(10, 11), true)
    )
    num == 7 -> listOf(
        LedColSpec(0, listOf(1, 2), true), LedColSpec(3, listOf(4), true),
        LedColSpec(5, listOf(6), true), LedColSpec(-1, emptyList(), false)
    )
    num == 5 -> listOf(
        LedColSpec(0, listOf(1, 2), true), LedColSpec(3, listOf(4), true),
        LedColSpec(-1, emptyList(), false), LedColSpec(-1, emptyList(), false)
    )
    else -> listOf(
        LedColSpec(0, listOf(1), true), LedColSpec(2, listOf(3), true),
        LedColSpec(4, listOf(5), true), LedColSpec(6, listOf(7), true)
    )
}

/**
 * 4 thin sharp rectangular LED lines with micro circular sub-beat dots below each line.
 * Lines represent the actual time signature beats; dots are the sub-beat helpers:
 * 4/4 (1 and 2 and 3 and 4 and): 8 ticks
 * 3/4 (1 and 2 and 3 and): 6 ticks (Col 4 disabled)
 * 6/8 (1 and and 2 and and 3 and and 4 and and): 12 ticks
 * 7/8 (1 and and 2 and 3 and): 7 ticks (Col 4 disabled)
 * 5/8 (1 and and 2 and): 5 ticks (Cols 3 & 4 disabled)
 */
@Composable
fun LedLineBarMeter(
    num: Int,
    den: Int,
    tickIndex: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val columns = getLedColumns(num, den)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEachIndexed { colIdx, spec ->
            LedColumn(colIdx, spec, tickIndex, isPlaying)
        }
    }
}

@Composable
private fun LedColumn(colIdx: Int, spec: LedColSpec, tickIndex: Int, isPlaying: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val isLineActive = isPlaying && spec.isEnabled && spec.lineTick == tickIndex
        val lineBase = if (colIdx == 0) Color(0xFFFFB03A) else Color(0xFF10B981)
        val lineGlow = if (colIdx == 0) Color(0xFFFFE082) else Color(0xFF6EE7B7)

        Box(
            modifier = Modifier
                .width(18.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(0.8.dp))
                .background(
                    if (isLineActive) androidx.compose.ui.graphics.Brush.verticalGradient(listOf(lineGlow, lineBase))
                    else if (spec.isEnabled) androidx.compose.ui.graphics.SolidColor(Color(0xFF0D1420))
                    else androidx.compose.ui.graphics.SolidColor(Color(0xFF070B12))
                )
                .border(
                    0.8.dp,
                    if (isLineActive) lineGlow else if (spec.isEnabled) Color(0xFF192436) else Color(0xFF10151E),
                    RoundedCornerShape(0.8.dp)
                )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(4.dp)
        ) {
            if (spec.isEnabled && spec.dotTicks.isNotEmpty()) {
                spec.dotTicks.forEach { dTick ->
                    val isDotActive = isPlaying && dTick == tickIndex
                    val dotBase = if (colIdx == 0) Color(0xFFF59E0B) else Color(0xFF059669)
                    val dotGlow = if (colIdx == 0) Color(0xFFFDE68A) else Color(0xFFA7F3D0)
                    Box(
                        modifier = Modifier
                            .size(3.2.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isDotActive) dotBase else Color(0xFF111722))
                            .border(0.5.dp, if (isDotActive) dotGlow else Color(0xFF1E2838), androidx.compose.foundation.shape.CircleShape)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
