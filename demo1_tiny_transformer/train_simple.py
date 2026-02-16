#!/usr/bin/env python3
"""
Simple training script for a tiny transformer that learns to parse natural language
commands into structured JSON actions.

The model learns patterns like:
  "show alert Hello" -> {"action": "alert", "message": "Hello"}
  "navigate to settings" -> {"action": "navigate", "target": "settings"}
"""
import torch
import torch.nn as nn
import torch.nn.functional as F
import json
import math
import os
import shutil

# =============================================================================
# HYPERPARAMETERS
# =============================================================================
# block_size: Maximum sequence length the model can process (context window)
#   - Longer sequences = more context but slower training
#   - 80 characters is enough for our command->JSON pairs
block_size = 80

# batch_size: Number of sequences processed in parallel during training
#   - Larger batches = more stable gradients but more memory
batch_size = 20

# n_layer: Number of transformer blocks (depth)
#   - More layers = more complex patterns but risk of overfitting
n_layer = 4

# n_head: Number of attention heads per layer
#   - Multi-head attention allows learning different relationships simultaneously
n_head = 4

# n_embd: Embedding dimension (model's internal representation size)
#   - Larger = more expressive but more parameters
n_embd = 128

# dropout: Regularization to prevent overfitting
#   - Randomly zeros some activations during training
#   - 0.05 = 5% dropout, low since our dataset is focused
dropout = 0.05

# max_iters: Total training steps
max_iters = 10000

# lr: Learning rate for optimizer
lr = 1e-3

# =============================================================================
# LOAD DATASET
# =============================================================================
# Load pre-generated training samples from dataset.txt
# Each line is: "INPUT: <command> OUTPUT: <json>"
with open('dataset.txt', 'r') as f:
    samples = [line.strip() for line in f if line.strip()]

# Repeat samples 30x to create more training data
# This helps the model see each pattern multiple times
training_text = '\n'.join(samples * 30)
print(f'Samples: {len(samples)}, Total chars: {len(training_text)}')

# =============================================================================
# BUILD VOCABULARY (Character-level tokenization)
# =============================================================================
# Unlike word-level tokenization (like GPT uses), we use character-level
# This means each character gets its own token ID
# Pros: No unknown tokens, can generate any character
# Cons: Sequences are longer, less semantic understanding per token
chars = sorted(set(training_text))
vocab_size = len(chars)

# stoi: string-to-integer mapping (character -> token ID)
# itos: integer-to-string mapping (token ID -> character)
stoi = {c:i for i,c in enumerate(chars)}
itos = {i:c for c,i in stoi.items()}

# Helper functions to convert between text and token IDs
encode = lambda s: [stoi[c] for c in s]  # "hello" -> [h_id, e_id, l_id, l_id, o_id]
decode = lambda t: ''.join(itos[i] for i in t)  # [h_id, e_id] -> "he"

# Convert entire training text to token IDs
data = torch.tensor(encode(training_text), dtype=torch.long)

# Split into training (90%) and validation (10%) sets
n = int(0.9 * len(data))
train_data, val_data = data[:n], data[n:]

def get_batch(split):
    """
    Create a batch of training examples.
    
    For each batch, we randomly sample 'batch_size' starting positions
    and extract sequences of length 'block_size'.
    
    x: input sequence (tokens 0 to block_size-1)
    y: target sequence (tokens 1 to block_size) - shifted by 1 for next-token prediction
    
    The model learns: given tokens [0...i], predict token [i+1]
    """
    d = train_data if split == 'train' else val_data
    ix = torch.randint(len(d) - block_size, (batch_size,))
    x = torch.stack([d[i:i+block_size] for i in ix])
    y = torch.stack([d[i+1:i+block_size+1] for i in ix])
    return x, y

# =============================================================================
# MODEL ARCHITECTURE
# =============================================================================

class CausalSelfAttention(nn.Module):
    """
    Multi-head causal self-attention layer.
    
    This is the core of the transformer - it allows each position to attend
    to all previous positions (but not future ones, hence "causal").
    
    Intuition:
    - query: "what am I looking for?"
    - key: "what do I contain?"
    - value: "what will I contribute if attended to?"
    
    Attention score = softmax(Q @ K^T / sqrt(d)) @ V
    """
    def __init__(self):
        super().__init__()
        self.n_head = n_head
        # Project embeddings to query, key, value vectors
        self.key = nn.Linear(n_embd, n_embd, bias=False)
        self.query = nn.Linear(n_embd, n_embd, bias=False)
        self.value = nn.Linear(n_embd, n_embd, bias=False)
        # Output projection
        self.proj = nn.Linear(n_embd, n_embd, bias=False)
        # Dropout for regularization
        self.attn_drop = nn.Dropout(dropout)
        self.resid_drop = nn.Dropout(dropout)
        # Causal mask: lower triangular matrix ensures we only attend to past tokens
        # [[1, 0, 0],     Position 0 sees only itself
        #  [1, 1, 0],     Position 1 sees positions 0, 1
        #  [1, 1, 1]]     Position 2 sees positions 0, 1, 2
        self.register_buffer('mask', torch.tril(torch.ones(block_size, block_size)).view(1,1,block_size,block_size))

    def forward(self, x):
        B, T, C = x.size()  # batch, sequence length, embedding dimension
        
        # Compute Q, K, V and reshape for multi-head attention
        # (B, T, C) -> (B, n_head, T, C//n_head)
        k = self.key(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        q = self.query(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        v = self.value(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        
        # Compute attention scores: Q @ K^T / sqrt(d_k)
        # Scaling by sqrt(d_k) prevents softmax from becoming too peaked
        att = (q @ k.transpose(-2, -1)) / math.sqrt(k.size(-1))
        
        # Apply causal mask: set future positions to -inf (becomes 0 after softmax)
        att = att.masked_fill(self.mask[:,:,:T,:T]==0, float('-inf'))
        
        # Softmax normalizes scores to sum to 1 (probability distribution)
        att = F.softmax(att, dim=-1)
        att = self.attn_drop(att)
        
        # Weighted sum of values based on attention scores
        y = att @ v
        
        # Reshape back to (B, T, C) and project
        y = y.transpose(1,2).contiguous().view(B, T, C)
        return self.resid_drop(self.proj(y))


class MLP(nn.Module):
    """
    Feed-forward network applied after each attention layer.
    
    This allows the model to process and transform the information
    gathered by attention. The expansion to 4*n_embd creates a
    "bottleneck" architecture that learns more complex transformations.
    """
    def __init__(self):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_embd, 4*n_embd),  # Expand: 128 -> 512
            nn.GELU(),                     # Non-linearity (smoother than ReLU)
            nn.Linear(4*n_embd, n_embd),   # Contract: 512 -> 128
            nn.Dropout(dropout)
        )
    def forward(self, x): return self.net(x)


class Block(nn.Module):
    """
    A single transformer block = Attention + MLP with residual connections.
    
    Residual connections (x = x + layer(x)) help with:
    1. Gradient flow during backpropagation
    2. Preserving information from earlier layers
    
    LayerNorm is applied BEFORE the layer (pre-norm architecture)
    This is more stable than post-norm for training deep networks.
    """
    def __init__(self):
        super().__init__()
        self.ln1 = nn.LayerNorm(n_embd)
        self.attn = CausalSelfAttention()
        self.ln2 = nn.LayerNorm(n_embd)
        self.mlp = MLP()
    
    def forward(self, x):
        # Residual connection: add the layer's output to the input
        x = x + self.attn(self.ln1(x))  # Attention block
        x = x + self.mlp(self.ln2(x))   # MLP block
        return x


class TinyTransformer(nn.Module):
    """
    The complete transformer model.
    
    Architecture:
    1. Token embeddings: Convert token IDs to dense vectors
    2. Position embeddings: Add position information (since attention is permutation-invariant)
    3. N transformer blocks: Process the sequence
    4. Final layer norm: Stabilize output
    5. Linear head: Project to vocabulary logits
    
    Weight tying: The output projection shares weights with token embeddings.
    This reduces parameters and can improve performance.
    """
    def __init__(self):
        super().__init__()
        # Embeddings
        self.tok_emb = nn.Embedding(vocab_size, n_embd)  # Token -> vector
        self.pos_emb = nn.Embedding(block_size, n_embd)  # Position -> vector
        self.drop = nn.Dropout(dropout)
        
        # Transformer blocks
        self.blocks = nn.Sequential(*[Block() for _ in range(n_layer)])
        
        # Output
        self.ln_f = nn.LayerNorm(n_embd)
        self.head = nn.Linear(n_embd, vocab_size, bias=False)
        
        # Weight tying: share embedding and output weights
        self.head.weight = self.tok_emb.weight

    def forward(self, idx, targets=None):
        """
        Forward pass.
        
        Args:
            idx: Input token IDs, shape (B, T)
            targets: Target token IDs for loss computation, shape (B, T)
        
        Returns:
            logits: Predicted token probabilities (before softmax), shape (B, T, vocab_size)
            loss: Cross-entropy loss if targets provided
        """
        B, T = idx.shape
        
        # Get embeddings
        tok = self.tok_emb(idx)                    # (B, T, n_embd)
        pos = self.pos_emb(torch.arange(T))        # (T, n_embd)
        x = self.drop(tok + pos)                    # Combine and dropout
        
        # Process through transformer blocks
        x = self.blocks(x)
        
        # Final layer norm and project to vocabulary
        x = self.ln_f(x)
        logits = self.head(x)
        
        # Compute loss if targets provided
        loss = None
        if targets is not None:
            # Cross-entropy: -log(softmax(logits)[target])
            # Reshape to (B*T, vocab_size) and (B*T,) for cross_entropy
            loss = F.cross_entropy(logits.view(-1, logits.size(-1)), targets.view(-1))
        return logits, loss

    @torch.no_grad()
    def generate_greedy(self, idx, max_new_tokens, end_token=None):
        """
        Generate tokens autoregressively using greedy decoding.
        
        Greedy decoding: always pick the most likely next token.
        This is deterministic but may not produce the best overall sequence.
        
        Args:
            idx: Starting context, shape (1, T)
            max_new_tokens: Maximum tokens to generate
            end_token: Optional token ID to stop generation (e.g., '}' to complete JSON)
        
        Returns:
            Generated token sequence including input context
        """
        for _ in range(max_new_tokens):
            # Crop to block_size if sequence gets too long
            idx_cond = idx[:, -block_size:]
            
            # Get predictions for next token
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :]  # Only need the last position's predictions
            
            # Greedy: pick the highest probability token
            next_id = torch.argmax(logits, dim=-1, keepdim=True)
            
            # Append to sequence
            idx = torch.cat((idx, next_id), dim=1)
            
            # Stop if we hit the end token
            if end_token is not None and next_id.item() == end_token:
                break
        return idx

# =============================================================================
# TRAINING
# =============================================================================
# Initialize model
model = TinyTransformer()

# AdamW optimizer with weight decay (helps prevent overfitting)
optimizer = torch.optim.AdamW(model.parameters(), lr=lr)

print('Training...')
for it in range(max_iters + 1):
    # Get a batch of training data
    xb, yb = get_batch('train')
    
    # Forward pass: compute predictions and loss
    logits, loss = model(xb, yb)
    
    # Backward pass: compute gradients
    optimizer.zero_grad(set_to_none=True)  # Clear old gradients
    loss.backward()                         # Compute new gradients
    
    # Gradient clipping: prevent exploding gradients
    torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
    
    # Update weights
    optimizer.step()
    
    # Log progress
    if it % 2000 == 0:
        print(f'Step {it}: loss = {loss.item():.4f}')

# =============================================================================
# SAVE MODEL
# =============================================================================
os.makedirs('model_output', exist_ok=True)

# Save PyTorch state dict (for loading in Python)
torch.save(model.state_dict(), 'model_output/tiny_transformer.pt')

# Save vocabulary and config as JSON (for inference in other languages)
vocab_data = {
    'stoi': stoi,                                    # Character to ID mapping
    'itos': {int(k): v for k, v in itos.items()},   # ID to character mapping
    'vocab_size': vocab_size,
    'block_size': block_size,
    'n_embd': n_embd,
    'n_layer': n_layer,
    'n_head': n_head
}
with open('model_output/vocab.json', 'w') as f:
    json.dump(vocab_data, f)

# =============================================================================
# TEST THE MODEL
# =============================================================================
# Get the token ID for '}' to use as generation stop token
stop_token = stoi.get('}', None)

# Switch to evaluation mode (disables dropout)
model.eval()
print('\nTesting:')

# Test various command types
test_cases = [
    'show alert Hello',           # Simple alert
    'alert Test',                 # Short alert syntax
    'show alert Hello world',     # Multi-word message
    'navigate to settings',       # Navigation command
    'hello',                      # Unrecognized input
    'hi'                          # Unrecognized input
]

for test in test_cases:
    # Create the input prompt
    prompt = f'INPUT: {test} OUTPUT: '
    
    # Encode prompt to token IDs
    context = torch.tensor([[stoi[c] for c in prompt]], dtype=torch.long)
    
    # Generate response (stop when we complete the JSON with '}')
    output = model.generate_greedy(context, max_new_tokens=80, end_token=stop_token)[0].tolist()
    
    # Decode back to text
    result = decode(output)
    
    # Extract just the JSON output part
    output_part = result.split('OUTPUT: ')[-1] if 'OUTPUT: ' in result else result
    print(f'  "{test}" -> {output_part}')

# =============================================================================
# EXPORT FOR MOBILE APP
# =============================================================================
# Convert model weights to JSON for loading in mobile apps
# (Android/Kotlin and iOS/Swift don't have PyTorch, so we need raw weights)
weights = {name: param.tolist() for name, param in model.state_dict().items()}
with open('model_output/weights.json', 'w') as f:
    json.dump(weights, f)

# Copy to mobile app asset directories
shutil.copy('model_output/vocab.json', 'mobile-app/composeApp/src/androidMain/assets/vocab.json')
shutil.copy('model_output/weights.json', 'mobile-app/composeApp/src/androidMain/assets/weights.json')
shutil.copy('model_output/vocab.json', 'mobile-app/iosApp/iosApp/vocab.json')
shutil.copy('model_output/weights.json', 'mobile-app/iosApp/iosApp/weights.json')

print(f'\nModel params: {sum(p.numel() for p in model.parameters())/1e6:.2f}M')
print('Saved!')