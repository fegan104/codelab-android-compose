package com.example.compose.rally.ui.jsoninput

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.compose.rally.RallyViewModel

@Composable
fun JsonInputScreen(
    viewModel: RallyViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Paste Promotion JSON",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = viewModel.jsonContent,
            onValueChange = { viewModel.jsonContent = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            label = { Text("JSON Content") },
            placeholder = { Text("Paste your promotion JSON here...") },
            maxLines = Int.MAX_VALUE,
            textStyle = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace),
        )
    }
}
