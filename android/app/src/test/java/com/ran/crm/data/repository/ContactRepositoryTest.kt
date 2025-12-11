package com.ran.crm.data.repository

import com.ran.crm.data.local.dao.ContactDao
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.BatchContactResponse
import com.ran.crm.data.remote.model.BatchContactResult
import com.ran.crm.data.remote.model.BatchContactSummary
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ContactRepositoryTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var mockContactDao: ContactDao

    @Before
    fun setup() {
        mockContactDao = mockk()
        contactRepository = ContactRepository(mockContactDao)
    }

    @Test
    fun `getAllContacts should return flow from DAO`() = runTest {
        // Given
        val expectedContacts = listOf(
            Contact("1", "John Doe", "09123456789", "+989123456789", "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z"),
            Contact("2", "Jane Smith", "09123456790", "+989123456790", "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")
        )
        every { mockContactDao.getAllContacts() } returns flowOf(expectedContacts)

        // When
        val result = contactRepository.getAllContacts()

        // Then
        result.collect { contacts ->
            assertEquals(expectedContacts, contacts)
        }
    }

    @Test
    fun `searchContacts should return filtered results from DAO`() = runTest {
        // Given
        val searchQuery = "John"
        val expectedContacts = listOf(
            Contact("1", "John Doe", "09123456789", "+989123456789", "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")
        )
        every { mockContactDao.searchContacts(searchQuery) } returns flowOf(expectedContacts)

        // When
        val result = contactRepository.searchContacts(searchQuery)

        // Then
        result.collect { contacts ->
            assertEquals(expectedContacts, contacts)
            assertEquals(1, contacts.size)
            assertTrue(contacts[0].name.contains("John"))
        }
    }

    @Test
    fun `getContactByPhoneNormalized should return contact from DAO`() = runTest {
        // Given
        val phoneNormalized = "+989123456789"
        val expectedContact = Contact("1", "John Doe", "09123456789", phoneNormalized, "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")
        coEvery { mockContactDao.getContactByPhoneNormalized(phoneNormalized) } returns expectedContact

        // When
        val result = contactRepository.getContactByPhoneNormalized(phoneNormalized)

        // Then
        assertEquals(expectedContact, result)
    }

    @Test
    fun `createContact should save contact locally and sync with server`() = runTest {
        // Given
        val name = "John Doe"
        val phoneRaw = "09123456789"
        val phoneNormalized = "+989123456789"

        val serverContact = Contact("server123", name, phoneRaw, phoneNormalized, "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")

        // Mock API call
        mockkObject(ApiClient)
        coEvery { ApiClient.apiService.createContact(any()) } returns mockk {
            every { contact } returns serverContact
        }

        // When
        val result = contactRepository.createContact(name, phoneRaw, phoneNormalized)

        // Then
        assertNotNull(result)
        assertEquals(serverContact, result)

        // Verify contact was saved locally
        coVerify { mockContactDao.insertContact(serverContact) }
    }

    @Test
    fun `createContact should handle conflicts gracefully`() = runTest {
        // Given
        val name = "John Doe"
        val phoneRaw = "09123456789"
        val phoneNormalized = "+989123456789"

        val existingContact = Contact("existing123", "Existing User", phoneRaw, phoneNormalized, "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")

        // Mock API call to return conflict
        mockkObject(ApiClient)
        coEvery { ApiClient.apiService.createContact(any()) } throws retrofit2.HttpException(
            retrofit2.Response.error<Any>(409, okhttp3.ResponseBody.create(null, ""))
        )

        // Mock finding existing contact
        coEvery { mockContactDao.getContactByPhoneNormalized(phoneNormalized) } returns existingContact

        // When
        val result = contactRepository.createContact(name, phoneRaw, phoneNormalized)

        // Then
        assertEquals(existingContact, result)
    }

    @Test
    fun `syncContacts should upload local changes and download remote changes`() = runTest {
        // Given
        val localContacts = listOf(
            Contact("1", "Local Contact", "09123456789", "+989123456789", "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")
        )

        val batchResponse = BatchContactResponse(
            results = listOf(
                BatchContactResult(0, "created", localContacts[0].copy(id = "server123"))
            ),
            errors = emptyList(),
            summary = BatchContactSummary(1, 1, 0, 0, 0)
        )

        val remoteContacts = listOf(
            Contact("2", "Remote Contact", "09123456790", "+989123456790", "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")
        )

        // Mock local data
        every { mockContactDao.getAllContacts() } returns flowOf(localContacts)
        coEvery { mockContactDao.getContactsUpdatedSince(any()) } returns localContacts

        // Mock API calls
        mockkObject(ApiClient)
        coEvery { ApiClient.apiService.batchCreateContacts(any()) } returns batchResponse
        coEvery { ApiClient.apiService.getContacts(updatedSince = any()) } returns mockk {
            every { data } returns remoteContacts
        }

        // When
        contactRepository.syncContacts()

        // Then
        // Verify batch upload was called
        coVerify { ApiClient.apiService.batchCreateContacts(any()) }

        // Verify remote contacts were downloaded and saved
        coVerify { mockContactDao.insertContacts(remoteContacts) }

        // Verify local contact was updated with server ID
        coVerify { mockContactDao.updateContact(any()) }
    }

    @Test
    fun `insertContacts should delegate to DAO`() = runTest {
        // Given
        val contacts = listOf(
            Contact("1", "Test Contact", "09123456789", "+989123456789", "user1", "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z")
        )

        // When
        contactRepository.insertContacts(contacts)

        // Then
        coVerify { mockContactDao.insertContacts(contacts) }
    }
}
