package com.opensetlist.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SortMenu(
    currentLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { open = true }) {
            Text(currentLabel)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(index)
                        open = false
                    }
                )
            }
        }
    }
}
