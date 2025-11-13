package com.ran.crm.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ContactImporter(
    private val context: Context,
    private val contactRepository: ContactRepository
) {

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val errors: Int
    )

    suspend fun importDeviceContacts(): ImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var skipped = 0
        var errors = 0

        try {
            val contentResolver = context.contentResolver
            val contacts = getDeviceContacts(contentResolver)

            for (deviceContact in contacts) {
                try {
                    // Check if contact already exists
                    val existingContact = contactRepository.getContactByPhoneNormalized(deviceContact.phoneNormalized)

                    if (existingContact == null) {
                        // Create new contact
                        val contact = Contact(
                            id = UUID.randomUUID().toString(),
                            name = deviceContact.name,
                            phoneRaw = deviceContact.phoneRaw,
                            phoneNormalized = deviceContact.phoneNormalized,
                            createdBy = "", // Will be set when syncing
                            createdAt = kotlinx.datetime.Clock.System.now().toString(),
                            updatedAt = kotlinx.datetime.Clock.System.now().toString()
                        )

                        contactRepository.insertContact(contact)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    errors++
                }
            }
        } catch (e: Exception) {
            errors++
        }

        ImportResult(imported, skipped, errors)
    }

    private fun getDeviceContacts(contentResolver: ContentResolver): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (c.moveToNext()) {
                try {
                    val name = c.getString(nameIndex) ?: continue
                    val phoneRaw = c.getString(numberIndex) ?: continue

                    // Normalize phone number
                    val phoneNormalized = PhoneUtils.normalizePhoneNumber(phoneRaw) ?: continue

                    // Skip if we already have this phone number
                    if (contacts.any { it.phoneNormalized == phoneNormalized }) {
                        continue
                    }

                    contacts.add(DeviceContact(name, phoneRaw, phoneNormalized))
                } catch (e: Exception) {
                    // Skip invalid contacts
                    continue
                }
            }
        }

        return contacts
    }

    private data class DeviceContact(
        val name: String,
        val phoneRaw: String,
        val phoneNormalized: String
    )
}
