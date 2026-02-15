# Demo 1: Train a Tiny Transformer

**Model Size:** ~5-10MB  
**Training Time:** 2-3 minutes on laptop  
**Purpose:** Show that you can build and train models from scratch

## What This Demo Shows

- Build a GPT-style transformer from scratch
- Train it in minutes on a laptop
- Understand the core architecture components:
  - Token embeddings
  - Positional embeddings
  - Causal self-attention
  - Layer normalization
  - Feed-forward networks (MLP)

## Key Architecture Decisions

| Component | Value | Why |
|-----------|-------|-----|
| Layers | 2 | Keep it tiny for mobile |
| Heads | 2 | Sufficient for small vocab |
| Embedding dim | 128 | Balance size/quality |
| Block size | 128 | Short context is fine |

## Running the Demo

```bash
python tiny_transformer_train.py
```

## Output

The model will:
1. Train for 2000 iterations
2. Print loss every 500 steps
3. Save the model to `tiny_transformer.pt`
4. Generate a sample text output

## Key Takeaways

1. **Simplicity wins** - A working transformer is ~150 lines of code
2. **Size matters** - Only 0.02M parameters = runs anywhere
3. **From scratch to production** - Same concepts scale to GPT-4