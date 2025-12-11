package com.ran.crm.utils

import android.Manifest
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PermissionsManagerTest {

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mockContext: Context
    private lateinit var mockLifecycleOwner: LifecycleOwner
    private lateinit var mockActivity: ComponentActivity

    @Before
    fun setup() {
        mockContext = mockk()
        mockLifecycleOwner = mockk()
        mockActivity = mockk()

        // Mock context as activity for permission launcher
        every { mockContext as? ComponentActivity } returns mockActivity

        permissionsManager = PermissionsManager(mockContext, mockLifecycleOwner)
    }

    @Test
    fun `hasContactsPermission should return true when permission granted`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CONTACTS)
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionsManager.hasContactsPermission()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasContactsPermission should return false when permission denied`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CONTACTS)
        } returns android.content.pm.PackageManager.PERMISSION_DENIED

        // When
        val result = permissionsManager.hasContactsPermission()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasCallLogPermission should return true when permission granted`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CALL_LOG)
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionsManager.hasCallLogPermission()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasCallLogPermission should return false when permission denied`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CALL_LOG)
        } returns android.content.pm.PackageManager.PERMISSION_DENIED

        // When
        val result = permissionsManager.hasCallLogPermission()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasAllRequiredPermissions should return true when both permissions granted`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, any())
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionsManager.hasAllRequiredPermissions()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasAllRequiredPermissions should return false when contacts permission denied`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CONTACTS)
        } returns android.content.pm.PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CALL_LOG)
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        // When
        val result = permissionsManager.hasAllRequiredPermissions()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasAllRequiredPermissions should return false when call log permission denied`() {
        // Given
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CONTACTS)
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_CALL_LOG)
        } returns android.content.pm.PackageManager.PERMISSION_DENIED

        // When
        val result = permissionsManager.hasAllRequiredPermissions()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getPermissionDisplayName should return correct display names`() {
        assertEquals("Contacts", permissionsManager.getPermissionDisplayName(Manifest.permission.READ_CONTACTS))
        assertEquals("Call Logs", permissionsManager.getPermissionDisplayName(Manifest.permission.READ_CALL_LOG))
        assertEquals("Unknown", permissionsManager.getPermissionDisplayName("unknown.permission"))
    }

    @Test
    fun `shouldShowRationale should delegate to activity when context is activity`() {
        // Given
        every { mockActivity.shouldShowRequestPermissionRationale(any()) } returns true

        // When
        val result = permissionsManager.shouldShowRationale(Manifest.permission.READ_CONTACTS)

        // Then
        assertTrue(result)
        verify { mockActivity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) }
    }

    @Test
    fun `shouldShowRationale should return false when context is not activity`() {
        // Given
        val nonActivityContext = mockk<Context>()
        val nonActivityPermissionsManager = PermissionsManager(nonActivityContext, mockLifecycleOwner)

        // When
        val result = nonActivityPermissionsManager.shouldShowRationale(Manifest.permission.READ_CONTACTS)

        // Then
        assertFalse(result)
    }
}
