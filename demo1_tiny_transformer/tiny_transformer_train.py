import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.onnx
import json
import math
import random
import os

block_size = 128
batch_size = 32
n_layer = 2
n_head = 2
n_embd = 128
dropout = 0.1
max_iters = 3000
lr = 3e-3
device = 'cuda' if torch.cuda.is_available() else 'cpu'

MESSAGES = [
    "Hello", "Welcome", "Success", "Error", "Warning", "Please wait", "Done",
    "Thank you", "Goodbye", "Try again", "Loading", "Saving", "Deleted",
    "Updated", "Created", "Failed", "Processing", "Complete", "Ready", "Busy"
]

SCREENS = [
    "home", "settings", "profile", "dashboard", "login", "register",
    "details", "list", "search", "about", "help", "notifications"
]

SETTINGS = [
    "dark_mode", "notifications", "sound", "vibration", "auto_update",
    "location", "bluetooth", "wifi", "data_saver", "battery_saver"
]

TEMPLATES = [
    ("show alert with message {msg}", lambda m: {"action": "alert", "message": m}),
    ("display an alert saying {msg}", lambda m: {"action": "alert", "message": m}),
    ("pop up a message {msg}", lambda m: {"action": "alert", "message": m}),
    ("alert the user with {msg}", lambda m: {"action": "alert", "message": m}),
    ("show a popup {msg}", lambda m: {"action": "alert", "message": m}),
    
    ("navigate to {screen}", lambda s: {"action": "navigate", "target": s}),
    ("go to {screen} screen", lambda s: {"action": "navigate", "target": s}),
    ("open {screen}", lambda s: {"action": "navigate", "target": s}),
    ("take me to {screen}", lambda s: {"action": "navigate", "target": s}),
    ("switch to {screen} page", lambda s: {"action": "navigate", "target": s}),
    
    ("toggle {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("switch {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("turn on {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("enable {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("disable {setting}", lambda s: {"action": "toggle", "setting": s}),
    ("turn off {setting}", lambda s: {"action": "toggle", "setting": s}),
    
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
    template, builder = random.choice(TEMPLATES)
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
        text = template
        result = builder()
    return text, json.dumps(result, separators=(',', ': '))

def generate_training_data(n_samples=2000):
    samples = []
    for _ in range(n_samples):
        text, json_out = generate_sample()
        combined = f"INPUT: {text} OUTPUT: {json_out}<|endoftext|>"
        samples.append(combined)
    return "\n".join(samples)

print("Generating synthetic training data...")
training_text = generate_training_data(3000)
print(f"Generated {len(training_text)} characters of training data")

chars = sorted(list(set(training_text)))
vocab_size = len(chars)
stoi = {ch:i for i,ch in enumerate(chars)}
itos = {i:ch for ch,i in stoi.items()}

encode = lambda s: torch.tensor([stoi[c] for c in s], dtype=torch.long)
decode = lambda t: ''.join([itos[int(i)] for i in t])

data = torch.tensor(encode(training_text), dtype=torch.long)
n = int(0.9 * len(data))
train_data = data[:n]
val_data = data[n:]

def get_batch(split):
    data = train_data if split == 'train' else val_data
    ix = torch.randint(len(data) - block_size, (batch_size,))
    x = torch.stack([data[i:i+block_size] for i in ix])
    y = torch.stack([data[i+1:i+block_size+1] for i in ix])
    return x.to(device), y.to(device)

class CausalSelfAttention(nn.Module):
    def __init__(self, n_embd, n_head, dropout):
        super().__init__()
        self.n_head = n_head
        self.key = nn.Linear(n_embd, n_embd, bias=False)
        self.query = nn.Linear(n_embd, n_embd, bias=False)
        self.value = nn.Linear(n_embd, n_embd, bias=False)
        self.proj = nn.Linear(n_embd, n_embd, bias=False)
        self.attn_drop = nn.Dropout(dropout)
        self.resid_drop = nn.Dropout(dropout)
        self.register_buffer('mask', torch.tril(torch.ones(block_size, block_size))
                             .view(1,1,block_size,block_size))

    def forward(self, x):
        B, T, C = x.size()
        k = self.key(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        q = self.query(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        v = self.value(x).view(B, T, self.n_head, C//self.n_head).transpose(1,2)
        att = (q @ k.transpose(-2, -1)) / math.sqrt(k.size(-1))
        att = att.masked_fill(self.mask[:,:,:T,:T]==0, float('-inf'))
        att = F.softmax(att, dim=-1)
        att = self.attn_drop(att)
        y = att @ v
        y = y.transpose(1,2).contiguous().view(B, T, C)
        return self.resid_drop(self.proj(y))

class MLP(nn.Module):
    def __init__(self, n_embd, dropout):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_embd, 4*n_embd),
            nn.GELU(),
            nn.Linear(4*n_embd, n_embd),
            nn.Dropout(dropout),
        )
    def forward(self, x): return self.net(x)

class Block(nn.Module):
    def __init__(self, n_embd, n_head, dropout):
        super().__init__()
        self.ln1 = nn.LayerNorm(n_embd)
        self.attn = CausalSelfAttention(n_embd, n_head, dropout)
        self.ln2 = nn.LayerNorm(n_embd)
        self.mlp = MLP(n_embd, dropout)
    def forward(self, x):
        x = x + self.attn(self.ln1(x))
        x = x + self.mlp(self.ln2(x))
        return x

class TinyTransformer(nn.Module):
    def __init__(self):
        super().__init__()
        self.tok_emb = nn.Embedding(vocab_size, n_embd)
        self.pos_emb = nn.Embedding(block_size, n_embd)
        self.drop = nn.Dropout(dropout)
        self.blocks = nn.Sequential(*[Block(n_embd, n_head, dropout) for _ in range(n_layer)])
        self.ln_f = nn.LayerNorm(n_embd)
        self.head = nn.Linear(n_embd, vocab_size, bias=False)
        self.head.weight = self.tok_emb.weight

    def forward(self, idx, targets=None):
        B, T = idx.shape
        tok = self.tok_emb(idx)
        pos = self.pos_emb(torch.arange(T, device=idx.device))
        x = self.drop(tok + pos)
        x = self.blocks(x)
        x = self.ln_f(x)
        logits = self.head(x)
        loss = None
        if targets is not None:
            loss = F.cross_entropy(logits.view(-1, logits.size(-1)), targets.view(-1))
        return logits, loss

    @torch.no_grad()
    def generate(self, idx, max_new_tokens):
        for _ in range(max_new_tokens):
            idx_cond = idx[:, -block_size:]
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :]
            probs = F.softmax(logits, dim=-1)
            next_id = torch.multinomial(probs, num_samples=1)
            idx = torch.cat((idx, next_id), dim=1)
        return idx

model = TinyTransformer().to(device)
optimizer = torch.optim.AdamW(model.parameters(), lr=lr)

print("Training tiny transformer...")
for it in range(max_iters+1):
    xb, yb = get_batch('train')
    logits, loss = model(xb, yb)
    optimizer.zero_grad(set_to_none=True)
    loss.backward()
    torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
    optimizer.step()
    if it % 500 == 0:
        print(f"Step {it}: loss = {loss.item():.4f}")

os.makedirs('model_output', exist_ok=True)
torch.save(model.state_dict(), 'model_output/tiny_transformer.pt')

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
    context = encode(prompt).unsqueeze(0).to(device)
    output = model.generate(context, max_new_tokens=50)[0].tolist()
    result = decode(output)
    if '<|endoftext|>' in result:
        result = result.split('<|endoftext|>')[0]
    print(f"  '{test}' -> {result.split('OUTPUT: ')[-1]}")

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
    opset_version=14
)
print("ONNX model exported to model_output/tiny_transformer.onnx")

print("\nExporting model weights as JSON for mobile...")
weights = {}
for name, param in model.state_dict().items():
    weights[name] = param.cpu().tolist()
with open('model_output/weights.json', 'w') as f:
    json.dump(weights, f)
print("Weights exported to model_output/weights.json")

import shutil
assets_dir = 'mobile-app/composeApp/src/androidMain/assets'
os.makedirs(assets_dir, exist_ok=True)
shutil.copy('model_output/vocab.json', f'{assets_dir}/vocab.json')
shutil.copy('model_output/weights.json', f'{assets_dir}/weights.json')
print(f"Model files copied to {assets_dir}")