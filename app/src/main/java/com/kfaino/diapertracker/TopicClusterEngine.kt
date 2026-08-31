package com.kfaino.diapertracker

/**
 * 🕸️ 灵感闪念与剪藏文章自组织主题网络聚类引擎 (Topic Cluster Engine)
 */
object TopicClusterEngine {

    data class TopicCluster(
        val topicName: String,
        val ideaCount: Int,
        val clippingCount: Int,
        val memberTitles: List<String>
    )

    fun clusterKnowledge(store: DataStore): List<TopicCluster> {
        val ideas = store.getIdeas()
        val clippings = store.getClippings()

        val tagMap = mutableMapOf<String, MutableList<String>>()

        for (i in ideas) {
            for (t in i.tags) {
                tagMap.getOrPut(t) { mutableListOf() }.add("💡 " + i.getPreview(15))
            }
        }

        for (c in clippings) {
            for (t in c.tags) {
                val title = if (c.title.isNotBlank()) c.title else c.summary
                tagMap.getOrPut(t) { mutableListOf() }.add("📰 " + title.take(15))
            }
        }

        return tagMap.map { (tag, items) ->
            val iCount = items.count { it.startsWith("💡") }
            val cCount = items.count { it.startsWith("📰") }
            TopicCluster(tag, iCount, cCount, items)
        }.sortedByDescending { it.ideaCount + it.clippingCount }
    }
}