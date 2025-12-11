package com.ran.crm.data.manager

import android.content.Context
import android.provider.ContactsContract
import com.ran.crm.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactMigrationManager(
    private val context: Context,
    private val contactRepository: ContactRepository
) {

    suspend fun importSystemContacts(deleteAfterImport: Boolean): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val contactIdIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val normalizedNumberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Unknown"
                val phoneRaw = it.getString(numberIndex) ?: ""
                val contactId = it.getString(contactIdIndex)
                // Use normalized number if available, otherwise fallback to raw or basic normalization
                val phoneNormalized = it.getString(normalizedNumberIndex) ?: phoneRaw.replace(Regex("[^0-9+]"), "")

                if (phoneRaw.isNotBlank()) {
                    // Check if contact already exists in CRM
                    val existingContact = contactRepository.getContactByPhoneNormalized(phoneNormalized)
                    
                    if (existingContact == null) {
                        // Create new contact in CRM
                        val newContact = contactRepository.createContact(name, phoneRaw, phoneNormalized)
                        if (newContact != null) {
                            importedCount++
                            if (deleteAfterImport) {
                                deleteSystemContact(contentResolver, contactId)
                            }
                        }
                    } else if (deleteAfterImport) {
                        // Contact exists in CRM, but we want to move (delete from system)
                        deleteSystemContact(contentResolver, contactId)
                    }
                }
            }
        }
        importedCount
    }

    private fun deleteSystemContact(contentResolver: android.content.ContentResolver, contactId: String) {
        try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contactId
            )
            contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
