package com.zenitharranger.ui.components.stylecreator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun StyleCreatorHeader(
    styleTitle: String,
    onTitleChange: (String) -> Unit,
    folder: String,
    onFolderChange: (String) -> Unit,
    availableFolders: List<String>,
    timeSigNum: Int,
    timeSigDen: Int,
    onSetTimeSig: (Int, Int) -> Unit,
    onSaveStyle: () -> Unit,
    savedMessage: String?,
    modifier: Modifier = Modifier
) {
    var showFolderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1018))
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(6.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.5.dp)
    ) {
        HeaderLineOne(styleTitle, onTitleChange, folder, { showFolderDialog = true }, onSaveStyle, savedMessage)
        HeaderLineTwo(timeSigNum, timeSigDen, onSetTimeSig)
    }

    if (showFolderDialog) {
        FolderChoiceDialog(availableFolders, folder, { showFolderDialog = false }) {
            onFolderChange(it)
            showFolderDialog = false
        }
    }
}

@Composable
private fun HeaderLineOne(
    title: String, onTitleChange: (String) -> Unit, folder: String,
    onOpenFolder: () -> Unit, onSave: () -> Unit, savedMsg: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("NAME:", color = Color(0xFF64748B), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF141C2A)).border(0.8.dp, Color(0xFF263750), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = title, onValueChange = onTitleChange, singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    textStyle = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
            FolderBadgeChip(folder, onOpenFolder)
        }
        Spacer(Modifier.width(4.dp))
        DirectSaveButton(onSave, savedMsg)
    }
}

@Composable
private fun FolderBadgeChip(folder: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF162132))
            .border(0.8.dp, Color(0xFF273852), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        Text("📁 /User/$folder", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("▾", color = Color(0xFF64748B), fontSize = 8.sp)
    }
}

@Composable
private fun DirectSaveButton(onSave: () -> Unit, savedMsg: String?) {
    val isSaved = savedMsg != null
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(3.5.dp))
            .background(if (isSaved) Color(0xFF059669) else Color(0xFF10B981).copy(alpha = 0.25f))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(3.5.dp))
            .clickable { onSave() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val label = if (isSaved) "✔ $savedMsg" else "💾 SAVE STYLE"
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeaderLineTwo(num: Int, den: Int, onSetTimeSig: (Int, Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TIME SIG:", color = Color(0xFF64748B), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        listOf(Pair(4, 4), Pair(3, 4), Pair(6, 8), Pair(7, 8), Pair(5, 8)).forEach { (n, d) ->
            val isSel = num == n && den == d
            TimeSigChip("$n/$d", isSel) { onSetTimeSig(n, d) }
        }
    }
}

@Composable
private fun TimeSigChip(text: String, isSel: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSel) Color(0xFF1E3A5F) else Color(0xFF131924))
            .border(0.8.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF222C3E), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSel) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FolderChoiceDialog(folders: List<String>, current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var customText by remember { mutableStateOf("") }
    var isNewMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0E1420)).border(1.dp, Color(0xFF24334C), RoundedCornerShape(10.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Select /User Folder", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            folders.forEach { f ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if (f == current && !isNewMode) Color(0xFF1B2E48) else Color(0xFF141C28)).clickable { onSelect(f) }.padding(8.dp)
                ) {
                    Text("📁 $f", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!isNewMode) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2230)).clickable { isNewMode = true }.padding(8.dp)) {
                    Text("+ New Folder...", color = Color(0xFFFFB03A), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF141C2A)).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(value = customText, onValueChange = { customText = it }, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 11.sp))
                    }
                    Box(modifier = Modifier.height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF10B981)).clickable { if (customText.isNotBlank()) onSelect(customText.trim()) }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Add", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
