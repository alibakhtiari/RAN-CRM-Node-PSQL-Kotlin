package com.ran.crm.data.repository

import com.ran.crm.data.local.dao.ContactDao
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.BatchContactRequest
import com.ran.crm.data.remote.model.BatchContactData
import com.ran.crm.data.remote.model.CreateContactRequest
import com.ran.crm.data.remote.safeApiCall
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import java.text.SimpleDateFormat
import com.ran.crm.data.local.PreferenceManager

class ContactRepository(
    private val contactDao: ContactDao,
    private val preferenceManager: PreferenceManager
) {

    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()

    fun getAllContactsPaged(): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { contactDao.getAllContactsPaged() }
        ).flow
    }

    fun searchContacts(query: String): Flow<List<Contact>> = contactDao.searchContacts(query)

    fun searchContactsPaged(query: String): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { contactDao.searchContactsPaged(query) }
        ).flow
    }

    suspend fun getContactById(id: String): Contact? = contactDao.getContactById(id)

    suspend fun getContactByPhoneNormalized(phoneNormalized: String): Contact? =
        contactDao.getContactByPhoneNormalized(phoneNormalized)

    suspend fun insertContacts(contacts: List<Contact>) = contactDao.insertContacts(contacts)

    suspend fun insertContact(contact: Contact) = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) {
        // Update locally
        contactDao.updateContact(contact)
        
        // Update remotely
        val request = CreateContactRequest(
            name = contact.name,
            phone_raw = contact.phoneRaw,
            phone_normalized = contact.phoneNormalized,
            created_at = contact.createdAt
        )
        
        try {
            ApiClient.apiService.updateContact(contact.id, request)
        } catch (e: Exception) {
            // If offline, this will fail. Ideally we should queue this.
            // For now, we'll just log it or rely on next sync if we mark it as dirty.
            // Since we don't have a dirty flag in the entity, we might miss this update if offline.
            // However, the requirement says "delta sync", so we should probably rely on `updatedAt`.
            // But `updateContact` API is direct.
            // Let's assume for now we just try to update.
        }
    }

    suspend fun deleteContact(contact: Contact) {
        // Delete locally
        contactDao.deleteContact(contact)
        
        // Delete remotely
        try {
            ApiClient.apiService.deleteContact(contact.id)
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun getContactsUpdatedSince(since: String): List<Contact> =
        contactDao.getContactsUpdatedSince(since)


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
                if (result.code == 409) {
                    // Conflict - fetch from server and replace local
                    // We can't easily know WHICH one caused conflict in batch, 
                    // but usually 409 in batch might mean one or more failed.
                    // If the API returns 409 for the whole batch, we should probably fetch all.
                    // Assuming standard REST, 409 might be per item or whole request.
                    // Let's assume we should re-sync (download) to resolve.
                    downloadContacts(null) // Force full download to resolve conflicts
                } else {
                     // Handle error - could implement retry logic
                    // throw Exception("Failed to upload contacts: ${result.message}")
                }
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


    suspend fun createContact(name: String, phoneRaw: String, phoneNormalized: String): Contact? {
        val request = CreateContactRequest(
            name = name,
            phone_raw = phoneRaw,
            phone_normalized = phoneNormalized,
            created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
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
        return preferenceManager.lastSyncContacts
    }

    private suspend fun updateLastSyncTime() {
        preferenceManager.lastSyncContacts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
    }
}
