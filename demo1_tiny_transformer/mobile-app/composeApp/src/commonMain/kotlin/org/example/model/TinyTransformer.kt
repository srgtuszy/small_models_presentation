package org.example.model

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class ModelConfig(
    val vocabSize: Int,
    val blockSize: Int,
    val nEmbd: Int,
    val nLayer: Int,
    val nHead: Int
)

class TinyTransformer(private val config: ModelConfig) {
    private val tokenEmbedding = Array(config.vocabSize) { FloatArray(config.nEmbd) }
    private val positionEmbedding = Array(config.blockSize) { FloatArray(config.nEmbd) }
    private val blocks = Array(config.nLayer) { TransformerBlock(config) }
    private val lnF = LayerNorm(config.nEmbd)
    private val headWeight = Array(config.vocabSize) { FloatArray(config.nEmbd) }
    
    private var mask: Array<FloatArray>? = null
    
    fun loadWeights(weights: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val tokEmb = weights["tok_emb.weight"] as? List<List<Double>> ?: return
        for (i in tokEmb.indices) {
            for (j in tokEmb[i].indices) {
                tokenEmbedding[i][j] = tokEmb[i][j].toFloat()
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        val posEmb = weights["pos_emb.weight"] as? List<List<Double>> ?: return
        for (i in posEmb.indices) {
            for (j in posEmb[i].indices) {
                positionEmbedding[i][j] = posEmb[i][j].toFloat()
            }
        }
        
        for (layerIdx in 0 until config.nLayer) {
            val block = blocks[layerIdx]
            val prefix = "blocks.$layerIdx"
            
            @Suppress("UNCHECKED_CAST")
            val ln1Weight = weights["$prefix.ln1.weight"] as? List<Double> ?: return
            @Suppress("UNCHECKED_CAST")
            val ln1Bias = weights["$prefix.ln1.bias"] as? List<Double> ?: return
            for (i in ln1Weight.indices) {
                block.ln1.gamma[i] = ln1Weight[i].toFloat()
                block.ln1.beta[i] = ln1Bias[i].toFloat()
            }
            
            @Suppress("UNCHECKED_CAST")
            val keyWeight = weights["$prefix.attn.key.weight"] as? List<List<Double>> ?: return
            @Suppress("UNCHECKED_CAST")
            val queryWeight = weights["$prefix.attn.query.weight"] as? List<List<Double>> ?: return
            @Suppress("UNCHECKED_CAST")
            val valueWeight = weights["$prefix.attn.value.weight"] as? List<List<Double>> ?: return
            @Suppress("UNCHECKED_CAST")
            val projWeight = weights["$prefix.attn.proj.weight"] as? List<List<Double>> ?: return
            
            for (i in keyWeight.indices) {
                for (j in keyWeight[i].indices) {
                    block.attn.key.weight[i][j] = keyWeight[i][j].toFloat()
                    block.attn.query.weight[i][j] = queryWeight[i][j].toFloat()
                    block.attn.value.weight[i][j] = valueWeight[i][j].toFloat()
                    block.attn.proj.weight[i][j] = projWeight[i][j].toFloat()
                }
            }
            
            @Suppress("UNCHECKED_CAST")
            val ln2Weight = weights["$prefix.ln2.weight"] as? List<Double> ?: return
            @Suppress("UNCHECKED_CAST")
            val ln2Bias = weights["$prefix.ln2.bias"] as? List<Double> ?: return
            for (i in ln2Weight.indices) {
                block.ln2.gamma[i] = ln2Weight[i].toFloat()
                block.ln2.beta[i] = ln2Bias[i].toFloat()
            }
            
            @Suppress("UNCHECKED_CAST")
            val mlpFc1Weight = weights["$prefix.mlp.net.0.weight"] as? List<List<Double>> ?: return
            @Suppress("UNCHECKED_CAST")
            val mlpFc1Bias = weights["$prefix.mlp.net.0.bias"] as? List<Double> ?: return
            @Suppress("UNCHECKED_CAST")
            val mlpFc2Weight = weights["$prefix.mlp.net.2.weight"] as? List<List<Double>> ?: return
            @Suppress("UNCHECKED_CAST")
            val mlpFc2Bias = weights["$prefix.mlp.net.2.bias"] as? List<Double> ?: return
            
            for (i in mlpFc1Weight.indices) {
                block.mlp.fc1.bias[i] = mlpFc1Bias[i].toFloat()
                for (j in mlpFc1Weight[i].indices) {
                    block.mlp.fc1.weight[i][j] = mlpFc1Weight[i][j].toFloat()
                }
            }
            
            for (i in mlpFc2Weight.indices) {
                block.mlp.fc2.bias[i] = mlpFc2Bias[i].toFloat()
                for (j in mlpFc2Weight[i].indices) {
                    block.mlp.fc2.weight[i][j] = mlpFc2Weight[i][j].toFloat()
                }
            }
        }
        
        @Suppress("UNCHECKED_CAST")
        val lnFWeight = weights["ln_f.weight"] as? List<Double> ?: return
        @Suppress("UNCHECKED_CAST")
        val lnFBias = weights["ln_f.bias"] as? List<Double> ?: return
        for (i in lnFWeight.indices) {
            lnF.gamma[i] = lnFWeight[i].toFloat()
            lnF.beta[i] = lnFBias[i].toFloat()
        }
        
        for (i in tokEmb.indices) {
            for (j in tokEmb[i].indices) {
                headWeight[i][j] = tokEmb[i][j].toFloat()
            }
        }
        
        mask = createCausalMask(config.blockSize)
    }
    
    private fun createCausalMask(size: Int): Array<FloatArray> {
        val mask = Array(size) { FloatArray(size) }
        for (i in 0 until size) {
            for (j in 0 until size) {
                mask[i][j] = if (j <= i) 0f else Float.NEGATIVE_INFINITY
            }
        }
        return mask
    }
    
    fun forward(inputIds: IntArray): Array<FloatArray> {
        val seqLen = inputIds.size
        val x = Array(seqLen) { FloatArray(config.nEmbd) }
        
        for (i in inputIds.indices) {
            val tokenId = inputIds[i]
            for (j in 0 until config.nEmbd) {
                x[i][j] = tokenEmbedding[tokenId][j] + positionEmbedding[i][j]
            }
        }
        
        var hidden = x
        for (block in blocks) {
            hidden = block.forward(hidden, mask!!, seqLen)
        }
        
        hidden = lnF.forward(hidden)
        
        val logits = Array(seqLen) { FloatArray(config.vocabSize) }
        for (i in 0 until seqLen) {
            for (j in 0 until config.vocabSize) {
                var sum = 0f
                for (k in 0 until config.nEmbd) {
                    sum += hidden[i][k] * headWeight[j][k]
                }
                logits[i][j] = sum
            }
        }
        
        return logits
    }
    
    fun generate(inputIds: IntArray, maxNewTokens: Int, endToken: Int = -1): IntArray {
        val result = inputIds.toMutableList()
        
        for (_i in 0 until maxNewTokens) {
            val context = if (result.size > config.blockSize) {
                result.takeLast(config.blockSize).toIntArray()
            } else {
                result.toIntArray()
            }
            
            val logits = forward(context)
            val lastLogits = logits[logits.size - 1]
            val probs = softmax(lastLogits)
            
            val nextToken = sampleFromProbs(probs)
            result.add(nextToken)
            
            if (endToken >= 0 && nextToken == endToken) break
        }
        
        return result.toIntArray()
    }
    
    fun generateGreedy(inputIds: IntArray, maxNewTokens: Int, endToken: Int = -1): IntArray {
        val result = inputIds.toMutableList()
        
        for (_i in 0 until maxNewTokens) {
            val context = if (result.size > config.blockSize) {
                result.takeLast(config.blockSize).toIntArray()
            } else {
                result.toIntArray()
            }
            
            val logits = forward(context)
            val lastLogits = logits[logits.size - 1]
            
            var maxIdx = 0
            var maxVal = lastLogits[0]
            for (i in 1 until lastLogits.size) {
                if (lastLogits[i] > maxVal) {
                    maxVal = lastLogits[i]
                    maxIdx = i
                }
            }
            
            result.add(maxIdx)
            
            if (endToken >= 0 && maxIdx == endToken) break
        }
        
        return result.toIntArray()
    }
    
    private fun softmax(logits: FloatArray): FloatArray {
        val maxVal = logits.max()
        val exps = FloatArray(logits.size) { exp(logits[it] - maxVal) }
        val sum = exps.sum()
        return FloatArray(logits.size) { exps[it] / sum }
    }
    
    private fun sampleFromProbs(probs: FloatArray): Int {
        val rand = kotlin.random.Random.nextDouble()
        var cumSum = 0.0
        for (i in probs.indices) {
            cumSum += probs[i]
            if (rand < cumSum) return i
        }
        return probs.size - 1
    }
}

class TransformerBlock(config: ModelConfig) {
    val ln1 = LayerNorm(config.nEmbd)
    val attn = CausalSelfAttention(config)
    val ln2 = LayerNorm(config.nEmbd)
    val mlp = MLP(config)
    
    fun forward(x: Array<FloatArray>, mask: Array<FloatArray>, seqLen: Int): Array<FloatArray> {
        val attnOut = attn.forward(ln1.forward(x), mask, seqLen)
        val residual1 = Array(x.size) { i -> FloatArray(x[0].size) { j -> x[i][j] + attnOut[i][j] } }
        
        val mlpOut = mlp.forward(ln2.forward(residual1))
        return Array(x.size) { i -> FloatArray(x[0].size) { j -> residual1[i][j] + mlpOut[i][j] } }
    }
}

class CausalSelfAttention(config: ModelConfig) {
    val key = Linear(config.nEmbd, config.nEmbd)
    val query = Linear(config.nEmbd, config.nEmbd)
    val value = Linear(config.nEmbd, config.nEmbd)
    val proj = Linear(config.nEmbd, config.nEmbd)
    
    private val nHead = config.nHead
    private val headDim = config.nEmbd / config.nHead
    
    fun forward(x: Array<FloatArray>, mask: Array<FloatArray>, seqLen: Int): Array<FloatArray> {
        val k = key.forward(x)
        val q = query.forward(x)
        val v = value.forward(x)
        
        val batchSize = 1
        val seqLenActual = x.size
        val nEmbd = x[0].size
        
        val heads = Array(nHead) { Array(seqLenActual) { FloatArray(headDim) } }
        val outHeads = Array(nHead) { Array(seqLenActual) { FloatArray(headDim) } }
        
        for (h in 0 until nHead) {
            for (i in 0 until seqLenActual) {
                for (j in 0 until headDim) {
                    val kIdx = h * headDim + j
                    val qIdx = h * headDim + j
                    heads[h][i][j] = k[i][kIdx]
                }
            }
        }
        
        for (h in 0 until nHead) {
            val att = Array(seqLenActual) { FloatArray(seqLenActual) }
            for (i in 0 until seqLenActual) {
                for (j in 0 until seqLenActual) {
                    var score = 0f
                    for (d in 0 until headDim) {
                        val qVal = q[i][h * headDim + d]
                        val kVal = k[j][h * headDim + d]
                        score += qVal * kVal
                    }
                    att[i][j] = score / sqrt(headDim.toFloat()) + mask[i][j]
                }
            }
            
            for (i in 0 until seqLenActual) {
                val row = FloatArray(seqLenActual) { j -> 
                    val maxVal = att[i].max()
                    exp(att[i][j] - maxVal)
                }
                val sum = row.sum()
                for (j in 0 until seqLenActual) {
                    att[i][j] = row[j] / sum
                }
            }
            
            for (i in 0 until seqLenActual) {
                for (d in 0 until headDim) {
                    var sum = 0f
                    for (j in 0 until seqLenActual) {
                        sum += att[i][j] * v[j][h * headDim + d]
                    }
                    outHeads[h][i][d] = sum
                }
            }
        }
        
        val concat = Array(seqLenActual) { FloatArray(nEmbd) }
        for (i in 0 until seqLenActual) {
            for (h in 0 until nHead) {
                for (d in 0 until headDim) {
                    concat[i][h * headDim + d] = outHeads[h][i][d]
                }
            }
        }
        
        return proj.forward(concat)
    }
}

class MLP(config: ModelConfig) {
    val fc1 = Linear(config.nEmbd, 4 * config.nEmbd)
    val fc2 = Linear(4 * config.nEmbd, config.nEmbd)
    
    fun forward(x: Array<FloatArray>): Array<FloatArray> {
        val hidden = fc1.forward(x)
        val activated = Array(hidden.size) { i ->
            FloatArray(hidden[0].size) { j -> gelu(hidden[i][j]) }
        }
        return fc2.forward(activated)
    }
    
    private fun gelu(x: Float): Float {
        return (0.5f * x * (1f + kotlin.math.tanh(sqrt(2f / kotlin.math.PI.toFloat()) * (x + 0.044715f * x * x * x))))
    }
}

class LayerNorm(private val nEmbd: Int) {
    var gamma = FloatArray(nEmbd) { 1f }
    var beta = FloatArray(nEmbd) { 0f }
    
    fun forward(x: Array<FloatArray>): Array<FloatArray> {
        val result = Array(x.size) { FloatArray(nEmbd) }
        for (i in x.indices) {
            val mean = x[i].average().toFloat()
            val variance = x[i].map { (it - mean) * (it - mean) }.average().toFloat()
            val std = sqrt(variance + 1e-5f)
            for (j in 0 until nEmbd) {
                result[i][j] = gamma[j] * (x[i][j] - mean) / std + beta[j]
            }
        }
        return result
    }
}

class Linear(private val inFeatures: Int, private val outFeatures: Int) {
    var weight = Array(outFeatures) { FloatArray(inFeatures) }
    var bias = FloatArray(outFeatures)
    
    fun forward(x: Array<FloatArray>): Array<FloatArray> {
        return Array(x.size) { i ->
            FloatArray(outFeatures) { j ->
                var sum = bias[j]
                for (k in 0 until inFeatures) {
                    sum += x[i][k] * weight[j][k]
                }
                sum
            }
        }
    }
}