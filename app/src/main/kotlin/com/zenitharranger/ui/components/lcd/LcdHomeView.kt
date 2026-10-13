package com.zenitharranger.ui.components.lcd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.ui.components.GenreBadgeIcon
import com.zenitharranger.ui.components.LedLineBarMeter

@Composable
fun LcdHomeColumns(
    styleName: String,
    styleGenre: String,
    accentColor: Color,
    state: ArrangerState,
    onOpenStyleSelect: () -> Unit,
    onSaveBeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Column 1: Style, Chord, Reg Mem (3 Equally Spaced Rows)
        Column(
            modifier = Modifier.weight(1.05f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StyleCardRow(
                styleName = styleName,
                styleGenre = styleGenre,
                accentColor = accentColor,
                bpm = state.bpm.toInt(),
                timeSig = "${state.timeSigNum}/${state.timeSigDen}",
                onClick = onOpenStyleSelect,
                modifier = Modifier.weight(1f)
            )

            ChordCardRow(modifier = Modifier.weight(0.9f))

            RegMemCardRow(onSave = onSaveBeat, modifier = Modifier.weight(0.9f))
        }

        // Column 2: Right Voices & Left Voices (2 Equally Spaced Rows)
        Column(
            modifier = Modifier.weight(0.95f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RightVoicesRow(modifier = Modifier.weight(1f))
            LeftVoicesRow(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun BrandingTopBar() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ZENITH ARRANGER",
            color = Color(0xFF5A6982),
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun StyleCardRow(
    styleName: String,
    styleGenre: String,
    accentColor: Color,
    bpm: Int,
    timeSig: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1524))
            .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GenreBadgeIcon(genre = styleGenre, accentColor = accentColor, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STYLE • $styleGenre", color = accentColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Text("▾", color = Color(0xFFFFB03A), fontSize = 8.sp)
                }
                Text(styleName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$timeSig  •  $bpm BPM (Opt)", color = Color(0xFF94A3B8), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ChordCardRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1524))
            .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CHORD", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("ROOT: C", color = Color(0xFF475569), fontSize = 7.sp)
            }
            Text("Cmaj7", color = Color(0xFFFFCC00), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun RegMemCardRow(onSave: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1524))
            .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("REG MEM", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("BANK 01: DEFAULT", color = Color(0xFFCBD5E1), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF162520))
                    .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .clickable { onSave() }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("SAVE", color = Color(0xFF34D399), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LcdBottomBar(
    state: ArrangerState,
    activeSectionName: String,
    queuedSectionName: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("BAR ${state.currentBar}   BEAT ${state.currentBeat}", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

            val statusText = if (state.isPlaying) "● $activeSectionName" else "○ READY"
            val statusColor = if (state.isPlaying) Color(0xFF60A5FA) else Color(0xFF64748B)
            Text(statusText, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)

            if (queuedSectionName != null && state.isPlaying) {
                Text("▸ $queuedSectionName", color = Color(0xFFFF7043), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        LedLineBarMeter(
            num = state.timeSigNum,
            den = state.timeSigDen,
            tickIndex = state.tickIndex,
            isPlaying = state.isPlaying
        )
    }
}
