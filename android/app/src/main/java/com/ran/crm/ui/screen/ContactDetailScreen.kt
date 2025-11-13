package com.ran.crm.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ran.crm.R
import com.ran.crm.data.local.entity.CallLog
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.repository.CallLogRepository
import com.ran.crm.data.repository.ContactRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: String,
    onBackClick: () -> Unit,
    contactRepository: ContactRepository,
    callLogRepository: CallLogRepository
) {
    var contact by remember { mutableStateOf<Contact?>(null) }
    var callLogs by remember { mutableStateOf<List<CallLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(contactId) {
        // Load contact details
        contact = contactRepository.getContactById(contactId)

        // Load call logs for this contact
        if (contact != null) {
            callLogRepository.getCallLogsForContact(contactId).collectLatest { logs ->
                callLogs = logs
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact?.name ?: stringResource(R.string.contacts)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Contact actions
                    contact?.let {
                        IconButton(onClick = {
                            // Handle call action
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = stringResource(R.string.call)
                            )
                        }
                        IconButton(onClick = {
                            // Handle message action
                        }) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = stringResource(R.string.message)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                contact?.let { contact ->
                    // Contact info section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = contact.phoneRaw,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Call history section
                    Text(
                        text = "Call History",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (callLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No call history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(callLogs) { callLog ->
                                CallLogItem(callLog = callLog)
                            }
                        }
                    }
                } ?: run {
                    // Contact not found
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Contact not found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogItem(callLog: CallLog) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val timestamp = remember(callLog.timestamp) {
        try {
            val instant = kotlinx.datetime.Instant.parse(callLog.timestamp)
            dateFormat.format(Date(instant.toEpochMilliseconds()))
        } catch (e: Exception) {
            callLog.timestamp
        }
    }

    val directionIcon = when (callLog.direction) {
        "incoming" -> "📥"
        "outgoing" -> "📤"
        "missed" -> "📞"
        else -> "📞"
    }

    val durationText = if (callLog.durationSeconds > 0) {
        val minutes = callLog.durationSeconds / 60
        val seconds = callLog.durationSeconds % 60
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    } else {
        when (callLog.direction) {
            "missed" -> "Missed"
            else -> "0:00"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = directionIcon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = callLog.direction.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = durationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
