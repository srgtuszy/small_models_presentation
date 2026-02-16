package org.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.model.Command
import org.example.model.CommandParser

@Composable
fun App(
    parser: CommandParser? = null,
    parsing: Boolean = false,
    onParse: (String, (Command?) -> Unit) -> Unit = { _, _ -> }
) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var lastCommand by remember { mutableStateOf<Command?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Command Parser",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Type a natural language command to convert to JSON",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter command") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g., show alert with message Hello") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (inputText.isNotBlank() && parser != null) {
onParse(inputText) { command ->
                val jsonOutput = command?.let { 
                    when {
                        it.isUnrecognized() -> """{"action": "unrecognized", "input": "${it.input}"}"""
                        it.message != null -> """{"action": "${it.action}", "message": "${it.message}"}"""
                        it.target != null -> """{"action": "${it.action}", "target": "${it.target}"}"""
                        it.setting != null -> """{"action": "${it.action}", "setting": "${it.setting}"}"""
                        else -> """{"action": "${it.action}"}"""
                    }
                } ?: "Failed to parse command"
                
                outputText = jsonOutput
                lastCommand = command
                history.add(0, "Input: $inputText\nOutput: $jsonOutput")
                
                if (command?.action == "alert" && command.message != null) {
                    showDialog = true
                }
            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !parsing
                ) {
                    if (parsing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Parse Command")
                    }
                }
                
                OutlinedButton(
                    onClick = {
                        inputText = ""
                        outputText = ""
                        lastCommand = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (outputText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Result",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = outputText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            if (lastCommand != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                CommandActionCard(lastCommand!!)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                history.forEach { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = entry,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ExampleCommandsSection { command ->
                inputText = command
            }
        }
    }
    
    if (showDialog && lastCommand?.message != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Alert") },
            text = { Text(lastCommand!!.message!!) },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CommandActionCard(command: Command) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (command.action) {
                "alert" -> Color(0xFFFFEBEE)
                "navigate" -> Color(0xFFE3F2FD)
                "toggle" -> Color(0xFFF3E5F5)
                "refresh" -> Color(0xFFE8F5E9)
                "back" -> Color(0xFFFFF3E0)
                "close" -> Color(0xFFFFECEC)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Action: ${command.action.uppercase()}",
                style = MaterialTheme.typography.titleMedium
            )
            when {
                command.message != null -> Text(
                    text = "  Message: ${command.message}",
                    style = MaterialTheme.typography.bodyMedium
                )
                command.target != null -> Text(
                    text = "  Target: ${command.target}",
                    style = MaterialTheme.typography.bodyMedium
                )
                command.setting != null -> Text(
                    text = "  Setting: ${command.setting}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ExampleCommandsSection(onClick: (String) -> Unit) {
    Column {
        Text(
            text = "Example commands:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val examples = listOf(
            "show alert with message Hello",
            "navigate to settings",
            "toggle dark_mode",
            "go back",
            "refresh the page",
            "close the app"
        )
        
        examples.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { example ->
                    Box(modifier = Modifier.weight(1f)) {
                        FilterChip(
                            selected = false,
                            onClick = { onClick(example) },
                            label = { 
                                Text(
                                    example, 
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}