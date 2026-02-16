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
import demo.functiongemma.llm.*
import kotlinx.coroutines.launch

data class FunctionCall(
    val name: String,
    val params: Map<String, String>,
    val timestamp: String
)

data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val functionCall: FunctionCall? = null,
    val isError: Boolean = false
)

val availableFunctions = listOf(
    FunctionDef("set_reminder", "Set a reminder for a specific time", listOf("title", "time")),
    FunctionDef("navigate_to_screen", "Navigate to a specific screen in the app", listOf("screen")),
    FunctionDef("toggle_setting", "Toggle a setting on or off", listOf("setting", "enabled")),
    FunctionDef("send_message", "Send a message to a contact", listOf("contact", "message")),
    FunctionDef("get_weather", "Get weather information for a location", listOf("location")),
    FunctionDef("play_music", "Play a song by an artist", listOf("song", "artist")),
    FunctionDef("set_timer", "Set a timer for a duration", listOf("duration")),
    FunctionDef("make_call", "Make a phone call to a contact", listOf("contact"))
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
        var isLoading by remember { mutableStateOf(false) }
        var modelStatus by remember { mutableStateOf("Loading model...") }
        
        val llmEngine = remember { createLLMEngine() }
        val scope = rememberCoroutineScope()
        
        LaunchedEffect(Unit) {
            val error = llmEngine.initialize()
            modelStatus = if (error == null) {
                "Model loaded ✓"
            } else {
                "Model load failed: $error"
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar(modelStatus = modelStatus)
            
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
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    
                    InputArea(
                        text = inputText,
                        onTextChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val userMessage = ChatMessage(
                                    id = messageId++,
                                    text = inputText,
                                    isUser = true
                                )
                                messages = messages + userMessage
                                isLoading = true
                                
val tools = availableFunctions.map { func ->
        val properties = func.params.associate {
            it to mapOf(
                "type" to "string",
                "description" to "The ${it.replace("_", " ")} parameter"
            )
        }
        Tool(
            name = func.name,
            description = func.description,
            parameters = mapOf(
                "type" to "object",
                "properties" to properties,
                "required" to func.params
            )
        )
    }
                                
                                scope.launch {
                                    val response = llmEngine.generateResponse(inputText, tools)
                                    isLoading = false
                                    
                                    val responseMessage = when (response) {
                                        is LLMResponse.Text -> ChatMessage(
                                            id = messageId++,
                                            text = response.content,
                                            isUser = false
                                        )
                                        is LLMResponse.FunctionCall -> ChatMessage(
                                            id = messageId++,
                                            text = "Function called: ${response.result.name}",
                                            isUser = false,
                                            functionCall = FunctionCall(
                                                name = response.result.name,
                                                params = response.result.arguments,
                                                timestamp = "now"
                                            )
                                        )
                                        is LLMResponse.Error -> ChatMessage(
                                            id = messageId++,
                                            text = response.message,
                                            isUser = false,
                                            isError = true
                                        )
                                    }
                                    messages = messages + responseMessage
                                }
                                
                                inputText = ""
                            }
                        },
                        selectedFunction = selectedFunction,
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(modelStatus: String) {
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
                        modelStatus,
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
                    "FunctionGemma 270M\n~230MB (Q4_0)\n~0.3s TTFT\n~60 tok/s",
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
    isLoading: Boolean,
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
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thinking...", fontSize = 14.sp)
                            }
                        }
                    }
                }
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
                containerColor = when {
                    message.isError -> MaterialTheme.colorScheme.errorContainer
                    isUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.functionCall != null) {
                    FunctionCallCard(message.functionCall)
                } else {
                    Text(
                        message.text,
                        color = when {
                            message.isError -> MaterialTheme.colorScheme.onErrorContainer
                            isUser -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
    selectedFunction: FunctionDef?,
    isLoading: Boolean
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
                    enabled = text.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}