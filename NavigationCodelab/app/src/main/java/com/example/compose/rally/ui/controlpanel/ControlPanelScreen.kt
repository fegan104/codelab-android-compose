package com.example.compose.rally.ui.controlpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.compose.rally.data.mock.MockSourceKeyEntry
import com.example.compose.rally.data.mock.MockVegasDataSource
import java.util.Map.entry

/**
 * Control Panel screen that displays all source/key entries
 * with appropriate input controls for each type.
 */
@Composable
fun ControlPanelScreen(
    entries: List<MockSourceKeyEntry>,
    dataSource: MockVegasDataSource,
    onResetAll: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Control Panel",
                style = MaterialTheme.typography.h5
            )
            TextButton(onClick = onResetAll) {
                Text("Reset All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Text(
                text = "No entries found. Paste a promotion JSON first.",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = "${entries.size} source keys found",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (entry in entries) {
                    SourceKeyEntryCard(
                        entry = entry,
                        dataSource = dataSource
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceKeyEntryCard(
    entry: MockSourceKeyEntry,
    dataSource: MockVegasDataSource
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Label
            Text(
                text = entry.displayLabel,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary
            )

            // Type indicator
            Text(
                text = when (entry) {
                    is MockSourceKeyEntry.IntEntry -> "Integer"
                    is MockSourceKeyEntry.StringEntry -> "String"
                    is MockSourceKeyEntry.BooleanEntry -> "Boolean"
                    is MockSourceKeyEntry.StringSetEntry -> "String Set"
                },
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input control based on type
            when (entry) {
                is MockSourceKeyEntry.IntEntry -> IntEntryInput(
                    entry = entry,
                    dataSource = dataSource
                )
                is MockSourceKeyEntry.StringEntry -> StringEntryInput(
                    entry = entry,
                    dataSource = dataSource
                )
                is MockSourceKeyEntry.BooleanEntry -> BooleanEntryInput(
                    entry = entry,
                    dataSource = dataSource
                )
                is MockSourceKeyEntry.StringSetEntry -> StringSetEntryInput(
                    entry = entry,
                    dataSource = dataSource
                )
            }
        }
    }
}

@Composable
private fun IntEntryInput(
    entry: MockSourceKeyEntry.IntEntry,
    dataSource: MockVegasDataSource
) {
    val currentValue = dataSource.getIntOrNull(entry.uniqueId) ?: entry.defaultValue
    var textValue by remember(entry.uniqueId, currentValue) { 
        mutableStateOf(currentValue.toString()) 
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                newValue.toIntOrNull()?.let { intValue ->
                    dataSource.setIntByUniqueId(entry.uniqueId, intValue)
                }
            },
            modifier = Modifier.weight(1f),
            label = { Text("Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Default: ${entry.defaultValue}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun StringEntryInput(
    entry: MockSourceKeyEntry.StringEntry,
    dataSource: MockVegasDataSource
) {
    val currentValue = dataSource.getStringOrNull(entry.uniqueId) ?: entry.defaultValue

    OutlinedTextField(
        value = currentValue.toString(),
        onValueChange = { newValue ->
            dataSource.setStringByUniqueId(entry.uniqueId, newValue)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Value") },
        singleLine = true,
        placeholder = { 
            if (entry.defaultValue.isNullOrEmpty()) {
                Text("(empty)")
            } else {
                Text("Default: ${entry.defaultValue}")
            }
        }
    )
}

@Composable
private fun BooleanEntryInput(
    entry: MockSourceKeyEntry.BooleanEntry,
    dataSource: MockVegasDataSource
) {
    val currentValue = dataSource.getBooleanOrNull(entry.uniqueId) ?: entry.defaultValue

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentValue.toString(),
                style = MaterialTheme.typography.body1
            )
            Text(
                text = "Default: ${entry.defaultValue}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = currentValue == true,
            onCheckedChange = { newValue ->
                dataSource.setBooleanByUniqueId(entry.uniqueId, newValue)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary
            )
        )
    }
}

@Composable
private fun StringSetEntryInput(
    entry: MockSourceKeyEntry.StringSetEntry,
    dataSource: MockVegasDataSource
) {
    val currentValue = dataSource.getStringSetOrNull(entry.uniqueId) ?: entry.defaultValue
    var newItemText by remember { mutableStateOf("") }

    Column {
        // Current items
        if (currentValue.isNullOrEmpty()) {
            Text(
                text = "(empty set)",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        } else {
            currentValue.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            dataSource.setStringSetByUniqueId(
                                entry.uniqueId,
                                currentValue - item
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove $item",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
                Divider()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add new item
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.weight(1f),
                label = { Text("Add item") },
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        dataSource.setStringSetByUniqueId(
                            entry.uniqueId,
                            currentValue.orEmpty() + newItemText.trim()
                        )
                        newItemText = ""
                    }
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add item",
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }
}
