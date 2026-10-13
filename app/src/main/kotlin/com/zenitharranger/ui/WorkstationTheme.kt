package com.zenitharranger.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

object WorkstationTheme {
    // Chassis & Surfaces (Matte Plastic Synth Black & Aluminum)
    val ChassisBg = Color(0xFF07080B)
    val PanelBg = Color(0xFF0F1118)
    val PanelBorder = Color(0xFF1C202C)
    val MetallicTrim = Color(0xFF2C3242)
    val MetallicHighlight = Color(0xFF4A546E)

    // Rubber Buttons (Tactile Synth Pads)
    val RubberIdleBg = Color(0xFF141720)
    val RubberIdleBorder = Color(0xFF222836)
    val RubberIdleText = Color(0xFFA6AFBF)
    val RubberDimmed = Color(0xFF0B0D12)

    val RubberBtnIdle = RubberIdleBg
    val RubberBtnBorder = RubberIdleBorder
    val RubberBtnText = RubberIdleText
    val RubberBtnDimmed = RubberDimmed

    // Glowing Illuminated LED Lights (True Hardware Glow)
    val LedAmber = Color(0xFFFFB03A)
    val LedAmberActive = LedAmber
    val LedAmberDim = Color(0x66FFB03A)
    val LedAmberHalo = Color(0x33FFB03A)
    val LedAmberBg = Color(0xFF2D1E08)
    val LedAmberFill = Brush.verticalGradient(listOf(Color(0xFF382408), Color(0xFF1E1304)))

    val LedOrange = Color(0xFFFF7043)
    val LedOrangeArmed = LedOrange
    val LedOrangeHalo = Color(0x40FF7043)
    val LedOrangeFill = Brush.verticalGradient(listOf(Color(0xFF3D1A0E), Color(0xFF200D06)))

    val LedGreen = Color(0xFF34D399)
    val LedCyan = Color(0xFF38BDF8)
    val LcdCyan = LedCyan
    val LedRed = Color(0xFFEF4444)
    val LedRedArmed = LedRed

    // Digital LCD Glass (Embedded High-Contrast Screen)
    val LcdGlassBg = Color(0xFF090D14)
    val LcdCardBg = Color(0xFF0E131E)
    val LcdBorder = Color(0xFF1A2332)
    val LcdTextPrimary = Color(0xFFF1F5F9)
    val LcdTextMuted = Color(0xFF64748B)
    val LcdMeterIdle = Color(0xFF141C28)

    // Piano Keys (Physically Proportioned Keybed)
    val KeyIvory = Color(0xFFECEFF3)
    val KeyIvoryPressed = Color(0xFFFFB03A)
    val KeyIvoryBorder = Color(0xFF7E8896)
    val KeyIvoryLip = Color(0xFFCBD2DC)

    val KeyEbony = Color(0xFF12141A)
    val KeyEbonyPressed = Color(0xFFFF7043)
    val KeyEbonyBorder = Color(0xFF282D3B)

    // Glowing Text Shadows
    val AmberTextGlow = Shadow(color = LedAmber, offset = Offset.Zero, blurRadius = 8f)
    val OrangeTextGlow = Shadow(color = LedOrange, offset = Offset.Zero, blurRadius = 8f)
}
