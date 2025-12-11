package com.ran.crm.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.ran.crm.data.local.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

        @Query("SELECT * FROM contacts ORDER BY name ASC") fun getAllContacts(): Flow<List<Contact>>

        @Query("SELECT * FROM contacts ORDER BY name ASC")
        fun getAllContactsPaged(): PagingSource<Int, Contact>

        @Query(
                "SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phone_raw LIKE '%' || :query || '%' ORDER BY name ASC"
        )
        fun searchContacts(query: String): Flow<List<Contact>>

        @Query(
                "SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phone_raw LIKE '%' || :query || '%' ORDER BY name ASC"
        )
        fun searchContactsPaged(query: String): PagingSource<Int, Contact>

        @Query("SELECT * FROM contacts WHERE updated_at > :since ORDER BY updated_at ASC")
        suspend fun getContactsUpdatedSince(since: String): List<Contact>

        @Query("SELECT * FROM contacts WHERE id = :id")
        suspend fun getContactById(id: String): Contact?

        @Query("SELECT * FROM contacts WHERE phone_normalized = :phoneNormalized")
        suspend fun getContactByPhoneNormalized(phoneNormalized: String): Contact?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertContacts(contacts: List<Contact>)

        @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertContact(contact: Contact)

        @Update suspend fun updateContact(contact: Contact)

        @Delete suspend fun deleteContact(contact: Contact)

        @Query("DELETE FROM contacts WHERE id = :id") suspend fun deleteContactById(id: String)

        @Query("SELECT COUNT(*) FROM contacts") suspend fun getContactsCount(): Int

        @Query("SELECT * FROM contacts WHERE sync_status = 1")
        suspend fun getDirtyContacts(): List<Contact>

        @Query("UPDATE contacts SET sync_status = 0 WHERE id = :id")
        suspend fun markAsSynced(id: String)

        @Query("UPDATE contacts SET sync_status = 0 WHERE id IN (:ids)")
        suspend fun markAsSynced(ids: List<String>)

        // Mark and Sweep
        @Query(
                "UPDATE contacts SET sync_status = 2"
        ) // 2 = Pending Deletion (Temporary state for sync)
        suspend fun markAllPendingDeletion()

        @Query("UPDATE contacts SET sync_status = 0 WHERE id = :id")
        suspend fun unmarkPendingDeletion(id: String)

        @Query("DELETE FROM contacts WHERE sync_status = 2") suspend fun deletePendingDeletion()
}
