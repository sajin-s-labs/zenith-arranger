package com.zenitharranger.model

data class StyleCreatorState(
    val title: String = "User Style",
    val folder: String = "My_Beats",
    val bpm: Float = 120f,
    val timeSigNum: Int = 4,
    val timeSigDen: Int = 4,
    val sections: List<ArrangerSection> = emptyList(),
    val isPlaying: Boolean = false,
    val activeSlot: String = "",
    val isLoadMode: Boolean = false,
    val isClearMode: Boolean = false
)
