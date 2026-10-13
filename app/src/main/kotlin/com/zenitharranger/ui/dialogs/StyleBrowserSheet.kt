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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zenitharranger.model.*

@Composable
fun StyleBrowserSheet(
    presets: List<BeatPreset>,
    activePresetId: String,
    onDismiss: () -> Unit,
    onLoad: (BeatPreset) -> Unit,
    onDelete: (BeatPreset) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0C101A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223048))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BrowserHeader(selectedCategory, selectedSubfolder, onDismiss, { selectedCategory = null; selectedSubfolder = null }, { selectedSubfolder = null })

                when {
                    selectedCategory == null -> CategoryRootList { selectedCategory = it }
                    selectedSubfolder == null -> SubfolderList(context, selectedCategory!!) { selectedSubfolder = it }
                    else -> PresetFileList(context, selectedCategory!!, selectedSubfolder!!, activePresetId, { onLoad(it); onDismiss() }, onDelete)
                }
            }
        }
    }
}

@Composable
private fun BrowserHeader(
    cat: String?, sub: String?, onDismiss: () -> Unit, onResetCat: () -> Unit, onResetSub: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("STYLE EXPLORER", color = Color(0xFFFFB03A), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            val path = when {
                cat != null && sub != null -> "📁 Styles > $cat > $sub"
                cat != null -> "📁 Styles > $cat"
                else -> "📁 Styles"
            }
            Text(path, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (sub != null) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E2838)).clickable { onResetSub() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("⬅ Up", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else if (cat != null) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E2838)).clickable { onResetCat() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("⬅ Root", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E2838)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryRootList(onSelectCat: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FolderEntryCard("Factory", "Built-in Arranger Accompaniment Styles", Color(0xFF38BDF8)) { onSelectCat("Factory") }
        FolderEntryCard("User", "Saved Beats & Custom Author Styles", Color(0xFF10B981)) { onSelectCat("User") }
    }
}

@Composable
private fun SubfolderList(context: android.content.Context, category: String, onSelectSub: (String) -> Unit) {
    val subfolders = remember(category) { BeatPresetManager.listSubfolders(context, category) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(subfolders) { sub ->
            FolderEntryCard(sub, "Folder in $category", Color(0xFFFFB03A)) { onSelectSub(sub) }
        }
    }
}

@Composable
private fun FolderEntryCard(name: String, desc: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF121724))
            .border(1.dp, Color(0xFF1E273A), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text("📁", fontSize = 18.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color(0xFF64748B), fontSize = 9.5.sp)
            }
        }
        Text("▶", color = Color(0xFF64748B), fontSize = 12.sp)
    }
}

@Composable
private fun PresetFileList(
    context: android.content.Context, category: String, subfolder: String,
    activeId: String, onLoad: (BeatPreset) -> Unit, onDelete: (BeatPreset) -> Unit
) {
    val presets = remember(category, subfolder) { BeatPresetManager.listPresetsInSubfolder(context, category, subfolder) }
    if (presets.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No style files in this folder yet.", color = Color(0xFF64748B), fontSize = 11.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(presets, key = { it.id }) { preset ->
                PresetFileCard(preset, preset.id == activeId || preset.name == activeId, { onLoad(preset) }, { onDelete(preset) })
            }
        }
    }
}

@Composable
private fun PresetFileCard(preset: BeatPreset, isActive: Boolean, onLoad: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFF1A233A) else Color(0xFF121724))
            .border(1.dp, if (isActive) Color(0xFFFFB03A) else Color(0xFF1E273A), RoundedCornerShape(8.dp))
            .clickable { onLoad() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(preset.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${preset.bpm.toInt()} BPM", color = Color(0xFF94A3B8), fontSize = 9.sp)
                Text("•", color = Color(0xFF475569), fontSize = 9.sp)
                Text("${preset.timeSigNum}/${preset.timeSigDen}", color = Color(0xFF38BDF8), fontSize = 9.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onLoad, shape = RoundedCornerShape(5.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Color(0xFFFFB03A) else Color(0xFF223048)),
                modifier = Modifier.height(26.dp)
            ) {
                Text(if (isActive) "ACTIVE" else "LOAD", color = if (isActive) Color.Black else Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
            if (preset.category == "User") {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x33EF4444)).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                    Text("✕", color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
