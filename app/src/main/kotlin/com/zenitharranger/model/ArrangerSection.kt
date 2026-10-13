package com.zenitharranger.model

enum class ArrangerSectionType(val rawValue: Int, val label: String) {
    MAIN(0, "Main"),
    FILL(1, "Fill"),
    INTRO(2, "Intro"),
    ENDING(3, "Ending"),
    BREAK(4, "Break"),
    CUSTOM(5, "Custom");

    companion object {
        fun fromRaw(raw: Int): ArrangerSectionType =
            entries.firstOrNull { it.rawValue == raw } ?: MAIN
    }
}

data class ArrangerSection(
    val id: String,
    val name: String,
    val type: ArrangerSectionType,
    val bars: Int = 2,
    val samplePath: String = "",
    val sampleName: String = "",
    val sampleBpm: Float = 120f
)
