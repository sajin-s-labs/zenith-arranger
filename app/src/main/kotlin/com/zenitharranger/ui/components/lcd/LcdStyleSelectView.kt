package com.zenitharranger.ui.components.lcd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.model.*
import com.zenitharranger.ui.components.GenreBadgeIcon

@Composable
fun LcdStyleSelectView(
    presets: List<BeatPreset>,
    activePresetName: String,
    onSelectPreset: (BeatPreset) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allPresets = if (presets.isEmpty()) BeatPresetManager.getFactoryPresets() else presets
    val activePreset = remember(allPresets, activePresetName) {
        val clean = activePresetName.replace("_", " ").trim()
        allPresets.firstOrNull {
            it.name.equals(activePresetName, true) || it.id.equals(activePresetName, true) ||
            it.name.replace("_", " ").trim().equals(clean, true)
        }
    }
    val initialIsUser = activePreset?.category?.equals("User", ignoreCase = true) == true
    val initialFolder = activePreset?.let { getStyleFolder(it) } ?: (if (initialIsUser) "My_Beats" else "Pop_&_Rock")

    var isUserSection by remember(activePresetName) { mutableStateOf(initialIsUser) }
    var selectedFolder by remember(activePresetName) { mutableStateOf(initialFolder) }
    var currentPage by remember(activePresetName) { mutableIntStateOf(0) }
    var customUserFolders by remember { mutableStateOf(listOf<String>()) }
    var showAddFolderPrompt by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val factoryFolders = listOf("Pop_&_Rock", "Ballad_&_Waltz", "World_Dance", "Blues_&_Jazz")
    val userFolders = remember(allPresets, customUserFolders) {
        val presetFolders = allPresets.filter { it.category.equals("User", ignoreCase = true) }
            .map { it.folder }.filter { it.isNotBlank() }
        (listOf("My_Beats") + customUserFolders + presetFolders).distinct()
    }
    val currentFolders = if (isUserSection) userFolders else factoryFolders

    val folderStyles = remember(allPresets, isUserSection, selectedFolder) {
        allPresets.filter { p ->
            val matchesSection = if (isUserSection) p.category.equals("User", ignoreCase = true)
                else p.category.equals("Factory", ignoreCase = true)
            matchesSection && getStyleFolder(p) == selectedFolder
        }
    }

    val pageSize = 8
    val totalPages = maxOf(1, (folderStyles.size + pageSize - 1) / pageSize)
    val pageItems = folderStyles.drop(currentPage * pageSize).take(pageSize)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Sub-Header: Back + Factory/User Toggle + Add Folder
        StyleSelectHeader(
            isUserSection = isUserSection,
            onToggleSection = { isUser ->
                isUserSection = isUser
                selectedFolder = if (isUser) (userFolders.firstOrNull() ?: "My_Beats") else factoryFolders.first()
                currentPage = 0
            },
            onBack = onBack,
            onAddFolder = { showAddFolderPrompt = true }
        )

        // Main 2-Column Table (Left: Folders | Right: Styles Grid + Pagination)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FolderListColumn(
                folders = currentFolders,
                selectedFolder = selectedFolder,
                onSelectFolder = { selectedFolder = it; currentPage = 0 },
                modifier = Modifier.width(105.dp).fillMaxHeight()
            )

            StyleGridColumn(
                styles = pageItems,
                activePresetName = activePresetName,
                currentPage = currentPage,
                totalPages = totalPages,
                onSelectPage = { currentPage = it },
                onSelectPreset = onSelectPreset,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }

    if (showAddFolderPrompt) {
        AlertDialog(
            onDismissRequest = { showAddFolderPrompt = false },
            title = { Text("New Folder", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        val clean = BeatPresetManager.sanitizeName(newFolderName)
                        customUserFolders = customUserFolders + clean
                        selectedFolder = clean
                        newFolderName = ""
                        showAddFolderPrompt = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddFolderPrompt = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StyleSelectHeader(
    isUserSection: Boolean,
    onToggleSection: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAddFolder: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
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
            Text("← BACK", color = Color(0xFFFFB03A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TabBadge("FACTORY", isSelected = !isUserSection) { onToggleSection(false) }
            TabBadge("USER", isSelected = isUserSection) { onToggleSection(true) }
        }

        if (isUserSection) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF162520))
                    .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .clickable { onAddFolder() }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("+ FOLDER", color = Color(0xFF34D399), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
private fun TabBadge(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) Color(0xFF25334D) else Color(0xFF101724))
            .border(1.dp, if (isSelected) Color(0xFFFFB03A) else Color(0xFF1C2738), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = if (isSelected) Color(0xFFFFB03A) else Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FolderListColumn(
    folders: List<String>,
    selectedFolder: String,
    onSelectFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C101A))
            .border(1.dp, Color(0xFF1A2436), RoundedCornerShape(6.dp))
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(folders) { folder ->
            val isSel = folder == selectedFolder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSel) Color(0xFF1E2F48) else Color(0xFF111724))
                    .border(1.dp, if (isSel) Color(0xFF38BDF8) else Color.Transparent, RoundedCornerShape(4.dp))
                    .clickable { onSelectFolder(folder) }
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                Text(
                    text = folder.replace('_', ' '),
                    color = if (isSel) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    fontSize = 8.sp,
                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getStyleFolder(preset: BeatPreset): String {
    if (preset.folder.isNotBlank()) return BeatPresetManager.sanitizeName(preset.folder)
    return when {
        preset.id.contains("pop", true) || preset.name.contains("Pop", true) -> "Pop_&_Rock"
        preset.id.contains("waltz", true) || preset.name.contains("Waltz", true) -> "Ballad_&_Waltz"
        preset.id.contains("balkan", true) || preset.name.contains("Balkan", true) -> "World_Dance"
        preset.id.contains("blues", true) || preset.name.contains("Blues", true) -> "Blues_&_Jazz"
        else -> "My_Beats"
    }
}

