package com.kfaino.collector.desktop.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopFamilyQrTest {
    @Test fun `家庭配对二维码可生成且地址符合接口契约`() {
        val address = DesktopWorkbench.localFamilyAddress(8848)
        assertTrue(address.endsWith(":8848/api/v1/family"))
        val image = DesktopWorkbench.qrImage("collecter://family?url=test&token=abcdefghijklmnopqrstuvwxyz123456", 128)
        assertEquals(128, image.width)
        assertEquals(128, image.height)
    }
}
