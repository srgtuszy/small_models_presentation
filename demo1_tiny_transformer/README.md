# Demo 1: Train a Tiny Transformer

**Model Size:** ~3MB  
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
| Layers | 4 | Keep it tiny for mobile |
| Heads | 4 | Sufficient for small vocab |
| Embedding dim | 128 | Balance size/quality |
| Block size | 80 | Short context is fine |

## Running the Demo

```bash
python tiny_transformer_train.py
```

## Output

The model will:
1. Train for 10,000 iterations
2. Print loss every 2,000 steps
3. Save model weights and vocabulary to `model_output/`
4. Run test inferences and print results

## Key Takeaways

1. **Simplicity wins** - A working transformer is ~420 lines of well-commented code
2. **Size matters** - Only ~0.8M parameters = runs anywhere
3. **From scratch to production** - Same concepts scale to GPT-4