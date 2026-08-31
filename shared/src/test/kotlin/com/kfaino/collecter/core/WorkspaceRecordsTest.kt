package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WorkspaceRecordsTest {
    @Test fun collectionRetainsOriginalAndLinksAreBidirectionalAndIdempotent() {
        val raw = "Receipt\nOriginal text <html>"
        val item = WorkspaceRecords.inbox(raw)
        assertEquals(raw, item.getString("original"))
        val a = "entries:a"; val b = "inbox:${item.getString("id")}" 
        val links = WorkspaceRecords.link(JSONArray(), a, b)
        assertEquals(listOf(b), WorkspaceRecords.related(links, a))
        assertEquals(listOf(a), WorkspaceRecords.related(links, b))
        assertEquals(1, WorkspaceRecords.link(links, b, a).length())
        assertEquals(raw, item.getString("original")) // linking never mutates the source
    }

    @Test fun remindersDedupePerCycleAndSnoozeAndMutePersist() {
        val state = JSONObject().put("id", "expiry:a").put("cycle", "one").put("sentAt", 100L)
        assertFalse(WorkspaceRecords.shouldNotify(JSONObject(state.toString()), "one", 200L))
        assertTrue(WorkspaceRecords.shouldNotify(state, "two", 200L))
        val snoozed = WorkspaceRecords.reminderAction(state, "snooze", 200L)
        assertFalse(WorkspaceRecords.shouldNotify(snoozed, "one", 300L))
        assertTrue(WorkspaceRecords.shouldNotify(snoozed, "one", 200L + 86400000L))
        assertFalse(WorkspaceRecords.shouldNotify(WorkspaceRecords.reminderAction(state, "done"), "one", 300L))
        assertFalse(WorkspaceRecords.shouldNotify(WorkspaceRecords.reminderAction(state, "mute"), "two", 300L))
        assertTrue(WorkspaceRecords.shouldNotify(WorkspaceRecords.reminderAction(state, "enable"), "one", 300L))
    }
}
