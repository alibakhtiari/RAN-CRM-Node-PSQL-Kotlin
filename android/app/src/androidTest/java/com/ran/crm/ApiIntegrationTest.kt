package com.ran.crm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.ApiResult
import com.ran.crm.data.remote.model.BatchContactData
import com.ran.crm.data.remote.model.BatchContactRequest
import com.ran.crm.data.remote.model.LoginRequest
import com.ran.crm.data.remote.safeApiCall
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class ApiIntegrationTest {

    @Before
    fun setup() {
        // Ensure we're using the correct base URL for testing
        // This would be configured for the test environment
    }

    @Test
    fun `login API should authenticate user successfully`() = runBlocking {
        // Given
        val loginRequest =
                LoginRequest(
                        username = BuildConfig.TEST_USERNAME,
                        password = BuildConfig.TEST_PASSWORD
                )

        // When
        val result = safeApiCall { ApiClient.apiService.login(loginRequest) }

        // Then
        assertTrue("Login should succeed", result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertNotNull("Token should not be null", response.token)
        assertNotNull("User should not be null", response.user)
        assertEquals("Username should match", BuildConfig.TEST_USERNAME, response.user.username)

        // Store token for subsequent tests
        ApiClient.setAuthToken(response.token)
    }

    @Test
    fun `get contacts API should return paginated results`() = runBlocking {
        // Given - assume login was successful in previous test
        val token = ApiClient.getAuthToken()
        assumeTrue("Token should be available from login", token != null)

        // When
        val result = safeApiCall { ApiClient.apiService.getContacts(page = 1, limit = 10) }

        // Then
        assertTrue("Get contacts should succeed", result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertNotNull("Data should not be null", response.data)
        assertNotNull("Pagination should not be null", response.pagination)
        assertTrue("Current page should be valid", response.pagination.currentPage >= 1)
    }

    @Test
    fun `batch contact upload should handle multiple contacts`() = runBlocking {
        // Given
        val token = ApiClient.getAuthToken()
        assumeTrue("Token should be available from login", token != null)

        val batchData =
                listOf(
                        BatchContactData(
                                name = "Test Contact 1",
                                phone_raw = "09123456789",
                                created_at = Clock.System.now().toString()
                        ),
                        BatchContactData(
                                name = "Test Contact 2",
                                phone_raw = "09123456790",
                                created_at = Clock.System.now().toString()
                        )
                )
        val batchRequest = BatchContactRequest(batchData)

        // When
        val result = safeApiCall { ApiClient.apiService.batchCreateContacts(batchRequest) }

        // Then
        assertTrue("Batch upload should succeed", result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertNotNull("Results should not be null", response.results)
        assertNotNull("Summary should not be null", response.summary)
        assertEquals("Should process 2 contacts", 2, response.summary.total)
    }

    @Test
    fun `get call logs API should return results when available`() = runBlocking {
        // Given
        val token = ApiClient.getAuthToken()
        assumeTrue("Token should be available from login", token != null)

        // When
        val result = safeApiCall { ApiClient.apiService.getCalls(page = 1, limit = 10) }

        // Then
        assertTrue("Get call logs should succeed", result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertNotNull("Data should not be null", response.data)
        assertNotNull("Pagination should not be null", response.pagination)
        // Note: May be empty if no call logs exist, which is fine
    }

    @Test
    fun `unauthorized request should return 401 error`() = runBlocking {
        // Given - clear token to simulate unauthorized request
        ApiClient.setAuthToken(null)

        // When
        val result = safeApiCall { ApiClient.apiService.getContacts() }

        // Then
        assertTrue("Request should fail with 401", result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals("Should be unauthorized error", 401, error.code)
    }

    @Test
    fun `invalid login should return authentication error`() = runBlocking {
        // Given
        val invalidLoginRequest = LoginRequest(username = "invalid", password = "invalid")

        // When
        val result = safeApiCall { ApiClient.apiService.login(invalidLoginRequest) }

        // Then
        assertTrue("Login should fail", result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertTrue("Should be authentication error", error.code == 401 || error.code == 400)
    }

    private fun assumeTrue(message: String, condition: Boolean) {
        if (!condition) {
            org.junit.Assume.assumeTrue(message, condition)
        }
    }
}
