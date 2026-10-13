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
fun LcdTimeSigView(
    currentNum: Int,
    currentDen: Int,
    onSelectTimeSig: (Int, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        Pair(4, 4),
        Pair(3, 4),
        Pair(6, 8),
        Pair(7, 8),
        Pair(5, 8)
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E2838))
                    .clickable { onBack() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("← BACK", color = Color(0xFFFFB03A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Text("SELECT TIME SIGNATURE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.width(30.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.chunked(3).forEach { columnPresets ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    columnPresets.forEach { (n, d) ->
                        val isSelected = n == currentNum && d == currentDen
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF1E3A5F) else Color(0xFF0F1524))
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E2B40), RoundedCornerShape(6.dp))
                            .clickable { onSelectTimeSig(n, d) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$n/$d",
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
