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
Tiny Transformer: Knows Shakespeare snippet → 0.02M params
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

1. **You can build this** - A working transformer is ~150 lines of code
2. **Size matters** - Match model size to task complexity
3. **Privacy wins** - On-device = no data leaves the phone
4. **Offline works** - No internet needed
5. **Cost savings** - No per-token API fees
6. **User experience** - <300ms latency vs 1-2s cloud calls

---

## Resources

- **NanoGPT:** Andrej Karpathy's minimal GPT implementation
- **Gemma Models:** Google's open models for on-device AI
- **MediaPipe:** Google's mobile ML inference framework
- **llama.cpp:** Efficient CPU inference for edge devices