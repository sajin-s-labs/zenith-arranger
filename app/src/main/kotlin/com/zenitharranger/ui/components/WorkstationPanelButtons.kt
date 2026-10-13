package com.zenitharranger.ui.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 6 Master Workstation Panel Buttons:
 * MAIN, VOLUMES, EQ, STYLE CREATOR, VOICE CREATOR, SETTINGS.
 */
val WORKSTATION_PANELS = listOf(
    "MAIN",
    "VOLUMES",
    "EQ",
    "STYLE CREATOR",
    "VOICE CREATOR",
    "SETTINGS"
)

val PANEL_COMPACT_LABELS = listOf(
    "MAIN",
    "VOLUMES",
    "EQ",
    "STYLE",
    "VOICE",
    "SETTINGS"
)

@Composable
fun HorizontalPanelButtonsBar(
    selectedPanel: String,
    onSelectPanel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F1522))
            .border(0.8.dp, Color(0xFF1E2A3E), RoundedCornerShape(4.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WORKSTATION_PANELS.forEachIndexed { index, panel ->
            val isSelected = panel == selectedPanel
            val label = PANEL_COMPACT_LABELS.getOrElse(index) { panel }
            PanelButton(
                title = label,
                isSelected = isSelected,
                onClick = { onSelectPanel(panel) },
                fontSize = 8.sp,
                modifier = Modifier.weight(1f).height(20.dp)
            )
        }
    }
}

val PANEL_SHORT_LABELS = listOf(
    "MAIN",
    "VOL",
    "EQ",
    "STYLE",
    "VOICE",
    "SET"
)

@Composable
fun VerticalPanelButtonsStrip(
    selectedPanel: String,
    onSelectPanel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F1522))
            .border(1.dp, Color(0xFF1E2A3E), RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WORKSTATION_PANELS.forEachIndexed { index, panel ->
            val isSelected = panel == selectedPanel
            val shortLabel = PANEL_SHORT_LABELS.getOrElse(index) { panel }
            PanelButton(
                title = shortLabel,
                isSelected = isSelected,
                onClick = { onSelectPanel(panel) },
                fontSize = 9.sp,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PanelButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 8.sp
) {
    val bgColor = if (isSelected) Color(0xFF1E2C44) else Color(0xFF131A28)
    val borderColor = if (isSelected) Color(0xFFFFB03A) else Color(0xFF1E2B3E)
    val textColor = if (isSelected) Color(0xFFFFB03A) else Color(0xFF8E9BAE)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(if (isSelected) 1.2.dp else 0.6.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = fontSize,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
