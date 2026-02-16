# Key Talking Points: Small Models Theory

## Core Concept: Why Small Models?

**The Insight:** Most mobile AI tasks don't need GPT-4 sized models.

| Task | Cloud Model Needed? | On-Device Model Size |
|------|---------------------|----------------------|
| Text generation | No | 50-300MB |
| Function calling | No | 100-300MB |
| Embeddings | No | 1-50MB |
| Classification | No | 1-10MB |
| Complex reasoning | Maybe | 1GB+ |

---

## Transformer Architecture (Simplified)

### The Building Blocks

```
Input → Embed → [Transformer Block × N] → Output
                     ↓
             ┌─────────────────┐
             │ Self-Attention  │  ← "Look at other words"
             │ + LayerNorm     │
             │ + Feed-Forward  │  ← "Process the info"
             │ + LayerNorm     │
             └─────────────────┘
```

### Key Components Explained Simply

**1. Embeddings**
- Convert words → numbers (vectors)
- Similar words get similar vectors
- `cat` and `feline` are close in embedding space

**2. Self-Attention**
- Each word "looks at" all other words
- Calculates: "How much should I pay attention to word X?"
- Enables understanding context: "bank" (river vs money)

**3. Feed-Forward Network**
- Simple neural network applied to each position
- Processes the attention-processed information
- Where most parameters live

**4. Layer Normalization**
- Keeps numbers stable during training
- Essential for deep networks

---

## Why Small Models Work

### 1. Task-Specific vs General-Purpose

```
GPT-4: Knows everything, answers everything → 1.7T params
Gemma 270M: Knows function calling, answers function calls → 270M params
Tiny Transformer: Knows command parsing → ~0.8M params
```

**Key insight:** Narrow the task → shrink the model

### 2. The Scaling Reality

| Model | Params | Can Do | Can't Do |
|-------|--------|--------|----------|
| 1M | Basic patterns | Complex reasoning |
| 10M | Simple NLP | Long contexts |
| 100M | App commands | Open-domain QA |
| 1B | Mobile assistant | Research papers |
| 10B+ | General chat | Needs GPU server |

### 3. Efficiency Tricks

**Quantization:** Store weights in fewer bits
- FP32 → INT8 → INT4
- 4× smaller with minimal quality loss

**Weight Tying:** Share embedding and output weights
- Cuts parameters in half for embeddings

**Pruning:** Remove unimportant connections
- Like trimming a tree, keep what matters

---

## Function Calling Architecture

### How It Works

```
User Input: "Set alarm for 7am"
              ↓
        [Tokenizer]
              ↓
        [Gemma 270M]
              ↓
Structured Output: set_alarm(time="7am")
```

### The Training Process

1. Start with base Gemma 270M
2. Fine-tune on (instruction, function_call) pairs
3. Model learns to output structured format
4. Accuracy improves from 58% → 85%

---

## Fine-Tuning FunctionGemma

### Why Fine-Tune?

Base FunctionGemma doesn't know YOUR functions. Fine-tuning:
- Teaches your custom tool names
- Improves accuracy 58% → 85%+
- Matches your app's terminology
- Reduces refusals

### Tools Available

**Unsloth (Recommended)**
- 30x faster than standard training
- 90% less VRAM usage
- Works on free Google Colab (T4 GPU)
- Auto exports to GGUF format
- Supports NVIDIA, AMD, and Intel GPUs (not Apple Silicon)

**Hardware Requirements**
- Free Colab T4: ✓ Works (1-2 hours)
- RX 6600 8GB: ✓ Works
- RTX 3060 12GB: ✓ Fast
- Mac M1/M2/M3: ✗ Not supported (use Colab)

### Training Data Format

```json
{
  "conversations": [
    {"role": "user", "content": "Show alert hello"},
    {"role": "assistant", "content": "{\"call\":\"show_alert\",\"msg\":\"hello\"}"}
  ]
}
```

### LoRA Benefits

- Train only 1-5% of weights
- 10x smaller checkpoints
- Can swap adapters for different tools
- Fits in consumer GPU memory

### Process Overview

1. Prepare training data (100-1000 examples)
2. Load base model with Unsloth
3. Configure LoRA adapters
4. Train 1-2 hours
5. Export to GGUF
6. Deploy to mobile via llama.cpp

---

## Embedding Models Explained

### What They Do

```
Text In → [Encoder] → Vector Out

"great product" → [0.23, -0.45, 0.12, ...]
"awesome item"  → [0.25, -0.42, 0.15, ...]  ← Similar!
"terrible"      → [-0.60, 0.30, -0.22, ...] ← Different!
```

### Why They're Tiny

- No need to generate text → smaller output head
- Pooling compresses information → fixed size output
- Similarity is simple math → no complex decoder

---

## Mobile Deployment Considerations

### Memory vs Model Size

| Model Size | RAM Needed | Devices |
|------------|------------|---------|
| 10MB | 100MB | All phones |
| 100MB | 500MB | Mid-range+ |
| 300MB | 1GB | Flagship |
| 1GB+ | 2GB+ | High-end only |

### Latency Factors

1. **Model loading:** One-time cost
2. **Token generation:** Per-token cost
3. **Context length:** Linear scaling

### Battery Impact

- 100MB model: ~0.03% per inference
- 500MB model: ~0.1% per inference
- Much better than cloud (network + server costs)

---

## Key Takeaways for the Audience

1. **You can build this** - A working transformer is ~420 lines of well-commented Python
2. **Size matters** - Match model size to task complexity
3. **Fine-tuning is accessible** - Free Colab + 1 hour = custom model
4. **Privacy wins** - On-device = no data leaves the phone
5. **Offline works** - No internet needed
6. **Cost savings** - No per-token API fees
7. **User experience** - <300ms latency vs 1-2s cloud calls

---

## Resources

- **NanoGPT:** Andrej Karpathy's minimal GPT implementation
- **Gemma Models:** Google's open models for on-device AI
- **Unsloth:** Fast fine-tuning library - works on free Colab
- **MediaPipe:** Google's mobile ML inference framework
- **llama.cpp:** Efficient CPU inference for edge devices
- **FunctionGemma Colab:** 1-click fine-tuning notebook