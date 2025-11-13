# Mobile Team Integration Guide

This guide provides everything the Android/Kotlin team needs to integrate with the RAN CRM Backend API.

## Authentication Flow

1. **Login**: POST `/auth/login` with username/password
2. **Receive Token**: Store JWT token from response
3. **Use Token**: Include `Authorization: Bearer <token>` in all subsequent requests
4. **Token Expiry**: Handle 403 responses by re-login

## API Field Types (for Room/Kotlin Models)

### User
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String, // UUID

    @ColumnInfo(name = "username")
    val username: String,

    val name: String,

    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: String // ISO 8601 timestamp
)
```

### Contact
```kotlin
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val id: String, // UUID

    val name: String,

    @ColumnInfo(name = "phone_raw")
    val phoneRaw: String,

    @ColumnInfo(name = "phone_normalized")
    val phoneNormalized: String,

    @ColumnInfo(name = "created_by")
    val createdBy: String, // UUID (User ID)

    @ColumnInfo(name = "created_at")
    val createdAt: String, // ISO 8601 timestamp

    @ColumnInfo(name = "updated_at")
    val updatedAt: String  // ISO 8601 timestamp
)
```

### Call Log
```kotlin
@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey
    val id: String, // UUID

    @ColumnInfo(name = "user_id")
    val userId: String, // UUID

    @ColumnInfo(name = "contact_id")
    val contactId: String?, // UUID or null

    val direction: String, // "incoming" | "outgoing" | "missed"

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,

    val timestamp: String // ISO 8601 timestamp
)
```

### Sync Audit
```kotlin
@Entity(tableName = "sync_audit")
data class SyncAudit(
    @PrimaryKey
    val id: String, // UUID

    @ColumnInfo(name = "user_id")
    val userId: String, // UUID

    @ColumnInfo(name = "synced_contacts")
    val syncedContacts: Int,

    @ColumnInfo(name = "synced_calls")
    val syncedCalls: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String // ISO 8601 timestamp
)
```

## Sync Logic Implementation

### Phone Number Normalization
- **Iranian Phone Support**: Handles multiple formats (09123456789, +989123456789, 00989123456789)
- **E.164 Standardization**: All phone numbers normalized to international format (+989123456789)
- **Automatic Cleaning**: Removes spaces, dashes, and parentheses before normalization

### Contact Sync
- **No Duplicates**: Phone numbers are unique via `phone_normalized`
- **Cross-User Protection**: Different users cannot create contacts with same phone number
- **Same User Updates**: Users can update their own existing contacts
- **Older Wins Rule**: Server compares `created_at` timestamps for conflicts
- **Client Behavior**:
  - Pre-normalize phone numbers using libphonenumber with Iran region support
  - Send all contacts with `created_at` timestamps
  - Handle server responses (created/updated/existing/conflict)
  - Update local database with server response

### Call Log Sync
- **Upload Only New**: Send only calls newer than last sync
- **Batch Upload**: Group calls in arrays (max 1000 per request)
- **Link to Contacts**: Use `contact_id` if available, or `phone_normalized` for matching
- **Server Matching**: If no `contact_id`, server finds contact by `phone_normalized`

### Pagination Handling
- Use `page` and `limit` parameters for all list requests
- Handle `pagination` object in responses
- Implement infinite scroll or load-more patterns

## API Request Examples

### Login
```kotlin
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)
```

### Create Contact
```kotlin
data class CreateContactRequest(
    val name: String,
    val phone_raw: String,
    val phone_normalized: String,
    val created_at: String
)

data class ContactResponse(
    val contact: Contact
)
```

### Bulk Upload Calls
```kotlin
data class CallUploadRequest(
    val calls: List<CallData>
)

data class CallData(
    val contact_id: String? = null,
    val direction: String, // "incoming", "outgoing", "missed"
    val duration_seconds: Int,
    val timestamp: String,
    val phone_normalized: String? = null
)

data class CallUploadResponse(
    val calls: List<CallLog>,
    val count: Int
)
```

### Batch Contact Upload
```kotlin
data class BatchContactRequest(
    val contacts: List<BatchContactData>
)

data class BatchContactData(
    val name: String,
    val phone_raw: String,
    val created_at: String? = null
)

data class BatchContactResponse(
    val results: List<BatchContactResult>,
    val errors: List<BatchContactError>,
    val summary: BatchContactSummary
)

data class BatchContactResult(
    val index: Int,
    val action: String, // "created", "updated", "existing"
    val contact: Contact
)

data class BatchContactError(
    val index: Int,
    val error: String,
    val existing_contact: Contact? = null,
    val contact: BatchContactData
)

data class BatchContactSummary(
    val total: Int,
    val created: Int,
    val updated: Int,
    val existing: Int,
    val errors: Int
)
```

### Paginated Response
```kotlin
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationInfo
)

data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val itemsPerPage: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
)
```

## Error Handling

### HTTP Status Codes
- **200**: Success
- **201**: Created
- **400**: Bad Request - validation error
- **401**: Unauthorized - invalid/missing token
- **403**: Forbidden - insufficient permissions
- **404**: Not Found - resource doesn't exist
- **409**: Conflict - duplicate data (handled gracefully)
- **422**: Unprocessable Entity - validation failed
- **500**: Internal Server Error - retry with backoff

### Error Response Format
```kotlin
data class ErrorResponse(
    val error: String
)

// Special case for contact conflicts (409)
data class ConflictErrorResponse(
    val error: String,
    val existing_contact: Contact // Details of the conflicting contact
)
```

### Handling Strategies
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Error(401, "Re-authentication required")
            403 -> ApiResult.Error(403, "Access denied")
            409 -> ApiResult.Error(409, "Data conflict - merge required")
            else -> ApiResult.Error(e.code(), e.message())
        }
    } catch (e: IOException) {
        ApiResult.Error(-1, "Network error")
    }
}
```

## Network Client Setup (Kotlin)

```kotlin
// Retrofit Interface
interface CrmApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): User

    @GET("contacts")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): PaginatedResponse<Contact>

    @GET("contacts")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("updated_since") updatedSince: String? = null
    ): PaginatedResponse<Contact>

    @POST("contacts")
    suspend fun createContact(@Body contact: CreateContactRequest): ContactResponse

    @POST("contacts/batch")
    suspend fun batchCreateContacts(@Body request: BatchContactRequest): BatchContactResponse

    @GET("calls")
    suspend fun getCalls(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("updated_since") updatedSince: String? = null
    ): PaginatedResponse<CallLog>

    @POST("calls")
    suspend fun uploadCalls(@Body request: CallUploadRequest): CallUploadResponse
}

// OkHttp Client with Auth Interceptor
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}

// Retrofit Setup
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor { authToken })
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://your-api-domain.com/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(CrmApiService::class.java)
```

## Sync Strategy

### Initial Sync
1. Login and get token
2. Fetch all contacts with pagination
3. Fetch recent call logs with pagination
4. Store last sync timestamp

### Incremental Sync (Delta Sync)
1. Query local database for changes since last sync timestamp
2. **Delta Fetch**: GET `/contacts?updated_since=<timestamp>` and `/calls?updated_since=<timestamp>`
3. **Batch Upload**: POST `/contacts/batch` for multiple contacts (max 100)
4. Upload new call logs in batches (max 1000 per request)
5. Update local data with server responses
6. Update last sync timestamp

### Conflict Resolution
```kotlin
suspend fun syncContact(contact: Contact): Contact {
    return try {
        val response = apiService.createContact(contact.toCreateRequest())
        // Contact created successfully
        response.contact
    } catch (e: HttpException) {
        when (e.code()) {
            409 -> {
                // Contact already exists (cross-user duplicate)
                // This should not happen in normal sync - handle as error
                throw IllegalStateException("Contact already exists for another user")
            }
            else -> throw e
        }
    }
}

// For handling existing contacts (same user updates)
suspend fun updateOrCreateContact(contact: Contact): Contact {
    return try {
        val response = apiService.createContact(contact.toCreateRequest())
        response.contact // Created or updated
    } catch (e: HttpException) {
        if (e.code() == 200) {
            // Server returned existing contact (same user)
            // Parse the response to get the contact data
            // This happens when same user tries to create duplicate
            parseContactFromResponse(e.response()?.body()?.string())
        } else {
            throw e
        }
    }
}
```

## Performance Considerations

- **Batch Operations**: Upload calls in batches of 100-500
- **Pagination**: Use reasonable page sizes (20-50 items)
- **Background Sync**: Perform sync operations in background threads
- **Retry Logic**: Implement exponential backoff for failed requests
- **Caching**: Cache frequently accessed data locally
- **Delta Sync**: Only sync changed data since last successful sync

## Testing

### Unit Tests
```kotlin
@Test
fun testContactSync() = runBlocking {
    val contact = Contact(/* test data */)
    val result = syncContact(contact)

    assertNotNull(result.id)
    assertEquals(contact.phoneNormalized, result.phoneNormalized)
}
```

### Integration Tests
- Test authentication flow
- Test pagination handling
- Test conflict resolution
- Test bulk upload limits
- Test network error recovery

## Deployment Checklist

- [ ] API base URL configured for environment
- [ ] Authentication token storage implemented
- [ ] SSL certificate validation enabled
- [ ] Network timeout configurations set
- [ ] Error handling and user feedback implemented
- [ ] Background sync service configured
- [ ] Database schema matches API models
- [ ] ProGuard rules updated for API models
