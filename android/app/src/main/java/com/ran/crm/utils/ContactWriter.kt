package com.ran.crm.utils

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ContactWriter(
    private val context: Context,
    private val contactRepository: ContactRepository
) {

    data class SyncResult(
        val exported: Int,
        val updated: Int,
        val skipped: Int,
        val errors: Int
    )

    private val CRM_CONTACT_NOTE = "RAN_CRM_CONTACT"

    /**
     * Syncs CRM contacts to Android device contacts
     */
    /**
     * Syncs CRM contacts to Android device contacts.
     * This method assumes the local database is already up-to-date.
     */
    suspend fun syncToDevice(): SyncResult = withContext(Dispatchers.IO) {
        var exported = 0
        var updated = 0
        var skipped = 0
        var errors = 0

        try {
            val contentResolver = context.contentResolver
            
            com.ran.crm.utils.SyncLogger.log("ContactWriter: Starting export to device...")
            
            // 0. Deduplicate existing contacts to clean up mess
            deduplicateContacts(contentResolver)

            // Get all CRM contacts from LOCAL DB
            val crmContacts = contactRepository.getAllContacts().first()
            val crmPhoneNumbers = crmContacts.mapNotNull { it.phoneNormalized }.toSet()
            
            com.ran.crm.utils.SyncLogger.log("ContactWriter: Found ${crmContacts.size} CRM contacts to sync")

            // 1. Sync CRM contacts to Device (Create/Update)
            for (contact in crmContacts) {
                try {
                    if (contact.phoneNormalized.isNullOrBlank()) {
                        skipped++
                        continue
                    }

                    val existingId = findContactByPhone(contentResolver, contact.phoneNormalized)
                    
                    if (existingId != null) {
                        // Update existing contact
                        updateDeviceContact(contentResolver, existingId, contact)
                        updated++
                    } else {
                        // Create new contact
                        createDeviceContact(contentResolver, contact)
                        exported++
                    }
                } catch (e: Exception) {
                    com.ran.crm.utils.SyncLogger.log("ContactWriter: Failed to sync contact: ${contact.name}", e)
                    errors++
                }
            }
            
            // 2. Prune orphaned contacts (Contacts we created but are no longer in CRM)
            pruneOrphanedContacts(contentResolver, crmPhoneNumbers)

            com.ran.crm.utils.SyncLogger.log("ContactWriter: Export complete: $exported created, $updated updated, $errors errors")
        } catch (e: Exception) {
            com.ran.crm.utils.SyncLogger.log("ContactWriter: Failed to sync contacts to device", e)
            errors++
        }

        SyncResult(exported, updated, skipped, errors)
    }

    /**
     * Deduplicate contacts on device based on normalized phone number.
     * Keeps the one with our Note tag, or the first one found.
     */
    private fun deduplicateContacts(contentResolver: ContentResolver) {
        try {
            val contacts = getAllDeviceContacts(contentResolver)
            val grouped = contacts.groupBy { it.phoneNormalized }
            
            var deletedCount = 0
            
            for ((phone, list) in grouped) {
                if (phone == null) continue
                if (list.size > 1) {
                    // Found duplicates
                    android.util.Log.d("ContactWriter", "Found ${list.size} duplicates for $phone")
                    
                    // Prioritize keeping the one with our tag
                    val tagged = list.find { hasOurTag(contentResolver, it.id) }
                    val toKeep = tagged ?: list.first()
                    
                    val toDelete = list.filter { it.id != toKeep.id }
                    
                    for (contact in toDelete) {
                        deleteContact(contentResolver, contact.id)
                        deletedCount++
                    }
                }
            }
            if (deletedCount > 0) {
                android.util.Log.d("ContactWriter", "Deduplication complete. Removed $deletedCount duplicates.")
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactWriter", "Deduplication failed", e)
        }
    }

    /**
     * Delete contacts that have our tag but are not in the CRM list.
     */
    private fun pruneOrphanedContacts(contentResolver: ContentResolver, crmPhoneNumbers: Set<String>) {
        try {
            android.util.Log.d("ContactWriter", "=== PRUNING ORPHANED CONTACTS ===")
            android.util.Log.d("ContactWriter", "CRM Phone Numbers (${crmPhoneNumbers.size}): ${crmPhoneNumbers.joinToString()}")
            
            // Find all contacts with our tag
            val taggedContactIds = getTaggedContactIds(contentResolver)
            android.util.Log.d("ContactWriter", "Found ${taggedContactIds.size} tagged contacts on device")
            
            var prunedCount = 0
            for (rawContactId in taggedContactIds) {
                val phone = getPhoneForRawContact(contentResolver, rawContactId)
                android.util.Log.d("ContactWriter", "Checking rawContactId=$rawContactId, phone=$phone")
                
                if (phone != null) {
                    val isInCrm = crmPhoneNumbers.contains(phone)
                    android.util.Log.d("ContactWriter", "  Phone: $phone, In CRM: $isInCrm")
                    
                    if (!isInCrm) {
                        // This contact is ours, but not in CRM anymore. Delete it.
                        android.util.Log.d("ContactWriter", "  ❌ PRUNING orphaned contact: $phone (RawContactId: $rawContactId)")
                        deleteContact(contentResolver, rawContactId)
                        prunedCount++
                    } else {
                        android.util.Log.d("ContactWriter", "  ✅ KEEPING contact: $phone (Still in CRM)")
                    }
                }
            }
            
            android.util.Log.d("ContactWriter", "Pruning complete. Removed $prunedCount orphans.")
        } catch (e: Exception) {
            android.util.Log.e("ContactWriter", "Pruning failed", e)
        }
    }

    private fun getTaggedContactIds(contentResolver: ContentResolver): List<String> {
        val ids = mutableListOf<String>()
        
        // Query by ACCOUNT_TYPE instead of note tag
        val cursor = contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
            arrayOf(com.ran.crm.sync.AccountHelper.ACCOUNT_TYPE),
            null
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.RawContacts._ID)
            while (it.moveToNext()) {
                ids.add(it.getString(idIndex))
            }
        }
        
        android.util.Log.d("ContactWriter", "Found ${ids.size} contacts with account type ${com.ran.crm.sync.AccountHelper.ACCOUNT_TYPE}")
        return ids
    }

    private fun hasOurTag(contentResolver: ContentResolver, rawContactId: String): Boolean {
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Note.NOTE} = ?",
            arrayOf(rawContactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, CRM_CONTACT_NOTE),
            null
        )
        val hasTag = cursor?.count ?: 0 > 0
        cursor?.close()
        return hasTag
    }

    private fun getPhoneForRawContact(contentResolver: ContentResolver, rawContactId: String): String? {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ?",
            arrayOf(rawContactId),
            null
        )
        var phone: String? = null
        if (cursor?.moveToFirst() == true) {
            val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            phone = cursor.getString(idx)
        }
        cursor?.close()
        return phone
    }

    private fun deleteContact(contentResolver: ContentResolver, rawContactId: String) {
        val uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()
        contentResolver.delete(uri, "${ContactsContract.RawContacts._ID} = ?", arrayOf(rawContactId))
    }

    private data class DeviceContact(val id: String, val phoneNormalized: String?)

    private fun getAllDeviceContacts(contentResolver: ContentResolver): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER),
            null,
            null,
            null
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            
            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val phone = it.getString(phoneIndex)
                contacts.add(DeviceContact(id, phone))
            }
        }
        return contacts
    }

    /**
     * Find a contact in device by phone number
     */
    private fun findContactByPhone(contentResolver: ContentResolver, phoneNormalized: String): String? {
        val projection = arrayOf(ContactsContract.Data.RAW_CONTACT_ID)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNormalized)

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)
                return it.getString(idIndex)
            }
        }

        return null
    }

    /**
     * Create a new contact in device
     */
    private fun createDeviceContact(contentResolver: ContentResolver, contact: Contact) {
        val ops = ArrayList<ContentProviderOperation>()

        // Create raw contact associated with our Custom Account
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, com.ran.crm.sync.AccountHelper.ACCOUNT_TYPE)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, com.ran.crm.sync.AccountHelper.ACCOUNT_NAME)
                .build()
        )

        // Add name
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build()
        )

        // Add phone number
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneRaw)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        
        // Add Note tag (still useful for debugging, though Account Type is the main identifier now)
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, CRM_CONTACT_NOTE)
                .build()
        )

        contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    /**
     * Update an existing contact in device
     */
    private fun updateDeviceContact(contentResolver: ContentResolver, rawContactId: String, contact: Contact) {
        val ops = ArrayList<ContentProviderOperation>()

        // Update name
        val nameWhere = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val nameArgs = arrayOf(rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        
        ops.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(nameWhere, nameArgs)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build()
        )

        // Update phone (delete old, insert new to avoid duplicates)
        val phoneWhere = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val phoneArgs = arrayOf(rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(phoneWhere, phoneArgs)
                .build()
        )
        
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneRaw)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        
        // Ensure Note tag exists
        if (!hasOurTag(contentResolver, rawContactId)) {
             ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, CRM_CONTACT_NOTE)
                    .build()
            )
        }

        contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }
}
