package com.gchat.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gchat.domain.model.FormalityLevel

/**
 * Dropdown selector for message formality levels (Casual, Neutral, Formal).
 * 
 * Shows a button that opens a dropdown menu with 3 radio options.
 * Each option includes an emoji icon and description.
 * 
 * Only visible when the message text is longer than 10 characters.
 */
@Composable
fun FormalitySelector(
    selectedFormality: FormalityLevel,
    onFormalitySelected: (FormalityLevel) -> Unit,
    onAdjustClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // Icon button to open dropdown
        IconButton(
            onClick = { expanded = true },
            enabled = enabled && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Show formality emoji as the icon
                Text(
                    text = selectedFormality.icon(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(
                text = "Adjust Formality",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // Casual option
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Text(text = FormalityLevel.CASUAL.icon() + " ")
                            Text(text = FormalityLevel.CASUAL.displayName())
                        }
                        if (selectedFormality == FormalityLevel.CASUAL) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFormalitySelected(FormalityLevel.CASUAL)
                }
            )
            
            // Neutral option
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Text(text = FormalityLevel.NEUTRAL.icon() + " ")
                            Text(text = FormalityLevel.NEUTRAL.displayName())
                        }
                        if (selectedFormality == FormalityLevel.NEUTRAL) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFormalitySelected(FormalityLevel.NEUTRAL)
                }
            )
            
            // Formal option
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Text(text = FormalityLevel.FORMAL.icon() + " ")
                            Text(text = FormalityLevel.FORMAL.displayName())
                        }
                        if (selectedFormality == FormalityLevel.FORMAL) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFormalitySelected(FormalityLevel.FORMAL)
                }
            )
            
            Divider()
            
            // Apply button
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                onAdjustClick()
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Text("Apply Adjustment")
                        }
                    }
                },
                onClick = {
                    onAdjustClick()
                    expanded = false
                }
            )
        }
    }
}

