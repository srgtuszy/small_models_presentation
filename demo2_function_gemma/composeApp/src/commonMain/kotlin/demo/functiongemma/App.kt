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

fun extractAlertMessage(input: String): String {
    val lower = input.lowercase()
    val keywords = listOf("alert", "show", "display", "popup", "with", "saying", "message")
    
    var msg = input
    for (keyword in keywords) {
        val idx = lower.indexOf(keyword)
        if (idx >= 0) {
            msg = input.substring(idx + keyword.length).trim()
            break
        }
    }
    
    return msg.trim('"', '\'').ifEmpty { "Hello" }
}

@Composable
fun App() {
    MaterialTheme {
        var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
        var inputText by remember { mutableStateOf("") }
        var messageId by remember { mutableStateOf(0) }
        var isLoading by remember { mutableStateOf(false) }
        var modelStatus by remember { mutableStateOf("Loading model...") }
        var showDialog by remember { mutableStateOf(false) }
        var dialogMessage by remember { mutableStateOf("") }
        
        val llmEngine = remember { createLLMEngine() }
        val scope = rememberCoroutineScope()
        
        val alertTool = Tool(
            name = "show_alert",
            description = "Display an alert dialog with a message to the user",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "message" to mapOf(
                        "type" to "string",
                        "description" to "The message to display in the alert"
                    )
                ),
                "required" to listOf("message")
            )
        )
        
        LaunchedEffect(Unit) {
            val error = llmEngine.initialize()
            modelStatus = if (error == null) {
                "Model loaded"
            } else {
                "Model load failed: $error"
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                                "Function Gemma",
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
                        val messageText = inputText
                        inputText = ""
                        
                        val userMessage = ChatMessage(
                            id = messageId++,
                            text = messageText,
                            isUser = true
                        )
                        messages = messages + userMessage
                        isLoading = true
                        
                        scope.launch {
                            val response = llmEngine.generateResponse(messageText, listOf(alertTool))
                            isLoading = false
                            
                            val responseMessage = when (response) {
                                is LLMResponse.Text -> ChatMessage(
                                    id = messageId++,
                                    text = response.content,
                                    isUser = false
                                )
                                is LLMResponse.FunctionCall -> {
                                    val message = response.result.arguments["message"]?.takeIf { it.isNotBlank() }
                                        ?: extractAlertMessage(messageText)
                                    dialogMessage = message
                                    showDialog = true
                                    ChatMessage(
                                        id = messageId++,
                                        text = "Showing alert: \"$message\"",
                                        isUser = false,
                                        functionCall = FunctionCall(
                                            name = response.result.name,
                                            params = response.result.arguments,
                                            timestamp = "now"
                                        )
                                    )
                                }
                                is LLMResponse.Error -> ChatMessage(
                                    id = messageId++,
                                    text = response.message,
                                    isUser = false,
                                    isError = true
                                )
                            }
                            messages = messages + responseMessage
                        }
                    }
                },
                isLoading = isLoading
            )
        }
        
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Alert") },
                text = { Text(dialogMessage) },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
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
                    "Show an alert with message Hello World",
                    "Display a popup saying Welcome",
                    "Alert the user that the task is done"
                ).forEach { example ->
                    Text(
                        "\"$example\"",
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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