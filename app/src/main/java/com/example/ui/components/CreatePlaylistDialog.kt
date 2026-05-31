package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val appColors = com.example.ui.theme.LocalAppColors.current
    val darkMocha = appColors.darkMocha
    val deepEspresso = appColors.deepEspresso
    val coffeeBrown = appColors.coffeeBrown
    val softLatte = appColors.softLatte
    val warmCream = appColors.warmCream
    val secondaryText = appColors.secondaryText

    var playlistName by remember { mutableStateOf("") }
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
                    text = "New Playlist",
                    color = warmCream,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                        showError = false
                    },
                    label = { Text("Playlist Name", color = secondaryText) },
                    placeholder = { Text("e.g., Chill Lounging Beats", color = secondaryText.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = coffeeBrown,
                        unfocusedBorderColor = deepEspresso,
                        focusedLabelColor = softLatte,
                        cursorColor = coffeeBrown,
                        focusedTextColor = warmCream,
                        unfocusedTextColor = warmCream
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (showError) {
                    Text(
                        text = "Name cannot be empty",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = secondaryText)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                onCreate(playlistName.trim())
                                onDismiss()
                            } else {
                                showError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = coffeeBrown),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create", color = deepEspresso, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
