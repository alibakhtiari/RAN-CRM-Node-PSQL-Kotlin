package com.ran.crm.utils

import org.junit.Assert.*
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun `normalizePhoneNumber should handle Iranian mobile numbers correctly`() {
        // Test various Iranian mobile number formats
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("09123456789"))
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("+989123456789"))
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("00989123456789"))
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("9123456789"))
    }

    @Test
    fun `normalizePhoneNumber should handle international numbers`() {
        assertEquals("+15551234567", PhoneUtils.normalizePhoneNumber("+1-555-123-4567"))
        assertEquals("+441234567890", PhoneUtils.normalizePhoneNumber("+44 123 456 7890"))
    }

    @Test
    fun `normalizePhoneNumber should return null for invalid numbers`() {
        assertNull(PhoneUtils.normalizePhoneNumber("invalid"))
        assertNull(PhoneUtils.normalizePhoneNumber("123"))
        assertNull(PhoneUtils.normalizePhoneNumber(""))
    }

    @Test
    fun `isValidPhoneNumber should validate Iranian numbers correctly`() {
        assertTrue(PhoneUtils.isValidPhoneNumber("09123456789"))
        assertTrue(PhoneUtils.isValidPhoneNumber("+989123456789"))
        assertFalse(PhoneUtils.isValidPhoneNumber("0912345678")) // Too short
        assertFalse(PhoneUtils.isValidPhoneNumber("invalid"))
    }

    @Test
    fun `isIranianNumber should identify Iranian numbers correctly`() {
        assertTrue(PhoneUtils.isIranianNumber("09123456789"))
        assertTrue(PhoneUtils.isIranianNumber("+989123456789"))
        assertFalse(PhoneUtils.isIranianNumber("+15551234567"))
        assertFalse(PhoneUtils.isIranianNumber("invalid"))
    }

    @Test
    fun `formatForDisplay should format numbers nicely`() {
        val formatted = PhoneUtils.formatForDisplay("09123456789")
        assertNotNull(formatted)
        assertTrue(formatted!!.contains("912") || formatted.contains("0912"))
    }

    @Test
    fun `getNationalNumber should extract national part correctly`() {
        assertEquals("9123456789", PhoneUtils.getNationalNumber("09123456789"))
        assertEquals("9123456789", PhoneUtils.getNationalNumber("+989123456789"))
        assertNull(PhoneUtils.getNationalNumber("invalid"))
    }

    @Test
    fun `normalizePhoneNumber should handle spaces and special characters`() {
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("0912 345 6789"))
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("0912-345-6789"))
        assertEquals("+989123456789", PhoneUtils.normalizePhoneNumber("(0912) 345-6789"))
    }
}
