@file:OptIn(ExperimentalMaterial3Api::class)

package demo.functiongemma

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FunctionCall(
    val name: String,
    val params: Map<String, String>,
    val timestamp: String
)

data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val functionCall: FunctionCall? = null
)

val availableFunctions = listOf(
    FunctionDef("set_reminder", "Set a reminder", listOf("title", "time")),
    FunctionDef("navigate_to_screen", "Navigate to screen", listOf("screen")),
    FunctionDef("toggle_setting", "Toggle a setting", listOf("setting", "enabled")),
    FunctionDef("send_message", "Send a message", listOf("contact", "message")),
    FunctionDef("get_weather", "Get weather info", listOf("location")),
    FunctionDef("play_music", "Play music", listOf("song", "artist")),
    FunctionDef("set_timer", "Set a timer", listOf("duration")),
    FunctionDef("make_call", "Make a phone call", listOf("contact"))
)

data class FunctionDef(
    val name: String,
    val description: String,
    val params: List<String>
)

@Composable
fun App() {
    MaterialTheme {
        var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
        var inputText by remember { mutableStateOf("") }
        var messageId by remember { mutableStateOf(0) }
        var selectedFunction by remember { mutableStateOf<FunctionDef?>(null) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar()
            
            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                Sidebar(
                    functions = availableFunctions,
                    selectedFunction = selectedFunction,
                    onFunctionSelected = { selectedFunction = it }
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ChatArea(
                        messages = messages,
                        modifier = Modifier.weight(1f)
                    )
                    
                    InputArea(
                        text = inputText,
                        onTextChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                val userMessage = ChatMessage(
                                    id = messageId++,
                                    text = inputText,
                                    isUser = true
                                )
                                val response = generateResponse(inputText)
                                messages = messages + userMessage + response
                                inputText = ""
                            }
                        },
                        selectedFunction = selectedFunction
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Function Gemma Demo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "On-device function calling",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun Sidebar(
    functions: List<FunctionDef>,
    selectedFunction: FunctionDef?,
    onFunctionSelected: (FunctionDef) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            "Available Functions",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(functions) { func ->
                FunctionChip(
                    func = func,
                    isSelected = selectedFunction == func,
                    onClick = { onFunctionSelected(func) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Model Info",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Gemma 270M IT\n~288MB (INT4)\n~0.3s TTFT\n~126 tok/s",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun FunctionChip(
    func: FunctionDef,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Text(func.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    func.params.joinToString(", ") { it },
                    fontSize = 10.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        },
        leadingIcon = {
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun ChatArea(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Try saying:",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Set a reminder for tomorrow at 5pm",
                    "Navigate to settings",
                    "Turn on dark mode",
                    "What's the weather in Warsaw?"
                ).forEach { example ->
                    Text(
                        "\"$example\"",
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.functionCall != null) {
                    FunctionCallCard(message.functionCall)
                } else {
                    Text(
                        message.text,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FunctionCallCard(call: FunctionCall) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Function Executed",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    call.name + "(",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                call.params.forEach { (key, value) ->
                    Row(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "$key=",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "\"$value\"",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                Text(
                    ")",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun InputArea(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    selectedFunction: FunctionDef?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (selectedFunction != null) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            "Target: ${selectedFunction.name}",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter your command...") },
                    leadingIcon = {
                        Icon(Icons.Default.Chat, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FilledIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

fun generateResponse(input: String): ChatMessage {
    val lowerInput = input.lowercase()
    
    val functionCall = when {
        "remind" in lowerInput || "reminder" in lowerInput -> {
            val time = extractValue(lowerInput, listOf("at", "for", "tomorrow", "today")) ?: "soon"
            val title = extractTitle(lowerInput)
            FunctionCall("set_reminder", mapOf("title" to title, "time" to time), "now")
        }
        "navigate" in lowerInput || "go to" in lowerInput || "open" in lowerInput -> {
            val screen = when {
                "setting" in lowerInput -> "settings"
                "profil" in lowerInput -> "profile"
                "home" in lowerInput -> "home"
                else -> "home"
            }
            FunctionCall("navigate_to_screen", mapOf("screen" to screen), "now")
        }
        "dark mode" in lowerInput || "light mode" in lowerInput -> {
            val enabled = "dark mode" in lowerInput && ("on" in lowerInput || "enable" in lowerInput || "turn" in lowerInput)
            FunctionCall("toggle_setting", mapOf("setting" to "dark_mode", "enabled" to enabled.toString()), "now")
        }
        "weather" in lowerInput -> {
            val location = extractValue(lowerInput, listOf("in", "at", "for")) ?: "current"
            FunctionCall("get_weather", mapOf("location" to location), "now")
        }
        "message" in lowerInput || "send" in lowerInput -> {
            val contact = extractValue(lowerInput, listOf("to")) ?: "contact"
            val msg = extractMessage(lowerInput)
            FunctionCall("send_message", mapOf("contact" to contact, "message" to msg), "now")
        }
        "timer" in lowerInput -> {
            val duration = extractValue(lowerInput, listOf("for")) ?: "5 minutes"
            FunctionCall("set_timer", mapOf("duration" to duration), "now")
        }
        "call" in lowerInput -> {
            val contact = extractValue(lowerInput, listOf("call")) ?: "contact"
            FunctionCall("make_call", mapOf("contact" to contact), "now")
        }
        "play" in lowerInput && ("music" in lowerInput || "song" in lowerInput) -> {
            val song = extractValue(lowerInput, listOf("play")) ?: "music"
            FunctionCall("play_music", mapOf("song" to song, "artist" to "unknown"), "now")
        }
        else -> null
    }
    
    return ChatMessage(
        id = (0..10000).random(),
        text = if (functionCall != null) {
            "Function call generated for: \"$input\""
        } else {
            "I can help you with reminders, navigation, settings, weather, messages, timers, calls, and music. Try: \"Set a reminder to buy milk tomorrow\""
        },
        isUser = false,
        functionCall = functionCall
    )
}

fun extractValue(input: String, keywords: List<String>): String? {
    for (keyword in keywords) {
        val index = input.indexOf(keyword)
        if (index >= 0) {
            val after = input.substring(index + keyword.length).trim()
            val words = after.split(" ").take(3).filter { 
                it !in listOf("at", "in", "to", "for", "the", "a", "an") 
            }
            if (words.isNotEmpty()) {
                return words.joinToString(" ")
            }
        }
    }
    return null
}

fun extractTitle(input: String): String {
    val words = input.split(" ")
    val remindIndex = words.indexOfFirst { it.contains("remind") }
    if (remindIndex >= 0) {
        val titleWords = words.drop(remindIndex + 1)
            .takeWhile { !listOf("at", "for", "tomorrow", "today", "pm", "am").contains(it) }
        return titleWords.joinToString(" ").ifEmpty { "reminder" }
    }
    return "reminder"
}

fun extractMessage(input: String): String {
    val words = input.split(" ")
    val sayingIndex = words.indexOfFirst { it == "saying" }
    if (sayingIndex >= 0) {
        return words.drop(sayingIndex + 1).joinToString(" ")
    }
    return "hello"
}