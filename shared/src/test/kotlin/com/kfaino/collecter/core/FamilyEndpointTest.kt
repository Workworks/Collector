package com.kfaino.collecter.core
import org.junit.Assert.*
import org.junit.Test
class FamilyEndpointTest {
    @Test fun plaintextIsRestrictedToPrivateLiteralAddresses() {
        for(host in listOf("127.0.0.1","localhost","192.168.3.8","10.0.2.2","172.16.1.2","172.31.1.2")) {
            assertEquals(host,FamilyEndpoint.validate("http://$host/api/v1/family").host)
        }
        for(host in listOf("example.com","8.8.8.8","172.32.1.2","192.168.3.999","127.0.0.1.evil.com")) {
            assertThrows(IllegalArgumentException::class.java) {FamilyEndpoint.validate("http://$host/api/v1/family")}
        }
        assertEquals("https",FamilyEndpoint.validate("https://example.com/api/v1/family").protocol)
        assertThrows(IllegalArgumentException::class.java) {FamilyEndpoint.validate("https://user:pass@example.com/api/v1/family")}
    }
}
