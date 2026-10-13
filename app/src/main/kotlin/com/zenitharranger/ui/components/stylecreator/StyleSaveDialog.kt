package com.zenitharranger.ui.components.stylecreator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun StyleSaveDialog(
    currentName: String,
    availableFolders: List<String>,
    currentFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, folder: String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var selectedFolder by remember { mutableStateOf(currentFolder) }
    var customFolderText by remember { mutableStateOf("") }
    var isNewFolderMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1420))
                .border(1.2.dp, Color(0xFF26334A), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DialogHeader()
            StyleNameInput(nameText) { nameText = it }
            FolderSelector(availableFolders, selectedFolder, isNewFolderMode, customFolderText, { selectedFolder = it }, { isNewFolderMode = it }, { customFolderText = it })
            DialogActionButtons(onDismiss) {
                val folder = if (isNewFolderMode && customFolderText.isNotBlank()) customFolderText.trim() else selectedFolder
                onConfirm(nameText.trim().ifEmpty { "User Style" }, folder)
            }
        }
    }
}

@Composable
private fun DialogHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981)))
        Text("SAVE USER STYLE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StyleNameInput(name: String, onNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Style Title", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE2E8F0),
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF243044)
            )
        )
    }
}

@Composable
private fun FolderSelector(
    folders: List<String>, selected: String, isNew: Boolean, customFolder: String,
    onSelectFolder: (String) -> Unit, onToggleNew: (Boolean) -> Unit, onCustomChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Target Folder in /User", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            folders.take(4).forEach { f ->
                FolderChip(f, !isNew && selected == f) { onToggleNew(false); onSelectFolder(f) }
            }
            FolderChip("+ New", isNew) { onToggleNew(true) }
        }
        if (isNew) {
            OutlinedTextField(
                value = customFolder,
                onValueChange = onCustomChange,
                placeholder = { Text("New folder name...", color = Color(0xFF64748B), fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFB03A),
                    unfocusedBorderColor = Color(0xFF243044)
                )
            )
        }
    }
}

@Composable
private fun FolderChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF1E2E48) else Color(0xFF131926))
            .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E2838), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DialogActionButtons(onDismiss: () -> Unit, onSave: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDismiss) {
            Text("Cancel", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text("SAVE STYLE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
