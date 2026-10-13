package com.zenitharranger.ui.dialogs

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.model.BeatPreset

/**
 * Dialog for naming and saving the current arranger setup as a beat preset.
 */
@Composable
fun SaveBeatDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Beat Preset", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Beat Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty()) onConfirm(name) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Dialog for browsing, auditioning, and deleting saved beat presets.
 */
@Composable
fun BeatLibraryDialog(
    presets: List<BeatPreset>,
    onDismiss: () -> Unit,
    onLoad: (BeatPreset) -> Unit,
    onDelete: (BeatPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved Beats Library", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { p ->
                    PresetListItem(preset = p, onLoad = onLoad, onDelete = onDelete)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun PresetListItem(
    preset: BeatPreset,
    onLoad: (BeatPreset) -> Unit,
    onDelete: (BeatPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E212B))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "${preset.bpm.toInt()} BPM • ${preset.timeSigNum}/${preset.timeSigDen} • ${preset.sections.size} sections",
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { onLoad(preset) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Load", fontSize = 11.sp)
            }
            if (!preset.id.startsWith("factory_")) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33EF4444))
                        .clickable { onDelete(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
