package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SleepTimerDialog(
    currentRemainingMs: Long,
    stopAfterCurrentEnabled: Boolean,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onToggleStopAfterCurrent: () -> Unit
) {
    val deepEspresso = Color(0xFF1E1814)
    val darkMocha = Color(0xFF2A211C)
    val coffeeBrown = Color(0xFFB08968)
    val softLatte = Color(0xFFDDB892)
    val warmCream = Color(0xFFF8F4F0)
    val secondaryText = Color(0xFFCBB9A8)

    var customInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = darkMocha,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sleep Timer",
                    color = warmCream,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Current Active Timer Indicator
                if (currentRemainingMs > 0) {
                    val remainingSeconds = currentRemainingMs / 1000
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)
                    
                    Text(
                        text = "Active countdown: $timeString",
                        color = softLatte,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(15, 30, 45, 60)
                    presets.forEach { mins ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(deepEspresso)
                                .clickable {
                                    onSetTimer(mins)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                color = softLatte,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Input Field
                OutlinedTextField(
                    value = customInput,
                    onValueChange = {
                        customInput = it
                        showError = false
                    },
                    label = { Text("Custom minutes", color = secondaryText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = coffeeBrown,
                        unfocusedBorderColor = deepEspresso,
                        focusedLabelColor = softLatte,
                        cursorColor = coffeeBrown,
                        focusedTextColor = warmCream,
                        unfocusedTextColor = warmCream
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (showError) {
                    Text(
                        text = "Please enter a valid number of minutes",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stop After Current Song Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(deepEspresso)
                        .clickable { onToggleStopAfterCurrent() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stop after current song",
                            color = warmCream,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ends play when this track completes",
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = stopAfterCurrentEnabled,
                        onCheckedChange = { onToggleStopAfterCurrent() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = warmCream,
                            checkedTrackColor = coffeeBrown,
                            uncheckedThumbColor = secondaryText,
                            uncheckedTrackColor = deepEspresso
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onSetTimer(0) // Cancel active timer
                        onDismiss()
                    }) {
                        Text("Cancel timer", color = secondaryText)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val mins = customInput.toIntOrNull()
                            if (mins != null && mins > 0) {
                                onSetTimer(mins)
                                onDismiss()
                            } else {
                                showError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = coffeeBrown),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", color = deepEspresso, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
