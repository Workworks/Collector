package com.kfaino.collecter.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DeferredWorkRegressionTest {
    @Test fun ocrDoesNotResurrectDeletionOrOverwriteNewerRequestAndTitle() {
        assertNull(WorkspaceRecords.ocrResult(null,"old","text",null))
        val current = WorkspaceRecords.inbox("original", "image").put("ocrRequest","new").put("title","edited").put("status","organized")
        assertNull(WorkspaceRecords.ocrResult(current,"old","old text",null))
        val result = WorkspaceRecords.ocrResult(current,"new","recognized",null)!!
        assertEquals("edited",result.getString("title"))
        assertEquals("organized",result.getString("status"))
        assertEquals("original",result.getString("original"))
        assertEquals("recognized",result.getString("ocr"))
        assertFalse(result.has("ocrRequest"))
    }
    @Test fun failurePreservesOriginalAndRetryCanSucceed() {
        val current = WorkspaceRecords.inbox("original","broken").put("status","processing").put("ocrRequest","first")
        val failed = WorkspaceRecords.ocrResult(current,"first",null,"decode failed")!!
        assertEquals("broken",failed.getString("photo"))
        assertEquals("error",failed.getString("status"))
        failed.put("ocrRequest","retry").put("status","processing")
        assertEquals("processed",WorkspaceRecords.ocrResult(failed,"retry","ok",null)!!.getString("status"))
    }
    @Test fun remindersAcrossYearSnoozeBoundaryMuteAndNewCycle() {
        val day = 86400000L
        val state = JSONObject().put("cycle","2026").put("sentAt",1L)
        val snoozed = WorkspaceRecords.reminderAction(state,"snooze",365L*day)
        assertFalse(WorkspaceRecords.shouldNotify(snoozed,"2026",366L*day-1))
        assertTrue(WorkspaceRecords.shouldNotify(snoozed,"2026",366L*day))
        assertFalse(WorkspaceRecords.shouldNotify(WorkspaceRecords.reminderAction(state,"mute"),"2027",730L*day))
        assertFalse(WorkspaceRecords.shouldNotify(WorkspaceRecords.reminderAction(state,"done"),"2026",730L*day))
        assertTrue(WorkspaceRecords.shouldNotify(WorkspaceRecords.reminderAction(state,"done"),"2027",730L*day))
    }
}
