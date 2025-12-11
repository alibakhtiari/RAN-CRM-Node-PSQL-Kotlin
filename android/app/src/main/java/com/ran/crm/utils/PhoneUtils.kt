package com.ran.crm.utils

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

object PhoneUtils {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    /**
     * Normalizes a phone number to E.164 format with Iranian region support
     * Handles multiple formats: 09123456789, +989123456789, 00989123456789
     */
    fun normalizePhoneNumber(phoneNumber: String): String? {
        return try {
            // Clean the input first
            val cleaned = cleanPhoneNumber(phoneNumber)

            // Try to parse with Iranian region first
            val irNumber = phoneUtil.parse(cleaned, "IR")

            // If it's a valid Iranian number, format to E.164
            if (phoneUtil.isValidNumber(irNumber)) {
                phoneUtil.format(irNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                // Try parsing without region hint
                val number = phoneUtil.parse(cleaned, null)
                if (phoneUtil.isValidNumber(number)) {
                    phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
                } else {
                    null // Invalid number
                }
            }
        } catch (e: NumberParseException) {
            null // Parsing failed
        }
    }

    /**
     * Cleans phone number by removing spaces, dashes, parentheses, etc.
     */
    private fun cleanPhoneNumber(phoneNumber: String): String {
        return phoneNumber
            .replace(Regex("[\\s\\-\\(\\)\\+\\.]"), "") // Remove spaces, dashes, parentheses, dots
            .replace(Regex("^00"), "") // Remove leading 00
            .let { cleaned ->
                // Handle Iranian numbers starting with 0
                if (cleaned.startsWith("09") && cleaned.length == 11) {
                    "98${cleaned.substring(1)}" // Convert 09123456789 to 989123456789
                } else {
                    cleaned
                }
            }
    }

    /**
     * Validates if a phone number is valid
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return try {
            val cleaned = cleanPhoneNumber(phoneNumber)
            val irNumber = phoneUtil.parse(cleaned, "IR")
            phoneUtil.isValidNumber(irNumber)
        } catch (e: NumberParseException) {
            false
        }
    }

    /**
     * Formats a phone number for display
     */
    fun formatForDisplay(phoneNumber: String): String {
        return try {
            val cleaned = cleanPhoneNumber(phoneNumber)
            val irNumber = phoneUtil.parse(cleaned, "IR")
            if (phoneUtil.isValidNumber(irNumber)) {
                phoneUtil.format(irNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            } else {
                phoneNumber // Return original if can't format
            }
        } catch (e: NumberParseException) {
            phoneNumber // Return original if parsing fails
        }
    }

    /**
     * Gets the national number part
     */
    fun getNationalNumber(phoneNumber: String): String? {
        return try {
            val cleaned = cleanPhoneNumber(phoneNumber)
            val irNumber = phoneUtil.parse(cleaned, "IR")
            if (phoneUtil.isValidNumber(irNumber)) {
                irNumber.nationalNumber.toString()
            } else {
                null
            }
        } catch (e: NumberParseException) {
            null
        }
    }

    /**
     * Checks if a phone number is Iranian
     */
    fun isIranianNumber(phoneNumber: String): Boolean {
        return try {
            val cleaned = cleanPhoneNumber(phoneNumber)
            val irNumber = phoneUtil.parse(cleaned, "IR")
            phoneUtil.isValidNumber(irNumber) && irNumber.countryCode == 98
        } catch (e: NumberParseException) {
            false
        }
    }
}
