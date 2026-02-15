@file:OptIn(ExperimentalMaterial3Api::class)

package demo.visualllm

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DemoImage(
    val id: Int,
    val name: String,
    val emoji: String,
    val category: String,
    val description: String
)

data class ModelSize(
    val name: String,
    val params: String,
    val size: String,
    val speed: String,
    val quality: String,
    val color: Color
)

data class ProcessingStep(
    val name: String,
    val description: String,
    val icon: ImageVector
)

val sampleImages = listOf(
    DemoImage(1, "Cat on Sofa", "🐱", "Animals", "A fluffy orange cat sitting comfortably on a beige sofa"),
    DemoImage(2, "City Skyline", "🏙️", "Architecture", "Modern city skyline at sunset with tall buildings"),
    DemoImage(3, "Beach Sunset", "🌅", "Nature", "Beautiful sunset over calm ocean waves"),
    DemoImage(4, "Forest Path", "🌲", "Nature", "Winding path through dense green forest"),
    DemoImage(5, "Coffee Cup", "☕", "Objects", "Hot coffee in white ceramic cup with steam rising"),
    DemoImage(6, "Mountain Peak", "🏔️", "Nature", "Snow-capped mountain peak against blue sky"),
    DemoImage(7, "Food Plate", "🍽️", "Food", "Gourmet dish with vegetables and grilled meat"),
    DemoImage(8, "Old Book", "📖", "Objects", "Vintage leather-bound book with gold lettering")
)

val modelSizes = listOf(
    ModelSize("Nano", "1M", "4MB", "50 tok/s", "Basic", Color(0xFF4CAF50)),
    ModelSize("Tiny", "10M", "40MB", "30 tok/s", "Good", Color(0xFF8BC34A)),
    ModelSize("Small", "100M", "400MB", "20 tok/s", "Better", Color(0xFFCDDC39)),
    ModelSize("Base", "500M", "2GB", "15 tok/s", "Great", Color(0xFFFFEB3B)),
    ModelSize("Large", "1B", "4GB", "10 tok/s", "Excellent", Color(0xFFFF9800))
)

val processingSteps = listOf(
    ProcessingStep("Image Input", "Raw image pixels", Icons.Default.Image),
    ProcessingStep("Vision Encoder", "Extract visual features", Icons.Default.Visibility),
    ProcessingStep("Projection", "Map to language space", Icons.Default.Transform),
    ProcessingStep("Language Model", "Generate description", Icons.Default.Chat),
    ProcessingStep("Output", "Final caption", Icons.Default.Output)
)

@Composable
fun App() {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Image Analysis", "Model Sizes", "Processing Flow", "Compare Models")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar()
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Image, contentDescription = null)
                                1 -> Icon(Icons.Default.Storage, contentDescription = null)
                                2 -> Icon(Icons.Default.AccountTree, contentDescription = null)
                                3 -> Icon(Icons.Default.Compare, contentDescription = null)
                            }
                        }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> ImageAnalysisTab()
                1 -> ModelSizesTab()
                2 -> ProcessingFlowTab()
                3 -> CompareModelsTab()
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
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Visual LLM Demo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Small models for image understanding",
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
fun ImageAnalysisTab() {
    var selectedImage by remember { mutableStateOf<DemoImage?>(null) }
    var analysisState by remember { mutableStateOf<AnalysisState>(AnalysisState.Idle) }
    var generatedCaption by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Row(modifier = Modifier.fillMaxSize()) {
        ImageSelector(
            images = sampleImages,
            selectedImage = selectedImage,
            onImageSelected = { image ->
                selectedImage = image
                analysisState = AnalysisState.Idle
                generatedCaption = ""
                answer = ""
            },
            modifier = Modifier.width(280.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            if (selectedImage != null) {
                ImagePreview(
                    image = selectedImage!!,
                    caption = generatedCaption,
                    analysisState = analysisState,
                    onAnalyze = {
                        scope.launch {
                            analysisState = AnalysisState.Processing
                            delay(1500)
                            generatedCaption = selectedImage!!.description
                            analysisState = AnalysisState.Complete
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                VisualQA(
                    question = question,
                    onQuestionChange = { question = it },
                    answer = answer,
                    onAsk = {
                        if (question.isNotBlank() && selectedImage != null) {
                            scope.launch {
                                analysisState = AnalysisState.Processing
                                delay(1000)
                                answer = generateAnswer(question, selectedImage!!)
                                analysisState = AnalysisState.Complete
                            }
                        }
                    }
                )
            } else {
                EmptyState()
            }
        }
    }
}

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Processing : AnalysisState()
    object Complete : AnalysisState()
}

@Composable
fun ImageSelector(
    images: List<DemoImage>,
    selectedImage: DemoImage?,
    onImageSelected: (DemoImage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            "Sample Images",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(images) { image ->
                ImageCard(
                    image = image,
                    isSelected = selectedImage == image,
                    onClick = { onImageSelected(image) }
                )
            }
        }
    }
}

@Composable
fun ImageCard(
    image: DemoImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(image.emoji, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    image.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    image.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ImagePreview(
    image: DemoImage,
    caption: String,
    analysisState: AnalysisState,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        image.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        image.category,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Button(
                    onClick = onAnalyze,
                    enabled = analysisState != AnalysisState.Processing
                ) {
                    if (analysisState == AnalysisState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analyze")
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(image.emoji, fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Simulated Image",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            AnimatedVisibility(
                visible = caption.isNotEmpty(),
                enter = fadeIn() + slideInVertically()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Title,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Generated Caption",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            caption,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VisualQA(
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAsk: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Visual Question Answering",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about the image...") },
                    singleLine = true
                )
                Button(onClick = onAsk, enabled = question.isNotBlank()) {
                    Icon(Icons.Default.Send, contentDescription = null)
                }
            }
            
            AnimatedVisibility(
                visible = answer.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            answer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ImageNotSupported,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Select an image to analyze",
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ModelSizesTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Model Size vs Capability Trade-off",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Smaller models are faster and use less memory, but have reduced capabilities. Choose based on your use case.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        items(modelSizes) { model ->
            ModelSizeCard(model)
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            RecommendationCard()
        }
    }
}

@Composable
fun ModelSizeCard(model: ModelSize) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(model.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    model.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = model.color
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    model.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "${model.params} parameters • ${model.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    model.quality,
                    fontWeight = FontWeight.Medium,
                    color = model.color
                )
                Text(
                    model.speed,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RecommendationCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recommendation for Mobile",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "For on-device image understanding:\n• Nano-Tiny: Simple classification, object detection\n• Small: Basic captioning, VQA\n• Base+: Complex reasoning, detailed descriptions",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ProcessingFlowTab() {
    var currentStep by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Vision-Language Model Pipeline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "How a small VLM processes images",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isRunning = true
                            currentStep = 0
                            processingSteps.indices.forEach { i ->
                                currentStep = i
                                delay(800)
                            }
                            isRunning = false
                        }
                    },
                    enabled = !isRunning
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Processing..." else "Run Demo")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(processingSteps.withIndex().toList()) { (index, step) ->
                ProcessingStepCard(
                    step = step,
                    isActive = currentStep == index,
                    isComplete = currentStep > index || (currentStep == processingSteps.lastIndex && index == currentStep),
                    stepNumber = index + 1
                )
            }
        }
    }
}

@Composable
fun ProcessingStepCard(
    step: ProcessingStep,
    isActive: Boolean,
    isComplete: Boolean,
    stepNumber: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isActive -> MaterialTheme.colorScheme.primaryContainer
                isComplete -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isComplete -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        stepNumber.toString(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    step.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    step.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                step.icon,
                contentDescription = null,
                tint = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isComplete -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
fun CompareModelsTab() {
    var selectedImage by remember { mutableStateOf(sampleImages.first()) }
    val captions = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Compare Model Outputs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "See how different model sizes generate captions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleImages.take(4)) { image ->
                        FilterChip(
                            selected = selectedImage == image,
                            onClick = { selectedImage = image },
                            label = { Text(image.emoji + " " + image.name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            captions.value = emptyMap()
                            modelSizes.forEach { model ->
                                delay(500)
                                captions.value = captions.value + (
                                    model.name to generateCaptionForModel(model, selectedImage)
                                )
                            }
                            isGenerating = false
                        }
                    },
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGenerating) "Generating..." else "Generate Captions")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modelSizes) { model ->
                CaptionResultCard(
                    model = model,
                    caption = captions.value[model.name]
                )
            }
        }
    }
}

@Composable
fun CaptionResultCard(
    model: ModelSize,
    caption: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            model.color.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(model.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            model.name.first().toString(),
                            fontWeight = FontWeight.Bold,
                            color = model.color
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        model.name,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    model.quality,
                    fontSize = 12.sp,
                    color = model.color
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (caption != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        caption,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    "Waiting to generate...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

fun generateAnswer(question: String, image: DemoImage): String {
    val q = question.lowercase()
    return when {
        "what" in q && "color" in q -> "Based on the image analysis, the main colors present are typical for a ${image.category.lowercase()} scene."
        "how many" in q -> "The image contains the main subject with a clear composition."
        "where" in q -> "This ${image.name.lowercase()} appears to be in a natural setting."
        "what" in q -> "This is ${image.description.lowercase()}."
        "describe" in q -> image.description
        else -> "I can see ${image.description.lowercase()}."
    }
}

fun generateCaptionForModel(model: ModelSize, image: DemoImage): String {
    return when (model.name) {
        "Nano" -> image.category
        "Tiny" -> "A ${image.category.lowercase()} image"
        "Small" -> image.name
        "Base" -> image.description.take(50) + "..."
        "Large" -> image.description
        else -> image.name
    }
}