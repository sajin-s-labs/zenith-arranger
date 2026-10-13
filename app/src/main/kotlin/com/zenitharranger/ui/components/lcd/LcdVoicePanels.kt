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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RightVoicesRow(
    onOpenVoiceBrowser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1524))
            .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(6.dp))
            .clickable { onOpenVoiceBrowser() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RIGHT VOICES", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text("R1 • R2 • R3", color = Color(0xFF38BDF8), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        VoiceChip(label = "R1", name = "ConcertGrand", color = Color(0xFF38BDF8), active = true)
        VoiceChip(label = "R2", name = "WarmStrings", color = Color(0xFF818CF8), active = false)
        VoiceChip(label = "R3", name = "SynthLead", color = Color(0xFFF472B6), active = false)
    }
}

@Composable
fun LeftVoicesRow(
    onOpenVoiceBrowser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1524))
            .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(6.dp))
            .clickable { onOpenVoiceBrowser() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LEFT VOICES", color = Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text("L1 • L2", color = Color(0xFF34D399), fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        VoiceChip(label = "L1", name = "FingerBass", color = Color(0xFF34D399), active = true)
        VoiceChip(label = "L2", name = "SoftPad", color = Color(0xFFFBBF24), active = false)
    }
}

@Composable
fun VoiceChip(label: String, name: String, color: Color, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) color.copy(alpha = 0.25f) else Color(0xFF161F2E))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(label, color = if (active) color else Color(0xFF64748B), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            Text(name, color = if (active) Color.White else Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(1.dp)).background(if (active) color else Color(0xFF1E293B)))
    }
}
