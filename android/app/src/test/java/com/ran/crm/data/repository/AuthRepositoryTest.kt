package com.ran.crm.data.repository

import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.ApiResult
import com.ran.crm.data.remote.model.LoginResponse
import com.ran.crm.data.local.entity.User
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var mockApiClient: ApiClient

    @Before
    fun setup() {
        mockApiClient = mockk()
        // Note: In a real scenario, we'd inject the ApiClient, but for simplicity we'll test the logic
        authRepository = AuthRepository()
    }

    @Test
    fun `login should return success when API call succeeds`() = runTest {
        // Given
        val username = "testuser"
        val password = "testpass"
        val expectedUser = User(
            id = "user123",
            username = username,
            name = "Test User",
            isAdmin = false,
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        )
        val loginResponse = LoginResponse("jwt_token", expectedUser)

        // Mock the API call
        mockkObject(ApiClient)
        coEvery { ApiClient.apiService.login(any()) } returns loginResponse

        // When
        val result = authRepository.login(username, password)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())

        // Verify token was set
        verify { ApiClient.setAuthToken("jwt_token") }
    }

    @Test
    fun `login should return failure when API call fails`() = runTest {
        // Given
        val username = "testuser"
        val password = "wrongpass"

        // Mock the API call to throw exception
        mockkObject(ApiClient)
        coEvery { ApiClient.apiService.login(any()) } throws RuntimeException("Invalid credentials")

        // When
        val result = authRepository.login(username, password)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
    }

    @Test
    fun `logout should clear token`() {
        // When
        authRepository.logout()

        // Then
        verify { ApiClient.setAuthToken(null) }
    }

    @Test
    fun `isLoggedIn should return true when token exists`() {
        // Given
        mockkObject(ApiClient)
        every { ApiClient.getAuthToken() } returns "valid_token"

        // When
        val result = authRepository.isLoggedIn()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isLoggedIn should return false when no token`() {
        // Given
        mockkObject(ApiClient)
        every { ApiClient.getAuthToken() } returns null

        // When
        val result = authRepository.isLoggedIn()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getToken should return current token`() {
        // Given
        mockkObject(ApiClient)
        every { ApiClient.getAuthToken() } returns "current_token"

        // When
        val token = authRepository.getToken()

        // Then
        assertEquals("current_token", token)
    }
}
