"""
Tiny Transformer Training Script
================================
This script trains a miniature GPT-style transformer from scratch.

Model Size: ~20,000 parameters (~80KB on disk)
Training Time: 2-3 minutes on CPU
Purpose: Demonstrate that transformers are just code, not magic

What it learns:
- Parse natural language commands like "show alert with message Hello"
- Output structured JSON: {"action": "alert", "message": "Hello"}
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.onnx
import json
import math
import random
import os

# ============================================================================
# HYPERPARAMETERS
# ============================================================================
# These control model size and training behavior.
# Smaller values = faster training but less capacity.

block_size = 64      # Maximum sequence length (input + output tokens)
batch_size = 16      # Number of sequences processed in parallel
n_layer = 3          # Number of transformer layers (GPT-4 has 100+)
n_head = 2           # Number of attention heads per layer
n_embd = 96          # Embedding dimension (GPT-4 uses 12288)
dropout = 0.1        # Regularization to prevent overfitting
max_iters = 8000     # Total training iterations
lr = 3e-3            # Learning rate - how fast the model learns
eval_interval = 1000 # How often to print progress
device = 'cuda' if torch.cuda.is_available() else 'cpu'

# ============================================================================
# TRAINING DATA
# ============================================================================
# We generate synthetic training data from templates.
# Each sample pairs a natural language command with its JSON output.

# Pool of message strings for alert commands
MESSAGES_SINGLE = [
    "Hello", "Welcome", "Success", "Error", "Warning", "Done",
    "Thank you", "Goodbye", "Try again", "Loading", "Saving", "Deleted",
    "Updated", "Created", "Failed", "Processing", "Complete", "Ready", "Busy"
]

MESSAGES_MULTI = [
    "Please wait", "File saved", "Connection lost", "Login successful",
    "Operation failed", "Item deleted", "Changes saved", "Task completed",
    "No results found", "Server error", "Access denied", "Invalid input",
    "Welcome back", "Good morning", "See you later", "Happy coding",
    "Hello world", "Test passed", "Build failed", "Sync complete"
]

MESSAGES = MESSAGES_SINGLE + MESSAGES_MULTI

# Pool of screen names for navigation commands
SCREENS = [
    "home", "settings", "profile", "dashboard", "login", "register",
    "details", "list", "search", "about", "help", "notifications"
]

# Pool of settings for toggle commands
SETTINGS = [
    "dark_mode", "notifications", "sound", "vibration", "auto_update",
    "location", "bluetooth", "wifi", "data_saver", "battery_saver"
]

# Template patterns: (pattern, output_builder_function)
# The model learns to map each pattern to its structured output.
TEMPLATES = [
    # Alert commands - show messages to users
    ("show alert with message {msg}", lambda m: {"action": "alert", "message": m}),
    ("display an alert saying {msg}", lambda m: {"action": "alert", "message": m}),
    ("pop up a message {msg}", lambda m: {"action": "alert", "message": m}),
    ("alert the user with {msg}", lambda m: {"action": "alert", "message": m}),
    ("show a popup {msg}", lambda m: {"action": "alert", "message": m}),
    
    # Navigation commands - move between screens
    ("navigate to {screen}", lambda s: {"action": "navigate", "target": s}),
    ("go to {screen} screen", lambda s: {"action": "navigate", "target": s}),
    ("open {screen}", lambda s: {"action": "navigate", "target": s}),
    ("take me to {screen}", lambda s: {"action": "navigate", "target": s}),
    ("switch to {screen} page", lambda s: {"action": "navigate", "target": s}),
    
    # Toggle commands - change settings
    ("toggle {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("switch {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("turn on {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("enable {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("disable {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("turn off {setting}", lambda s: {"action": "toggle", "setting": s}),
    
    # System commands - no parameters needed
    ("refresh the page", lambda: {"action": "refresh"}),
    ("reload", lambda: {"action": "refresh"}),
    ("refresh screen", lambda: {"action": "refresh"}),
    
    ("go back", lambda: {"action": "back"}),
    ("navigate back", lambda: {"action": "back"}),
    ("return to previous", lambda: {"action": "back"}),
    
    ("close the app", lambda: {"action": "close"}),
    ("exit application", lambda: {"action": "close"}),
    ("quit", lambda: {"action": "close"}),
]

def generate_sample():
    """
    Generate a single training example.
    Picks a random template and fills in placeholders with random values.
    Returns: (input_text, json_output)
    """
    template, builder = random.choice(TEMPLATES)
    
    # Fill in placeholders based on template type
    if "{msg}" in template:
        msg = random.choice(MESSAGES)
        text = template.format(msg=msg)
        result = builder(msg)
    elif "{screen}" in template:
        screen = random.choice(SCREENS)
        text = template.format(screen=screen)
        result = builder(screen)
    elif "{setting}" in template:
        setting = random.choice(SETTINGS)
        text = template.format(setting=setting)
        result = builder(setting)
    else:
        # No placeholders - use template as-is
        text = template
        result = builder()
    
    # Format as training example: INPUT: ... OUTPUT: {...}
    return text, json.dumps(result, separators=(',', ': '))

def generate_training_data(n_samples=2500):
    """Generate N training samples as a single text corpus."""
    samples = []
    for _ in range(n_samples):
        text, json_out = generate_sample()
        combined = f"INPUT: {text} OUTPUT: {json_out}"
        samples.append(combined)
    return "\n".join(samples)

# ============================================================================
# TOKENIZATION
# ============================================================================
# Convert text to numbers that the model can process.
# This is character-level tokenization (simple but works for small vocab).

print("Generating synthetic training data...")
training_text = generate_training_data(2500)
print(f"Generated {len(training_text)} characters of training data")

# Build vocabulary: map each unique character to an integer ID
chars = sorted(list(set(training_text)))
vocab_size = len(chars)
stoi = {ch:i for i,ch in enumerate(chars)}  # string to integer
itos = {i:ch for ch,i in stoi.items()}      # integer to string

# Encoder/decoder functions
encode = lambda s: [stoi[c] for c in s]      # text -> list of IDs
decode = lambda t: ''.join([itos[int(i)] for i in t])  # IDs -> text

# Convert entire training corpus to tensor of token IDs
data = torch.tensor(encode(training_text), dtype=torch.long)

# Split into training (90%) and validation (10%) sets
n = int(0.9 * len(data))
train_data = data[:n]
val_data = data[n:]

def get_batch(split):
    """
    Get a batch of training examples.
    Returns input sequences (x) and target sequences (y).
    For language modeling: y is x shifted by 1 token (predict next token).
    """
    data = train_data if split == 'train' else val_data
    
    # Pick random starting positions
    ix = torch.randint(len(data) - block_size, (batch_size,))
    
    # Extract sequences
    x = torch.stack([data[i:i+block_size] for i in ix])
    y = torch.stack([data[i+1:i+block_size+1] for i in ix])
    
    return x.to(device), y.to(device)

# ============================================================================
# MODEL ARCHITECTURE
# ============================================================================
# A transformer is made of stacked "blocks", each containing:
# 1. Self-attention: words look at other words for context
# 2. Feed-forward network: process the information
# 3. Layer normalization: keep values stable

class CausalSelfAttention(nn.Module):
    """
    Self-attention mechanism - the core of transformers.
    
    Each token can "attend" to all previous tokens (not future ones,
    hence "causal" - we can't peek ahead during generation).
    
    Process:
    1. Project input to Query, Key, Value vectors
    2. Compute attention scores: Q @ K^T
    3. Apply mask to prevent looking at future tokens
    4. Softmax to get attention weights
    5. Multiply by Value vectors
    """
    def __init__(self, n_embd, n_head, dropout):
        super().__init__()
        self.n_head = n_head
        
        # Linear projections for Q, K, V
        self.key = nn.Linear(n_embd, n_embd, bias=False)
        self.query = nn.Linear(n_embd, n_embd, bias=False)
        self.value = nn.Linear(n_embd, n_embd, bias=False)
        
        # Output projection
        self.proj = nn.Linear(n_embd, n_embd, bias=False)
        
        # Regularization
        self.attn_drop = nn.Dropout(dropout)
        self.resid_drop = nn.Dropout(dropout)
        
        # Causal mask: lower triangular matrix
        # 1 = can attend, 0 = cannot attend
        self.register_buffer('mask', torch.tril(torch.ones(block_size, block_size))
                             .view(1,1,block_size,block_size))

    def forward(self, x):
        B, T, C = x.size()  # Batch, Time, Channels
        
        # Compute Q, K, V and reshape for multi-head attention
        k = self.key(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        q = self.query(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        v = self.value(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        
        # Attention scores: scaled dot-product
        att = (q @ k.transpose(-2, -1)) / math.sqrt(k.size(-1))
        
        # Apply causal mask (prevent attending to future)
        att = att.masked_fill(self.mask[:,:,:T,:T]==0, float('-inf'))
        
        # Convert to probabilities
        att = F.softmax(att, dim=-1)
        att = self.attn_drop(att)
        
        # Apply attention to values
        y = att @ v
        
        # Reshape back and project
        y = y.transpose(1,2).contiguous().view(B, T, C)
        return self.resid_drop(self.proj(y))

class MLP(nn.Module):
    """
    Feed-forward network - processes attention output.
    
    Simple two-layer network with GELU activation.
    Expands dimension 4x then projects back.
    """
    def __init__(self, n_embd, dropout):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_embd, 4*n_embd),   # Expand
            nn.GELU(),                      # Non-linearity
            nn.Linear(4*n_embd, n_embd),   # Project back
            nn.Dropout(dropout),            # Regularization
        )
    def forward(self, x): return self.net(x)

class Block(nn.Module):
    """
    Transformer block - combines attention and feed-forward.
    
    Architecture:
    x -> LayerNorm -> Attention -> + -> LayerNorm -> MLP -> +
    \________________________________________________________/
                         (residual connection)
    
    Residual connections help gradients flow through deep networks.
    """
    def __init__(self, n_embd, n_head, dropout):
        super().__init__()
        self.ln1 = nn.LayerNorm(n_embd)
        self.attn = CausalSelfAttention(n_embd, n_head, dropout)
        self.ln2 = nn.LayerNorm(n_embd)
        self.mlp = MLP(n_embd, dropout)
    
    def forward(self, x):
        # Attention with residual
        x = x + self.attn(self.ln1(x))
        # MLP with residual
        x = x + self.mlp(self.ln2(x))
        return x

class TinyTransformer(nn.Module):
    """
    Complete transformer model for language modeling.
    
    Components:
    1. Token embeddings: convert token IDs to vectors
    2. Position embeddings: add position information
    3. Transformer blocks: process the sequence
    4. Output head: predict next token
    
    Weight tying: shares weights between input embeddings and output head.
    This reduces parameters and improves generalization.
    """
    def __init__(self):
        super().__init__()
        # Embeddings
        self.tok_emb = nn.Embedding(vocab_size, n_embd)    # Token -> vector
        self.pos_emb = nn.Embedding(block_size, n_embd)    # Position -> vector
        self.drop = nn.Dropout(dropout)
        
        # Transformer blocks
        self.blocks = nn.Sequential(*[Block(n_embd, n_head, dropout) for _ in range(n_layer)])
        
        # Output
        self.ln_f = nn.LayerNorm(n_embd)                    # Final normalization
        self.head = nn.Linear(n_embd, vocab_size, bias=False)  # Predict next token
        
        # Weight tying: output head shares weights with token embeddings
        self.head.weight = self.tok_emb.weight

    def forward(self, idx, targets=None):
        """
        Forward pass.
        
        Args:
            idx: Input token IDs, shape (batch, sequence_length)
            targets: Target token IDs for loss computation
        
        Returns:
            logits: Predicted token probabilities, shape (batch, seq, vocab)
            loss: Cross-entropy loss if targets provided
        """
        B, T = idx.shape
        
        # Get embeddings
        tok = self.tok_emb(idx)                             # Token embeddings
        pos = self.pos_emb(torch.arange(T, device=idx.device))  # Position embeddings
        x = self.drop(tok + pos)                            # Combine and dropout
        
        # Process through transformer blocks
        x = self.blocks(x)
        
        # Final layer norm and prediction
        x = self.ln_f(x)
        logits = self.head(x)
        
        # Compute loss if targets provided
        loss = None
        if targets is not None:
            # Cross-entropy: how well do we predict the next token?
            loss = F.cross_entropy(logits.view(-1, logits.size(-1)), targets.view(-1))
        
        return logits, loss

    @torch.no_grad()
    def generate(self, idx, max_new_tokens, stop_token=None):
        """
        Generate text by sampling from the model.
        
        Process:
        1. Get predictions for next token
        2. Sample from probability distribution
        3. Append to sequence
        4. Repeat until max_tokens or stop_token
        """
        for _ in range(max_new_tokens):
            # Only use last block_size tokens (context limit)
            idx_cond = idx[:, -block_size:]
            
            # Get predictions
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :]  # Only need last token's prediction
            
            # Convert to probabilities and sample
            probs = F.softmax(logits, dim=-1)
            next_id = torch.multinomial(probs, num_samples=1)
            
            # Append to sequence
            idx = torch.cat((idx, next_id), dim=1)
            
            # Stop if we hit the stop token (e.g., '}')
            if stop_token is not None and next_id.item() == stop_token:
                break
        
        return idx
    
    @torch.no_grad()
    def generate_greedy(self, idx, max_new_tokens, stop_token=None):
        """
        Generate text using greedy decoding.
        
        Instead of sampling, always pick the most likely token.
        More deterministic, good for testing.
        """
        for _ in range(max_new_tokens):
            idx_cond = idx[:, -block_size:]
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :]
            
            # Greedy: pick the highest probability token
            next_id = torch.argmax(logits, dim=-1, keepdim=True)
            
            idx = torch.cat((idx, next_id), dim=1)
            
            if stop_token is not None and next_id.item() == stop_token:
                break
        
        return idx

# ============================================================================
# TRAINING LOOP
# ============================================================================
# Standard PyTorch training: forward pass, compute loss, backward pass, update.

model = TinyTransformer().to(device)
optimizer = torch.optim.AdamW(model.parameters(), lr=lr)

print("Training tiny transformer...")
for it in range(max_iters+1):
    # Get training batch
    xb, yb = get_batch('train')
    
    # Forward pass
    logits, loss = model(xb, yb)
    
    # Backward pass
    optimizer.zero_grad(set_to_none=True)  # Clear gradients
    loss.backward()                          # Compute gradients
    
    # Gradient clipping: prevent exploding gradients
    torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
    
    # Update weights
    optimizer.step()
    
    # Periodically evaluate on validation set
    if it % eval_interval == 0:
        model.eval()
        val_loss = 0.0
        with torch.no_grad():
            for _ in range(10):
                xv, yv = get_batch('val')
                _, vloss = model(xv, yv)
                val_loss += vloss.item()
            val_loss /= 10
        model.train()
        print(f"Step {it}: train_loss = {loss.item():.4f}, val_loss = {val_loss:.4f}")

# ============================================================================
# SAVE MODEL
# ============================================================================

os.makedirs('model_output', exist_ok=True)
torch.save(model.state_dict(), 'model_output/tiny_transformer.pt')

# Save vocabulary and model config for inference
vocab_data = {
    'stoi': stoi,
    'itos': {int(k): v for k, v in itos.items()},
    'vocab_size': vocab_size,
    'block_size': block_size,
    'n_embd': n_embd,
    'n_layer': n_layer,
    'n_head': n_head
}
with open('model_output/vocab.json', 'w') as f:
    json.dump(vocab_data, f)

print(f"Model saved! Size: {sum(p.numel() for p in model.parameters())/1e6:.2f}M parameters")
print(f"Vocab size: {vocab_size}")

# ============================================================================
# TEST INFERENCE
# ============================================================================

stop_token = stoi.get('}', None)
print(f"Stop token '}}' ID: {stop_token}")

print("\nTesting inference:")
test_inputs = [
    "show alert with message Hello",
    "navigate to settings",
    "toggle dark_mode",
    "go back",
    "refresh the page"
]

model.eval()
for test in test_inputs:
    prompt = f"INPUT: {test} OUTPUT: "
    context = torch.tensor([encode(prompt)], dtype=torch.long, device=device)
    output = model.generate_greedy(context, max_new_tokens=50, stop_token=stop_token)[0].tolist()
    result = decode(output)
    if '}' in result:
        result = result[:result.rfind('}')+1]
    output_part = result.split('OUTPUT: ')[-1] if 'OUTPUT: ' in result else result
    print(f"  '{test}' -> {output_part}")

# ============================================================================
# EXPORT FOR MOBILE
# ============================================================================

# Export to ONNX format (for frameworks that support it)
print("\nExporting to ONNX format...")
dummy_input = torch.randint(0, vocab_size, (1, block_size), dtype=torch.long, device=device)
model.eval()
torch.onnx.export(
    model,
    (dummy_input,),
    'model_output/tiny_transformer.onnx',
    input_names=['input_ids'],
    output_names=['logits'],
    dynamic_axes={'input_ids': {0: 'batch', 1: 'seq'}, 'logits': {0: 'batch', 1: 'seq'}},
    do_constant_folding=True,
    opset_version=18
)
print("ONNX model exported to model_output/tiny_transformer.onnx")

# Export weights as JSON for Kotlin/Native inference
# This allows us to load the model in mobile apps without PyTorch
print("\nExporting model weights as JSON for mobile...")
weights = {}
for name, param in model.state_dict().items():
    weights[name] = param.cpu().tolist()
with open('model_output/weights.json', 'w') as f:
    json.dump(weights, f)
print("Weights exported to model_output/weights.json")

# Copy to Android assets directory
import shutil
assets_dir = 'mobile-app/composeApp/src/androidMain/assets'
os.makedirs(assets_dir, exist_ok=True)
shutil.copy('model_output/vocab.json', f'{assets_dir}/vocab.json')
shutil.copy('model_output/weights.json', f'{assets_dir}/weights.json')
print(f"Model files copied to {assets_dir}")