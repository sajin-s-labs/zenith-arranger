package com.zenitharranger.model

data class BeatPreset(
    val id: String,
    val name: String,
    val bpm: Float,
    val timeSigNum: Int,
    val timeSigDen: Int,
    val sections: List<ArrangerSection>,
    val folder: String = "Pop & Rock",
    val category: String = "Factory",
    val createdAt: Long = System.currentTimeMillis()
)

val BeatPreset.genre: String
    get() = if (folder.isNotBlank()) folder.uppercase() else when {
        id.contains("pop") || name.contains("Pop", ignoreCase = true) -> "POP & ROCK"
        id.contains("waltz") || name.contains("Waltz", ignoreCase = true) -> "BALLAD & WALTZ"
        id.contains("balkan") || name.contains("Balkan", ignoreCase = true) -> "WORLD DANCE"
        id.contains("blues") || name.contains("Blues", ignoreCase = true) -> "BLUES & JAZZ"
        else -> "USER STYLE"
    }

val BeatPreset.accentColorHex: Long
    get() = when {
        genre.contains("POP") -> 0xFFFF7043
        genre.contains("BALLAD") || genre.contains("WALTZ") -> 0xFF818CF8
        genre.contains("DANCE") || genre.contains("WORLD") -> 0xFF34D399
        genre.contains("BLUES") || genre.contains("JAZZ") -> 0xFF38BDF8
        else -> 0xFFFFB03A
    }
