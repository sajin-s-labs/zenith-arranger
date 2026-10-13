package com.zenitharranger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zenitharranger.core.audio.ArrangerState
import com.zenitharranger.model.BeatPreset
import com.zenitharranger.ui.components.lcd.*

/**
 * Yamaha-inspired 360dp x 240dp Workstation LCD Touchscreen Display.
 * Fixed top brand header and bottom bar (measure + glowing LEDs) remain permanently visible,
 * while the middle workspace transitions smoothly into in-screen style/voice/meter pickers.
 */
@Composable
fun LcdDisplayBar(
    styleName: String,
    styleGenre: String,
    accentColor: Color,
    state: ArrangerState,
    activeSectionName: String,
    queuedSectionName: String?,
    presets: List<BeatPreset>,
    onSelectPreset: (BeatPreset) -> Unit,
    onSelectTimeSig: (Int, Int) -> Unit,
    onSaveBeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(LcdDisplayMode.HOME) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151C2A))
            .border(1.5.dp, Color(0xFF223149), RoundedCornerShape(12.dp))
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0C1220), Color(0xFF060911))))
                .border(1.dp, Color(0xFF182436), RoundedCornerShape(10.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Permanent Top Branding Bar (Always Visible)
            BrandingTopBar()

            // 2. Middle Content Area (Switches in-place without hiding top/bottom bars)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                when (mode) {
                    LcdDisplayMode.HOME -> {
                        LcdHomeColumns(
                            styleName = styleName,
                            styleGenre = styleGenre,
                            accentColor = accentColor,
                            state = state,
                            onOpenStyleSelect = { mode = LcdDisplayMode.STYLE_SELECT },
                            onSaveBeat = onSaveBeat
                        )
                    }
                    LcdDisplayMode.STYLE_SELECT -> {
                        LcdStyleSelectView(
                            presets = presets,
                            activePresetName = styleName,
                            onSelectPreset = { preset ->
                                onSelectPreset(preset)
                                mode = LcdDisplayMode.HOME
                            },
                            onBack = { mode = LcdDisplayMode.HOME }
                        )
                    }
                    LcdDisplayMode.TIME_SIG -> {
                        LcdTimeSigView(
                            currentNum = state.timeSigNum,
                            currentDen = state.timeSigDen,
                            onSelectTimeSig = { n, d ->
                                onSelectTimeSig(n, d)
                                mode = LcdDisplayMode.HOME
                            },
                            onBack = { mode = LcdDisplayMode.HOME }
                        )
                    }
                }
            }

            // 3. Permanent Bottom Bar: Measure, Status & Sharp Glowing LEDs (Always Visible)
            LcdBottomBar(
                state = state,
                activeSectionName = activeSectionName,
                queuedSectionName = queuedSectionName
            )
        }
    }
}
