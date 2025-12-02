package com.ran.crm.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ran.crm.R
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.repository.ContactRepository
import com.ran.crm.utils.T9Utils
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
        onContactClick: (Contact) -> Unit,
        onCallLogsClick: () -> Unit,
        onSettingsClick: () -> Unit,
        onAddContactClick: () -> Unit,
        contactRepository: ContactRepository
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        contactRepository.getAllContacts().collectLatest { contactList ->
            contacts = contactList
            isLoading = false
        }
    }

    val filteredContacts =
            remember(contacts, searchQuery) {
                if (searchQuery.isBlank()) {
                    contacts
                } else {
                    contacts.filter { contact ->
                        T9Utils.advancedSearch(contact.name, searchQuery) ||
                                contact.phoneRaw.contains(searchQuery)
                    }
                }
            }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(stringResource(R.string.contacts)) },
                        actions = {
                            TextButton(onClick = onCallLogsClick) {
                                Text(stringResource(R.string.call_logs))
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                )
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddContactClick) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Contact")
                }
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            text = stringResource(R.string.no_data),
                            style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredContacts.size) { index ->
                        val contact = filteredContacts[index]
                        ContactItem(
                                contact = contact,
                                onContactClick = onContactClick,
                                onCallClick = {
                                    val intent =
                                            android.content.Intent(
                                                            android.content.Intent.ACTION_DIAL
                                                    )
                                                    .apply {
                                                        data =
                                                                android.net.Uri.parse(
                                                                        "tel:${contact.phoneRaw}"
                                                                )
                                                    }
                                    context.startActivity(intent)
                                },
                                onMessageClick = {
                                    val intent =
                                            android.content.Intent(
                                                            android.content.Intent.ACTION_VIEW
                                                    )
                                                    .apply {
                                                        data =
                                                                android.net.Uri.parse(
                                                                        "sms:${contact.phoneRaw}"
                                                                )
                                                    }
                                    context.startActivity(intent)
                                },
                                onWhatsAppClick = {
                                    try {
                                        val phoneNumber = contact.phoneNormalized.replace("+", "")
                                        val intent =
                                                android.content.Intent(
                                                                android.content.Intent.ACTION_VIEW
                                                        )
                                                        .apply {
                                                            data =
                                                                    android.net.Uri.parse(
                                                                            "https://wa.me/$phoneNumber"
                                                                    )
                                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // WhatsApp not installed or error
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(
        contact: Contact,
        onContactClick: (Contact) -> Unit,
        onCallClick: () -> Unit,
        onMessageClick: () -> Unit,
        onWhatsAppClick: () -> Unit
) {
    Card(
            modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable {
                        onContactClick(contact)
                    },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                    if (!contact.creatorName.isNullOrBlank()) {
                        Text(
                                text = " (${contact.creatorName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                        text = contact.phoneRaw,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCallClick) {
                    Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = stringResource(R.string.call),
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onMessageClick) {
                    Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(R.string.message),
                            tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onWhatsAppClick) {
                    Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            tint = androidx.compose.ui.graphics.Color(0xFF25D366)
                    )
                }
            }
        }
    }
}
