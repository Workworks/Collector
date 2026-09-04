package com.kfaino.diapertracker

import com.kfaino.collecter.core.WorkspaceRecords
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecallPolicyTest {
    @Test fun `只有积压或长期未归位才召回`() {
        val day=24L*60*60*1000; val now=40*day
        val small=JSONArray().put(WorkspaceRecords.inbox("一",now=now-day))
        assertTrue(RecallPolicy.evaluate(emptyList(),small,now).isEmpty())
        val backlog=JSONArray()
        repeat(5){backlog.put(WorkspaceRecords.inbox("记录$it",now=now-4*day))}
        val prompts=RecallPolicy.evaluate(listOf(Entry(brand="旧物",location="未整理",ts=now-31*day)),backlog,now)
        assertEquals(listOf("inbox-backlog","unlocated-items"),prompts.map { it.key })
    }
}
