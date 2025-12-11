package com.ran.crm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ran.crm.R
import com.ran.crm.data.local.entity.CallLog  
import com.ran.crm.data.repository.CallLogRepository
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogsScreen(
    onBackClick: () -> Unit,
    callLogRepository: CallLogRepository
) {
    var callLogs by remember { mutableStateOf<List<CallLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        callLogRepository.getAllCallLogs().collectLatest<List<CallLog>> { logs ->
            callLogs = logs
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.call_logs)) },
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
            } else if (callLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_data),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(callLogs.size) { index ->
                        GlobalCallLogItem(callLog = callLogs[index])
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalCallLogItem(callLog: CallLog) {
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
            // Call direction icon (replacing contact photo)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = when (callLog.direction) {
                            "incoming" -> Color(0xFF4CAF50) // Green
                            "outgoing" -> Color(0xFF2196F3) // Blue
                            "missed" -> Color(0xFFF44336)   // Red
                            else -> Color.Gray
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (callLog.direction) {
                        "incoming" -> Icons.Default.Call
                        "outgoing" -> Icons.Default.Phone
                        "missed" -> Icons.Default.Close
                        else -> Icons.Default.Phone
                    },
                    contentDescription = callLog.direction,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Contact name or phone number
                Text(
                    text = callLog.contactName ?: callLog.phoneNumber ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // User who uploaded this log
                Text(
                    text = "${callLog.userName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                // Timestamp
                Text(
                    text = formatTimestamp(callLog.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Duration
            Text(
                text = formatDuration(callLog.durationSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(timestamp)
        val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        if (date != null) outputFormat.format(date) else timestamp
    } catch (e: Exception) {
        timestamp
    }
}

fun formatDuration(durationSeconds: Int): String {
    return if (durationSeconds > 0) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    } else {
        "0:00"
    }
}
