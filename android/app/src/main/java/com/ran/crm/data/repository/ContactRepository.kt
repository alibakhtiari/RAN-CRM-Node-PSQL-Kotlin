package com.ran.crm.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.data.local.dao.ContactDao
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.BatchContactData
import com.ran.crm.data.remote.model.BatchContactRequest
import com.ran.crm.data.remote.model.CreateContactRequest
import com.ran.crm.data.remote.safeApiCall
import com.ran.crm.utils.DateUtils
import com.ran.crm.utils.SyncLogger
import java.util.*
import kotlinx.coroutines.flow.Flow

class ContactRepository(
        private val contactDao: ContactDao,
        private val preferenceManager: PreferenceManager
) {

    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()

    fun getAllContactsPaged(): Flow<PagingData<Contact>> {
        return Pager(
                        config =
                                PagingConfig(
                                        pageSize = 20,
                                        enablePlaceholders = false,
                                        prefetchDistance = 5
                                ),
                        pagingSourceFactory = { contactDao.getAllContactsPaged() }
                )
                .flow
    }

    fun searchContacts(query: String): Flow<List<Contact>> = contactDao.searchContacts(query)

    fun searchContactsPaged(query: String): Flow<PagingData<Contact>> {
        return Pager(
                        config =
                                PagingConfig(
                                        pageSize = 20,
                                        enablePlaceholders = false,
                                        prefetchDistance = 5
                                ),
                        pagingSourceFactory = { contactDao.searchContactsPaged(query) }
                )
                .flow
    }

    suspend fun getContactById(id: String): Contact? = contactDao.getContactById(id)

    suspend fun getContactByPhoneNormalized(phoneNormalized: String): Contact? =
            contactDao.getContactByPhoneNormalized(phoneNormalized)

    suspend fun getExistingNormalizedPhones(phones: List<String>): Set<String> =
            contactDao.getExistingNormalizedPhones(phones).toSet()

    suspend fun insertContacts(contacts: List<Contact>) = contactDao.insertContacts(contacts)

    suspend fun insertContact(contact: Contact) = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) {
        // Update locally and mark as dirty
        val dirtyContact = contact.copy(syncStatus = 1)
        contactDao.updateContact(dirtyContact)

        // Try to update remotely immediately (best effort)
        try {
            val request =
                    CreateContactRequest(
                            name = contact.name,
                            phone_raw = contact.phoneRaw,
                            phone_normalized = contact.phoneNormalized,
                            created_at = contact.createdAt
                    )
            val result = safeApiCall { ApiClient.apiService.updateContact(contact.id, request) }
            if (result is com.ran.crm.data.remote.ApiResult.Success) {
                contactDao.markAsSynced(contact.id)
            }
        } catch (e: Exception) {
            // Failed to update remotely, but it's marked as dirty locally.
            // SyncAdapter will pick it up later.
            SyncLogger.log("Repo: Update failed, marked as dirty. Error: ${e.message}")
        }
    }

    suspend fun deleteContact(contact: Contact) {
        // Delete locally
        contactDao.deleteContact(contact)

        // Delete remotely
        try {
            ApiClient.apiService.deleteContact(contact.id)
        } catch (e: Exception) {
            // If offline, we can't delete remotely.
            SyncLogger.log("Repo: Delete failed. Error: ${e.message}")
        }
    }

    suspend fun getContactsUpdatedSince(since: String): List<Contact> =
            contactDao.getContactsUpdatedSince(since)

    /**
     * Performs a full sync download using Mark and Sweep to avoid memory issues:
     * 1. Mark all local contacts as pending deletion (sync_status = 2).
     * 2. Fetch ALL contacts from server (paginated).
     * 3. For each batch, Insert/Update local DB and unmark pending deletion (set sync_status = 0).
     * 4. Delete all remaining contacts with sync_status = 2.
     */
    suspend fun performFullSyncDownload() {
        var page = 1
        val limit = 50
        var hasMore = true

        SyncLogger.log("Repo: Starting Full Download (Mark and Sweep)")

        // 1. Mark all as pending deletion
        contactDao.markAllPendingDeletion()

        while (hasMore) {
            val result = safeApiCall {
                ApiClient.apiService.getContacts(page = page, limit = limit)
            }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    val response = result.data
                    val contacts = response.data

                    SyncLogger.log("Repo: Fetched page $page, count: ${contacts.size}")

                    if (contacts.isNotEmpty()) {
                        // 2. Insert/Update and Unmark
                        // We map server contacts to local entities with syncStatus = 0
                        val entities = contacts.map { it.copy(syncStatus = 0) }
                        contactDao.insertContacts(entities)
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

        // 3. Delete remaining pending deletion
        contactDao.deletePendingDeletion()
        SyncLogger.log("Repo: Full sync cleanup complete")

        updateLastSyncTime()
    }

    /**
     * Performs a delta sync download:
     * 1. Fetches contacts updated since last sync.
     * 2. Updates/Inserts them.
     */
    suspend fun performDeltaSyncDownload() {
        val lastSyncTime = preferenceManager.lastSyncContacts
        val since =
                if (lastSyncTime > 0) {
                    DateUtils.formatIso(lastSyncTime)
                } else {
                    // If no last sync time, fallback to full sync
                    performFullSyncDownload()
                    return
                }

        SyncLogger.log("Repo: Starting Delta Download (since $since)")

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
                        val entities = contacts.map { it.copy(syncStatus = 0) }
                        contactDao.insertContacts(entities)
                        SyncLogger.log("Repo: Delta fetched ${contacts.size} contacts")
                    }

                    if (contacts.size < limit) {
                        hasMore = false
                    } else {
                        page++
                    }
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    val msg = "Failed to download delta contacts: ${result.message}"
                    SyncLogger.log(msg)
                    // Ensure we don't crash the sync process
                    return
                }
            }
        }

        updateLastSyncTime()
    }

    suspend fun uploadDirtyContacts(): Boolean {
        val dirtyContacts = contactDao.getDirtyContacts()
        if (dirtyContacts.isEmpty()) return true

        SyncLogger.log("Repo: Uploading ${dirtyContacts.size} dirty contacts")

        val chunks = dirtyContacts.chunked(100)
        var allSuccess = true

        for (chunk in chunks) {
            val batchRequest =
                    BatchContactRequest(
                            contacts =
                                    chunk.map {
                                        BatchContactData(
                                                name = it.name,
                                                phone_raw = it.phoneRaw,
                                                created_at = it.createdAt
                                        )
                                    }
                    )

            try {
                val result = safeApiCall { ApiClient.apiService.batchCreateContacts(batchRequest) }

                if (result is com.ran.crm.data.remote.ApiResult.Success) {
                    val response = result.data

                    // The server generates new IDs for contacts created offline.
                    // We must update our local contacts with the server's ID, or just insert the
                    // server ones and delete the dirty ones.
                    // Safest approach: For each successful remote contact, insert it (or replace)
                    // and delete the old dirty one matching by phone.
                    val serverContacts = response.results.map { it.contact }
                    if (serverContacts.isNotEmpty()) {
                        val serverEntities =
                                serverContacts.map {
                                    Contact(
                                            id = it.id,
                                            name = it.name,
                                            phoneRaw = it.phoneRaw,
                                            phoneNormalized = it.phoneNormalized,
                                            createdBy = it.createdBy,
                                            createdAt = it.createdAt,
                                            updatedAt = it.updatedAt,
                                            syncStatus = 0
                                    )
                                }

                        // To avoid duplicates, we can delete the local dirty ones that match the
                        // phone numbers of successfully synced contacts.
                        val syncedPhones = serverEntities.map { it.phoneNormalized }
                        for (phone in syncedPhones) {
                            val oldLocal = contactDao.getContactByPhoneNormalized(phone)
                            if (oldLocal != null) {
                                contactDao.deleteContactById(oldLocal.id)
                            }
                        }

                        contactDao.insertContacts(serverEntities)
                    }

                    if (response.errors.isNotEmpty()) {
                        SyncLogger.log("Repo: Batch upload had ${response.errors.size} errors")
                        allSuccess = false
                    }
                    SyncLogger.log("Repo: Batch upload success: ${serverContacts.size} processed")
                } else {
                    SyncLogger.log("Repo: Batch upload failed")
                    allSuccess = false
                }
            } catch (e: Exception) {
                SyncLogger.log("Repo: Batch upload exception: ${e.message}")
                allSuccess = false
            }
        }
        return allSuccess
    }

    suspend fun createContact(name: String, phoneRaw: String, phoneNormalized: String): Contact? {
        // Create locally first (Offline First)
        val newId = UUID.randomUUID().toString()
        val now = DateUtils.formatIso()

        val userId = preferenceManager.userId ?: "local-pending"

        val contact =
                Contact(
                        id = newId,
                        name = name,
                        phoneRaw = phoneRaw,
                        phoneNormalized = phoneNormalized,
                        createdBy = userId,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = 1 // Dirty
                )

        contactDao.insertContact(contact)

        // Try to upload immediately
        val request =
                CreateContactRequest(
                        name = name,
                        phone_raw = phoneRaw,
                        phone_normalized = phoneNormalized,
                        created_at = now
                )

        val result = safeApiCall { ApiClient.apiService.createContact(request) }

        return when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                val serverContact = result.data.contact
                contactDao.deleteContactById(newId) // Delete temporary local
                contactDao.insertContact(serverContact.copy(syncStatus = 0))
                serverContact
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
                if (result.code == 409) {
                    // Conflict - Server has the contact
                    try {
                        if (result.errorBody != null) {
                            val conflictResponse =
                                    com.google.gson.Gson()
                                            .fromJson(
                                                    result.errorBody,
                                                    com.ran.crm.data.remote.model
                                                                    .ConflictErrorResponse::class
                                                            .java
                                            )
                            val serverContact = conflictResponse.existing_contact
                            // Upsert server version locally as synced
                            contactDao.insertContact(serverContact.copy(syncStatus = 0))
                            serverContact
                        } else {
                            // Fallback
                            getContactByPhoneNormalized(phoneNormalized)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        getContactByPhoneNormalized(phoneNormalized)
                    }
                } else {
                    // Return local contact (offline mode)
                    contact
                }
            }
        }
    }

    private suspend fun updateLastSyncTime() {
        preferenceManager.lastSyncContacts = System.currentTimeMillis()
    }
}
