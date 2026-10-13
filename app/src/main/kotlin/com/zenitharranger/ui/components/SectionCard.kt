package com.zenitharranger.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.model.ArrangerSection
import com.zenitharranger.model.ArrangerSectionType

/**
 * 1:1 Square tactile performance pad for triggering and managing rhythm sections.
 */
@Composable
fun SectionCard(
    section: ArrangerSection,
    isActive: Boolean,
    isQueued: Boolean,
    onClick: () -> Unit,
    onAssignSample: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor = when {
        isActive -> Color(0xFFFFB03A)
        isQueued -> Color(0xFFFF7043).copy(alpha = pulseAlpha)
        else -> Color(0x22FFFFFF)
    }

    val bgColor = when {
        isActive -> Color(0xFF2E2211)
        isQueued -> Color(0xFF38190F).copy(alpha = pulseAlpha * 0.7f + 0.3f)
        else -> Color(0xFF13161F)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawBehind {
                if (isActive) {
                    drawRoundRect(
                        color = Color(0x44FFB03A),
                        size = size.copy(width = size.width + 6.dp.toPx(), height = size.height + 6.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx(), -3.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                    )
                } else if (isQueued) {
                    drawRoundRect(
                        color = Color(0xFFFF7043).copy(alpha = pulseAlpha * 0.4f),
                        size = size.copy(width = size.width + 6.dp.toPx(), height = size.height + 6.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx(), -3.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                    )
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(if (isActive || isQueued) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Section Name & Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.name,
                    color = if (isActive) Color(0xFFFFB03A) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                SectionTypeBadge(type = section.type)
            }

            // Info Line: Bar Count & Audio Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${section.bars} Bar${if (section.bars > 1) "s" else ""}",
                    color = Color(0xFF8E95A5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (section.sampleName.isNotEmpty()) "WAV" else "SYNTH",
                    color = if (section.sampleName.isNotEmpty()) Color(0xFF34D399) else Color(0xFF6B7280),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Footer Row: Sample Replace and Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222634))
                            .clickable { onAssignSample() }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (section.samplePath.isNotEmpty()) "REPLACE" else "+ WAV",
                            color = Color(0xFF93C5FD),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x22EF4444))
                            .clickable { onDelete() }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("✕", color = Color(0xFFFCA5A5), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 1:1 Square Pad specifically for adding a new section directly in the grid.
 */
@Composable
fun AddSectionPad(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F121A))
            .border(1.dp, Color(0x33FFB03A), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("+", color = Color(0xFFFFB03A), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("ADD", color = Color(0xFFFFB03A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTypeBadge(type: ArrangerSectionType) {
    val (bgColor, textColor) = when (type) {
        ArrangerSectionType.MAIN -> Color(0x3360A5FA) to Color(0xFF93C5FD)
        ArrangerSectionType.FILL -> Color(0x33F472B6) to Color(0xFFF472B6)
        ArrangerSectionType.INTRO -> Color(0x33A78BFA) to Color(0xFFA78BFA)
        ArrangerSectionType.ENDING -> Color(0x33FB923C) to Color(0xFFFB923C)
        ArrangerSectionType.BREAK -> Color(0x33FCD34D) to Color(0xFFFCD34D)
        ArrangerSectionType.CUSTOM -> Color(0x3334D399) to Color(0xFF34D399)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(type.label, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
