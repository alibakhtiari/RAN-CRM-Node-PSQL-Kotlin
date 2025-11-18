package com.ran.crm.data.repository

import com.ran.crm.data.local.dao.ContactDao
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.BatchContactRequest
import com.ran.crm.data.remote.model.BatchContactData
import com.ran.crm.data.remote.model.CreateContactRequest
import com.ran.crm.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.time.ExperimentalTime

class ContactRepository(
    private val contactDao: ContactDao
) {

    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()

    fun searchContacts(query: String): Flow<List<Contact>> = contactDao.searchContacts(query)

    suspend fun getContactById(id: String): Contact? = contactDao.getContactById(id)

    suspend fun getContactByPhoneNormalized(phoneNormalized: String): Contact? =
        contactDao.getContactByPhoneNormalized(phoneNormalized)

    suspend fun insertContacts(contacts: List<Contact>) = contactDao.insertContacts(contacts)

    suspend fun insertContact(contact: Contact) = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) = contactDao.updateContact(contact)

    suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)

    suspend fun getContactsUpdatedSince(since: String): List<Contact> =
        contactDao.getContactsUpdatedSince(since)

    @OptIn(ExperimentalTime::class)
    suspend fun syncContacts() {
        // Get local changes since last sync
        val lastSync = getLastSyncTime()
        val localContacts = if (lastSync != null) {
            getContactsUpdatedSince(lastSync)
        } else {
            // Initial sync - get all contacts
            getAllContacts().first()
        }

        // Upload local changes in batches
        if (localContacts.isNotEmpty()) {
            uploadContactsBatch(localContacts)
        }

        // Download remote changes
        downloadContacts(lastSync)
    }

    private suspend fun uploadContactsBatch(contacts: List<Contact>) {
        val batchData = contacts.map { contact ->
            BatchContactData(
                name = contact.name,
                phone_raw = contact.phoneRaw,
                created_at = contact.createdAt
            )
        }

        val batchRequest = BatchContactRequest(batchData)
        val result = safeApiCall {
            ApiClient.apiService.batchCreateContacts(batchRequest)
        }

        when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                // Update local contacts with server responses
                val response = result.data
                response.results.forEach { batchResult ->
                    val localContact = contacts.find { it.phoneNormalized == batchResult.contact.phoneNormalized }
                    localContact?.let {
                        val updatedContact = it.copy(
                            id = batchResult.contact.id,
                            updatedAt = batchResult.contact.updatedAt
                        )
                        updateContact(updatedContact)
                    }
                }
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
                // Handle error - could implement retry logic
                throw Exception("Failed to upload contacts: ${result.message}")
            }
        }
    }

    private suspend fun downloadContacts(since: String?) {
        val result = safeApiCall {
            if (since != null) {
                ApiClient.apiService.getContacts(updatedSince = since)
            } else {
                ApiClient.apiService.getContacts()
            }
        }

        when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                val contacts = result.data.data
                if (contacts.isNotEmpty()) {
                    insertContacts(contacts)
                }
                // Update last sync time
                updateLastSyncTime()
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
                // Handle error
                throw Exception("Failed to download contacts: ${result.message}")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun createContact(name: String, phoneRaw: String, phoneNormalized: String): Contact? {
        val request = CreateContactRequest(
            name = name,
            phone_raw = phoneRaw,
            phone_normalized = phoneNormalized,
            created_at = kotlinx.datetime.Clock.System.now().toString()
        )

        val result = safeApiCall {
            ApiClient.apiService.createContact(request)
        }

        return when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                val contact = result.data.contact
                insertContact(contact)
                contact
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
                if (result.code == 409) {
                    // Conflict - contact already exists
                    // Try to get existing contact
                    getContactByPhoneNormalized(phoneNormalized)
                } else {
                    null
                }
            }
        }
    }

    private suspend fun getLastSyncTime(): String? {
        // This would typically be stored in SharedPreferences or database
        // For now, return null to force full sync
        return null
    }

    private suspend fun updateLastSyncTime() {
        // Update last sync timestamp
        // This would typically be stored in SharedPreferences
    }
}
