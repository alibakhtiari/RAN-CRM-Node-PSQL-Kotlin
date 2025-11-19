package com.ran.crm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val timestamp = remember(callLog.timestamp) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(callLog.timestamp)
            if (date != null) {
                dateFormat.format(date)
            } else {
                callLog.timestamp
            }
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
                // Show contact info if available
                callLog.contactId?.let {
                    Text(
                        text = "Contact ID: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = durationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
