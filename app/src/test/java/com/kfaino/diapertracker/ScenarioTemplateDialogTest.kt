package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenarioTemplateDialogTest {
    @Test fun `模板只补充分类且保留已有顺序`() {
        val template = ScenarioTemplateDialog.Template("测试", listOf("药品", "食品", "药品"))
        assertEquals(listOf("自定义", "药品", "食品"), ScenarioTemplateDialog.merge(listOf("自定义", "药品"), template))
    }
}
