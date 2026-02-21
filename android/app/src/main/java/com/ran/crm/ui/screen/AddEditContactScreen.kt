package com.ran.crm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ran.crm.data.repository.ContactRepository
import com.ran.crm.utils.PhoneUtils
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
        contactId: String?,
        currentUserId: String?,
        onBackClick: () -> Unit,
        contactRepository: ContactRepository
) {
    var name by remember { mutableStateOf("") }
    var phoneRaw by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Load existing contact if editing
    LaunchedEffect(contactId) {
        if (contactId != null && contactId != "new") {
            isEditMode = true
            val contact = contactRepository.getContactById(contactId)
            if (contact != null) {
                // If the user isn't the owner, they shouldn't be here
                if (currentUserId != null && contact.createdBy != currentUserId) {
                    onBackClick()
                    return@LaunchedEffect
                }
                name = contact.name
                phoneRaw = contact.phoneRaw
            }
        } else {
            isEditMode = false
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(if (isEditMode) "Edit Contact" else "Add Contact") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                    value = phoneRaw,
                    onValueChange = { phoneRaw = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    supportingText = {
                        Text("Enter phone number with country code (e.g., +1234567890)")
                    }
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = {
                        scope.launch {
                            if (name.isBlank() || phoneRaw.isBlank()) {
                                errorMessage = "Please fill in all fields"
                                return@launch
                            }

                            val phoneNormalized = PhoneUtils.normalizePhoneNumber(phoneRaw)
                            if (phoneNormalized == null) {
                                errorMessage = "Invalid phone number format"
                                return@launch
                            }

                            isLoading = true
                            errorMessage = null

                            try {
                                if (isEditMode && contactId != null && contactId != "new") {
                                    // Update existing contact
                                    val existingContact =
                                            contactRepository.getContactById(contactId)
                                    if (existingContact != null) {
                                        val updatedContact =
                                                existingContact.copy(
                                                        name = name,
                                                        phoneRaw = phoneRaw,
                                                        phoneNormalized = phoneNormalized,
                                                        updatedAt =
                                                                java.text.SimpleDateFormat(
                                                                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                                                                Locale.US
                                                                        )
                                                                        .apply {
                                                                            timeZone =
                                                                                    TimeZone.getTimeZone(
                                                                                            "UTC"
                                                                                    )
                                                                        }
                                                                        .format(Date())
                                                )
                                        contactRepository.updateContact(updatedContact)
                                        onBackClick()
                                    }
                                } else {
                                    // Create new contact
                                    val newContact =
                                            contactRepository.createContact(
                                                    name = name,
                                                    phoneRaw = phoneRaw,
                                                    phoneNormalized = phoneNormalized
                                            )
                                    if (newContact != null) {
                                        onBackClick()
                                    } else {
                                        errorMessage = "Failed to create contact"
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditMode) "Update Contact" else "Add Contact")
                }
            }
        }
    }
}
