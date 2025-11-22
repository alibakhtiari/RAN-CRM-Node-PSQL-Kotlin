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


    // syncContacts removed. Logic moved to SyncManager.

    // uploadContactsBatch removed. Logic should be moved to SyncManager if needed.

    /**
     * Performs a full sync download:
     * 1. Fetches ALL contacts from server (paginated).
     * 2. Updates/Inserts them into local DB.
     * 3. Deletes any local contacts NOT present in the server list.
     */
    suspend fun performFullSyncDownload() {
        var page = 1
        val limit = 50
        val allServerContactIds = mutableSetOf<String>()
        var hasMore = true

        com.ran.crm.utils.SyncLogger.log("Repo: Starting Full Download")

        while (hasMore) {
            val result = safeApiCall {
                ApiClient.apiService.getContacts(page = page, limit = limit)
            }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    val response = result.data
                    val contacts = response.data
                    
                    com.ran.crm.utils.SyncLogger.log("Repo: Fetched page $page, count: ${contacts.size}")

                    if (contacts.isNotEmpty()) {
                        insertContacts(contacts)
                        allServerContactIds.addAll(contacts.map { it.id })
                    }
                    
                    if (contacts.size < limit) {
                        hasMore = false
                    } else {
                        page++
                    }
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    throw Exception("Failed to download contacts page $page: ${result.message}")
                }
            }
        }

        // Deletion Logic (Server is Truth)
        val localContacts = contactDao.getAllContacts().first()
        val contactsToDelete = localContacts.filter { it.id !in allServerContactIds }
        
        if (contactsToDelete.isNotEmpty()) {
            com.ran.crm.utils.SyncLogger.log("Repo: Deleting ${contactsToDelete.size} local contacts not on server")
            contactsToDelete.forEach { contact ->
                contactDao.deleteContact(contact)
            }
        } else {
            com.ran.crm.utils.SyncLogger.log("Repo: No local contacts to delete")
        }

        updateLastSyncTime()
    }

    /**
     * Performs a delta sync download:
     * 1. Fetches contacts updated since last sync.
     * 2. Updates/Inserts them.
     * Note: Delta sync typically doesn't handle deletions unless the API returns "deleted" records.
     * If the API doesn't support soft-deletes, delta sync might miss deletions.
     * For robustness, we might want to periodically force full sync.
     */
    suspend fun performDeltaSyncDownload() {
        val lastSyncTime = preferenceManager.lastSyncContacts
        val since = if (lastSyncTime > 0) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(lastSyncTime))
        } else {
            // If no last sync time, fallback to full sync
            performFullSyncDownload()
            return
        }

        com.ran.crm.utils.SyncLogger.log("Repo: Starting Delta Download (since $since)")

        var page = 1
        val limit = 50
        var hasMore = true

        while (hasMore) {
            val result = safeApiCall {
                ApiClient.apiService.getContacts(page = page, limit = limit, updatedSince = since)
            }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    val response = result.data
                    val contacts = response.data
                    
                    if (contacts.isNotEmpty()) {
                        insertContacts(contacts)
                        com.ran.crm.utils.SyncLogger.log("Repo: Delta fetched ${contacts.size} contacts")
                    }
                    
                    if (contacts.size < limit) {
                        hasMore = false
                    } else {
                        page++
                    }
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    throw Exception("Failed to download delta contacts: ${result.message}")
                }
            }
        }
        
        updateLastSyncTime()
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



    private suspend fun updateLastSyncTime() {
        preferenceManager.lastSyncContacts = System.currentTimeMillis()
    }
}
