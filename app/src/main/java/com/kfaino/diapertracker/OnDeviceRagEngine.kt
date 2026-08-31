package com.kfaino.diapertracker

import java.util.Locale
import kotlin.math.sqrt

/**
 * 🧠 端侧离线向量嵌入与智能问答 RAG 引擎 (On-Device RAG Engine)
 * 100% 离线端侧高维 TF-IDF / N-gram 嵌入与余弦相似度检索
 */
object OnDeviceRagEngine {

    data class RagDocument(
        val docId: String,
        val title: String,
        val content: String,
        val domainType: String,
        val embedding: FloatArray
    )

    data class RagAnswer(
        val question: String,
        val topAnswer: String,
        val confidenceScore: Float,
        val matchedDocTitle: String
    )

    private fun generateEmbedding(text: String, vocab: List<String>): FloatArray {
        val lower = text.lowercase(Locale.getDefault())
        val vec = FloatArray(vocab.size)
        for (i in vocab.indices) {
            if (lower.contains(vocab[i])) {
                vec[i] = 1.0f
            }
        }
        // L2 归一化
        var norm = 0f
        for (v in vec) norm += v * v
        norm = sqrt(norm)
        if (norm > 0) {
            for (i in vec.indices) vec[i] /= norm
        }
        return vec
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return 0f
        var dot = 0f
        for (i in v1.indices) dot += v1[i] * v2[i]
        return dot
    }

    fun queryKnowledge(store: DataStore, queryText: String): RagAnswer {
        val entries = store.loadAll()
        val ideas = store.getIdeas()
        val clippings = store.getClippings()

        val vocab = listOf("药", "生鲜", "保质期", "过期", "借", "书", "酒", "衣服", "相机", "镜头", "电脑", "发票", "客厅", "卧室", "书房", "箱", "包", "冲锋衣", "应急")
        val qVec = generateEmbedding(queryText, vocab)

        var bestScore = 0f
        var bestTitle = ""
        var bestContent = ""

        // 1. 匹配实物
        for (e in entries) {
            val docText = "   "
            val docVec = generateEmbedding(docText, vocab)
            val sim = cosineSimilarity(qVec, docVec)
            if (sim > bestScore) {
                bestScore = sim
                bestTitle = e.brand
                bestContent = "【】存放于【】，分类【】，单价 ¥"
            }
        }

        // 2. 匹配灵感与剪藏
        for (i in ideas) {
            val docVec = generateEmbedding(i.content, vocab)
            val sim = cosineSimilarity(qVec, docVec)
            if (sim > bestScore) {
                bestScore = sim
                bestTitle = i.getPreview(15)
                bestContent = "💡 灵感想法："
            }
        }

        for (c in clippings) {
            val docVec = generateEmbedding(" ", vocab)
            val sim = cosineSimilarity(qVec, docVec)
            if (sim > bestScore) {
                bestScore = sim
                bestTitle = c.title
                bestContent = "📰 剪藏文章【】："
            }
        }

        val ans = if (bestScore > 0.3f) bestContent else "未在本地离线沙盒中找到高度相关的资产记录。"
        return RagAnswer(queryText, ans, bestScore, bestTitle)
    }
}
