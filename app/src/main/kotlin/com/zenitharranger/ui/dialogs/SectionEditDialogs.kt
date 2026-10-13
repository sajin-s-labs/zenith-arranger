package com.zenitharranger.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenitharranger.model.ArrangerSectionType

/**
 * Dialog for configuring the master musical time signature.
 */
@Composable
fun TimeSignatureDialog(
    currentNum: Int,
    currentDen: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time Signature", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supported Workstation Signatures:", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4 to 4, 3 to 4, 6 to 8, 7 to 8, 5 to 8).forEach { (n, d) ->
                        val isSelected = (currentNum == n && currentDen == d)
                        Button(
                            onClick = { onConfirm(n, d) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF222633),
                                contentColor = if (isSelected) Color(0xFF38BDF8) else Color.White
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("$n/$d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/**
 * Dialog for creating a new custom arranger section.
 */
@Composable
fun AddSectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ArrangerSectionType, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ArrangerSectionType.MAIN) }
    var barsText by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Section", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section Name (e.g. Chorus, Bridge)") },
                    singleLine = true
                )
                Text("Role / Behavior:", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ArrangerSectionType.entries.forEach { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFFFFB03A) else Color(0xFF222633))
                                .clickable {
                                    selectedType = t
                                    if (t == ArrangerSectionType.FILL) barsText = "1"
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                t.label,
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = barsText,
                    onValueChange = { barsText = it },
                    label = { Text("Length in Bars") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalName = name.ifEmpty { selectedType.label }
                val bars = barsText.toIntOrNull() ?: 2
                onConfirm(finalName, selectedType, bars)
            }) { Text("Add Section") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Dialog for configuring sample metadata when loading a WAV loop into a section.
 */
@Composable
fun SampleAssignDialog(
    sectionName: String,
    fileName: String,
    initialBpm: Float,
    initialBars: Int,
    onDismiss: () -> Unit,
    onConfirm: (Float, Int) -> Unit
) {
    var bpmText by remember { mutableStateOf(initialBpm.toInt().toString()) }
    var barsText by remember { mutableStateOf(initialBars.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign WAV: $sectionName", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File: $fileName", color = Color.Gray, fontSize = 12.sp)
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it },
                    label = { Text("Sample BPM") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = barsText,
                    onValueChange = { barsText = it },
                    label = { Text("Bars in Sample") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val b = bpmText.toFloatOrNull() ?: initialBpm
                val bars = barsText.toIntOrNull() ?: initialBars
                onConfirm(b, bars)
            }) { Text("Assign") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
