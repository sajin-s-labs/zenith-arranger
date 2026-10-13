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
import com.zenitharranger.model.BeatPreset
import com.zenitharranger.model.accentColorHex
import com.zenitharranger.model.genre
import com.zenitharranger.ui.components.GenreBadgeIcon

/**
 * 8-slot style grid (4 on left side, 4 on right side) matching Yamaha workstation hardware screens.
 */
@Composable
fun StyleGridColumn(
    styles: List<BeatPreset>,
    activePresetName: String,
    currentPage: Int,
    totalPages: Int,
    onSelectPage: (Int) -> Unit,
    onSelectPreset: (BeatPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C101A))
            .border(1.dp, Color(0xFF1A2436), RoundedCornerShape(6.dp))
            .padding(3.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main Grid: 4 slots on left side, 4 slots on right side
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left Side: 4 slots (indices 0..3)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 0..3) {
                    val preset = styles.getOrNull(i)
                    if (preset != null) {
                        StyleTile(
                            preset = preset,
                            isSelected = isPresetActive(preset, activePresetName),
                            onClick = { onSelectPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        EmptyStyleTile(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Right Side: 4 slots (indices 4..7)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 4..7) {
                    val preset = styles.getOrNull(i)
                    if (preset != null) {
                        StyleTile(
                            preset = preset,
                            isSelected = isPresetActive(preset, activePresetName),
                            onClick = { onSelectPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        EmptyStyleTile(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Pagination row: P1, P2, P3...
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (p in 0 until totalPages) {
                val isCurrent = p == currentPage
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCurrent) Color(0xFFFFB03A) else Color(0xFF161F2E))
                        .clickable { onSelectPage(p) }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("P${p + 1}", color = if (isCurrent) Color.Black else Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(3.dp))
            }
        }
    }
}

@Composable
fun StyleTile(
    preset: BeatPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(preset.accentColorHex)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF241F12) else Color(0xFF121724))
            .border(if (isSelected) 1.5.dp else 0.8.dp, if (isSelected) Color(0xFFFFB03A) else Color(0xFF1E2838), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            GenreBadgeIcon(genre = preset.genre, accentColor = accent, modifier = Modifier.size(18.dp))
            Column {
                Text(
                    text = preset.name,
                    color = if (isSelected) Color(0xFFFFB03A) else Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${preset.bpm.toInt()} BPM • ${preset.timeSigNum}/${preset.timeSigDen}",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStyleTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF090D16))
            .border(0.5.dp, Color(0xFF141C2B), RoundedCornerShape(4.dp))
    )
}

private fun isPresetActive(preset: BeatPreset, activeName: String): Boolean {
    if (preset.name.equals(activeName, ignoreCase = true) || preset.id.equals(activeName, ignoreCase = true)) return true
    val c1 = preset.name.replace("_", " ").trim()
    val c2 = activeName.replace("_", " ").trim()
    return c1.equals(c2, ignoreCase = true)
}

