package com.ran.crm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.work.SyncWorker
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for settings
    var syncInterval by remember { mutableStateOf(15) } // Default 15 mins
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Sync Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Sync Interval
            Text("Sync Interval (minutes)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(15, 30, 60).forEach { interval ->
                    FilterChip(
                        selected = syncInterval == interval,
                        onClick = { 
                            syncInterval = interval
                            SyncWorker.schedulePeriodicSync(context, interval)
                        },
                        label = { Text("$interval m") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Persistent Notification")
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { 
                        notificationsEnabled = it
                        // TODO: Implement notification toggle logic if needed
                        // WorkManager notification handling is usually done in the Worker itself
                        // or via a ForegroundService. 
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Sync Logs (Placeholder)
            Button(
                onClick = { /* TODO: Navigate to Sync Logs */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Sync Logs")
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Logout
            Button(
                onClick = {
                    scope.launch {
                        preferenceManager.clear()
                        // Cancel background work
                        SyncWorker.cancelPeriodicSync(context)
                        // Navigate to Login
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}
