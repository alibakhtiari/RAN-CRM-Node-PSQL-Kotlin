package com.ran.crm.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.data.manager.ContactMigrationManager
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.ui.AppConfig
import com.ran.crm.work.SyncWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        navController: NavController,
        preferenceManager: PreferenceManager,
        contactMigrationManager: ContactMigrationManager,
        onBackClick: () -> Unit
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Sync Status
        var isSyncing by remember { mutableStateOf(false) }
        var lastSyncTime by remember { mutableStateOf(preferenceManager.lastSyncContacts) }

        // Server Status
        var isServerConnected by remember { mutableStateOf<Boolean?>(null) }

        // Check server status periodically
        LaunchedEffect(Unit) {
                while (true) {
                        try {
                                val response = ApiClient.apiService.healthCheck()
                                isServerConnected = response.status == "OK"
                        } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Health check failed", e)
                                isServerConnected = false
                        }
                        kotlinx.coroutines.delay(30000) // Check every 30 seconds
                }
        }

        // Force recomposition when permissions change
        var permissionRefreshCount by remember { mutableIntStateOf(0) }

        // Permission launcher
        val permissionLauncher =
                androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts
                                .RequestMultiplePermissions()
                ) { permissionRefreshCount++ }

        // Local state for immediate UI feedback
        var currentTheme by remember { mutableStateOf(preferenceManager.appTheme) }
        var currentScale by remember { mutableFloatStateOf(preferenceManager.fontScale) }
        var currentInterval by remember { mutableIntStateOf(preferenceManager.syncIntervalMinutes) }

        // Permissions State
        val permissions = remember {
                buildList {
                        add("Read Contacts" to android.Manifest.permission.READ_CONTACTS)
                        add("Write Contacts" to android.Manifest.permission.WRITE_CONTACTS)
                        add("Call Log" to android.Manifest.permission.READ_CALL_LOG)
                        if (android.os.Build.VERSION.SDK_INT >=
                                        android.os.Build.VERSION_CODES.TIRAMISU
                        ) {
                                add(
                                        "Notifications" to
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                )
                        }
                }
        }

        // Observe WorkManager
        DisposableEffect(Unit) {
                val observer =
                        androidx.lifecycle.Observer<List<androidx.work.WorkInfo>> { workInfos ->
                                val syncWorkInfo = workInfos.firstOrNull()
                                if (syncWorkInfo != null) {
                                        isSyncing =
                                                syncWorkInfo.state ==
                                                        androidx.work.WorkInfo.State.RUNNING ||
                                                        syncWorkInfo.state ==
                                                                androidx.work.WorkInfo.State
                                                                        .ENQUEUED

                                        if (syncWorkInfo.state ==
                                                        androidx.work.WorkInfo.State.SUCCEEDED
                                        ) {
                                                lastSyncTime = preferenceManager.lastSyncContacts
                                                isSyncing = false
                                        } else if (syncWorkInfo.state ==
                                                        androidx.work.WorkInfo.State.FAILED ||
                                                        syncWorkInfo.state ==
                                                                androidx.work.WorkInfo.State
                                                                        .CANCELLED
                                        ) {
                                                isSyncing = false
                                        }
                                }
                        }
                val liveData =
                        androidx.work.WorkManager.getInstance(context)
                                .getWorkInfosForUniqueWorkLiveData("crm_sync_work_one_time")

                liveData.observeForever(observer)
                onDispose { liveData.removeObserver(observer) }
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Settings") },
                                navigationIcon = {
                                        IconButton(onClick = onBackClick) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                }
                        )
                }
        ) { paddingValues ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(paddingValues)
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                ) {
                        // 1. Server Status
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "Server Status",
                                                style = MaterialTheme.typography.bodyMedium
                                        )

                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                when (isServerConnected) {
                                                        true -> {
                                                                androidx.compose.foundation.Canvas(
                                                                        modifier =
                                                                                Modifier.size(12.dp)
                                                                ) {
                                                                        drawCircle(
                                                                                color =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .Color
                                                                                                .Green
                                                                        )
                                                                }
                                                                Text(
                                                                        text = "Connected",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
                                                        false -> {
                                                                androidx.compose.foundation.Canvas(
                                                                        modifier =
                                                                                Modifier.size(12.dp)
                                                                ) {
                                                                        drawCircle(
                                                                                color =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .Color
                                                                                                .Red
                                                                        )
                                                                }
                                                                Text(
                                                                        text = "Disconnected",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                )
                                                        }
                                                        null -> {
                                                                CircularProgressIndicator(
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        12.dp
                                                                                ),
                                                                        strokeWidth = 1.5.dp
                                                                )
                                                                Text(
                                                                        text = "Checking...",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Sync Data
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                                Column {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text = "Sync Data",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )

                                                        val relativeTime =
                                                                if (lastSyncTime > 0) {
                                                                        android.text.format
                                                                                .DateUtils
                                                                                .getRelativeTimeSpanString(
                                                                                        lastSyncTime,
                                                                                        System.currentTimeMillis(),
                                                                                        android.text
                                                                                                .format
                                                                                                .DateUtils
                                                                                                .MINUTE_IN_MILLIS
                                                                                )
                                                                                .toString()
                                                                } else {
                                                                        "Never"
                                                                }

                                                        Text(
                                                                text = relativeTime,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }

                                                Button(
                                                        onClick = {
                                                                SyncWorker.scheduleOneTimeSync(
                                                                        context,
                                                                        forceFullSync = true
                                                                )
                                                        },
                                                        enabled = !isSyncing,
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 16.dp,
                                                                        vertical = 8.dp
                                                                )
                                                ) {
                                                        if (isSyncing) {
                                                                CircularProgressIndicator(
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        16.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary,
                                                                        strokeWidth = 2.dp
                                                                )
                                                        } else {
                                                                Text("Sync")
                                                        }
                                                }
                                        }

                                        HorizontalDivider()

                                        Text(
                                                text = "Import Contacts",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier =
                                                        Modifier.padding(start = 16.dp, top = 8.dp)
                                        )

                                        var isImporting by remember { mutableStateOf(false) }

                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Import from Device",
                                                        style = MaterialTheme.typography.bodySmall
                                                )

                                                Button(
                                                        onClick = {
                                                                scope.launch {
                                                                        isImporting = true
                                                                        val count =
                                                                                contactMigrationManager
                                                                                        .importSystemContacts(
                                                                                                false
                                                                                        )
                                                                        isImporting = false
                                                                        android.widget.Toast
                                                                                .makeText(
                                                                                        context,
                                                                                        "Imported $count contacts",
                                                                                        android.widget
                                                                                                .Toast
                                                                                                .LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        },
                                                        enabled = !isImporting,
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 16.dp,
                                                                        vertical = 8.dp
                                                                )
                                                ) {
                                                        if (isImporting) {
                                                                CircularProgressIndicator(
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        16.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary,
                                                                        strokeWidth = 2.dp
                                                                )
                                                        } else {
                                                                Text("Import")
                                                        }
                                                }
                                        }

                                        HorizontalDivider()

                                        Text(
                                                text = "Sync Interval",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier =
                                                        Modifier.padding(start = 16.dp, top = 8.dp)
                                        )

                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                ThemeOption(
                                                        label = "15m",
                                                        selected = currentInterval == 15,
                                                        onClick = {
                                                                preferenceManager
                                                                        .syncIntervalMinutes = 15
                                                                currentInterval = 15
                                                                SyncWorker.schedulePeriodicSync(
                                                                        context,
                                                                        15
                                                                )
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "30m",
                                                        selected = currentInterval == 30,
                                                        onClick = {
                                                                preferenceManager
                                                                        .syncIntervalMinutes = 30
                                                                currentInterval = 30
                                                                SyncWorker.schedulePeriodicSync(
                                                                        context,
                                                                        30
                                                                )
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "1h",
                                                        selected = currentInterval == 60,
                                                        onClick = {
                                                                preferenceManager
                                                                        .syncIntervalMinutes = 60
                                                                currentInterval = 60
                                                                SyncWorker.schedulePeriodicSync(
                                                                        context,
                                                                        60
                                                                )
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "Manual",
                                                        selected = currentInterval == 0,
                                                        onClick = {
                                                                preferenceManager
                                                                        .syncIntervalMinutes = 0
                                                                currentInterval = 0
                                                                SyncWorker.cancelPeriodicSync(
                                                                        context
                                                                )
                                                        }
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2b. Battery Optimization
                        val powerManager = remember {
                                context.getSystemService(Context.POWER_SERVICE) as
                                        android.os.PowerManager
                        }
                        var isIgnoringBattery by remember {
                                mutableStateOf(
                                        powerManager.isIgnoringBatteryOptimizations(
                                                context.packageName
                                        )
                                )
                        }

                        if (!isIgnoringBattery) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .errorContainer
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text = "Battery Optimization",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )
                                                        Text(
                                                                text =
                                                                        "Disable battery optimization to keep sync running in background",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onErrorContainer
                                                        )
                                                }
                                                Button(
                                                        onClick = {
                                                                try {
                                                                        val intent =
                                                                                android.content
                                                                                        .Intent(
                                                                                                android.provider
                                                                                                        .Settings
                                                                                                        .ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                                                                        )
                                                                        intent.data =
                                                                                android.net.Uri
                                                                                        .parse(
                                                                                                "package:${context.packageName}"
                                                                                        )
                                                                        context.startActivity(
                                                                                intent
                                                                        )
                                                                        // Re-check after returning
                                                                        isIgnoringBattery =
                                                                                powerManager
                                                                                        .isIgnoringBatteryOptimizations(
                                                                                                context.packageName
                                                                                        )
                                                                } catch (e: Exception) {
                                                                        android.widget.Toast
                                                                                .makeText(
                                                                                        context,
                                                                                        "Could not open battery settings",
                                                                                        android.widget
                                                                                                .Toast
                                                                                                .LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        },
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 16.dp,
                                                                        vertical = 8.dp
                                                                )
                                                ) { Text("Disable") }
                                        }
                                }
                        }

                        // 3. Appearance (Font Size & Theme)
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                                text = "Appearance",
                                                style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Theme
                                        Text(
                                                text = "Theme",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                ThemeOption(
                                                        label = "System",
                                                        selected = currentTheme == "system",
                                                        onClick = {
                                                                preferenceManager.appTheme =
                                                                        "system"
                                                                currentTheme = "system"
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "Light",
                                                        selected = currentTheme == "light",
                                                        onClick = {
                                                                preferenceManager.appTheme = "light"
                                                                currentTheme = "light"
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "Dark",
                                                        selected = currentTheme == "dark",
                                                        onClick = {
                                                                preferenceManager.appTheme = "dark"
                                                                currentTheme = "dark"
                                                        }
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Font Size
                                        Text(
                                                text = "Font Size",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                ThemeOption(
                                                        label = "Small",
                                                        selected =
                                                                currentScale ==
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_SMALL,
                                                        onClick = {
                                                                preferenceManager.fontScale =
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_SMALL
                                                                currentScale =
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_SMALL
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "Medium",
                                                        selected =
                                                                currentScale ==
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_MEDIUM,
                                                        onClick = {
                                                                preferenceManager.fontScale =
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_MEDIUM
                                                                currentScale =
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_MEDIUM
                                                        }
                                                )
                                                ThemeOption(
                                                        label = "Large",
                                                        selected =
                                                                currentScale ==
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_LARGE,
                                                        onClick = {
                                                                preferenceManager.fontScale =
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_LARGE
                                                                currentScale =
                                                                        com.ran.crm.ui.AppConfig
                                                                                .SCALE_LARGE
                                                        }
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Permissions (Compact)
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Permissions",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                                TextButton(
                                                        onClick = {
                                                                try {
                                                                        val intent =
                                                                                android.content
                                                                                        .Intent(
                                                                                                android.provider
                                                                                                        .Settings
                                                                                                        .ACTION_APPLICATION_DETAILS_SETTINGS
                                                                                        )
                                                                                        .apply {
                                                                                                data =
                                                                                                        android.net
                                                                                                                .Uri
                                                                                                                .parse(
                                                                                                                        "package:${context.packageName}"
                                                                                                                )
                                                                                        }
                                                                        context.startActivity(
                                                                                intent
                                                                        )
                                                                } catch (e: Exception) {}
                                                        }
                                                ) { Text("Open Settings") }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Regular permissions
                                        permissions.forEach { (label, permission) ->
                                                val isGranted =
                                                        androidx.core.content.ContextCompat
                                                                .checkSelfPermission(
                                                                        context,
                                                                        permission
                                                                ) ==
                                                                android.content.pm.PackageManager
                                                                        .PERMISSION_GRANTED

                                                PermissionItem(
                                                        label = label,
                                                        isGranted = isGranted,
                                                        onRequestPermission = {
                                                                permissionLauncher.launch(
                                                                        arrayOf(permission)
                                                                )
                                                        }
                                                )
                                        }

                                        // Battery Optimization
                                        val pm =
                                                context.getSystemService(
                                                        android.content.Context.POWER_SERVICE
                                                ) as
                                                        android.os.PowerManager
                                        val isIgnoring =
                                                if (android.os.Build.VERSION.SDK_INT >=
                                                                android.os.Build.VERSION_CODES.M
                                                ) {
                                                        pm.isIgnoringBatteryOptimizations(
                                                                context.packageName
                                                        )
                                                } else {
                                                        true
                                                }

                                        PermissionItem(
                                                label = "Battery Opt.",
                                                isGranted = isIgnoring,
                                                onRequestPermission = {
                                                        if (android.os.Build.VERSION.SDK_INT >=
                                                                        android.os.Build
                                                                                .VERSION_CODES
                                                                                .M
                                                        ) {
                                                                try {
                                                                        val intent =
                                                                                android.content
                                                                                        .Intent(
                                                                                                android.provider
                                                                                                        .Settings
                                                                                                        .ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                                                                        )
                                                                                        .apply {
                                                                                                data =
                                                                                                        android.net
                                                                                                                .Uri
                                                                                                                .parse(
                                                                                                                        "package:${context.packageName}"
                                                                                                                )
                                                                                        }
                                                                        context.startActivity(
                                                                                intent
                                                                        )
                                                                } catch (e: Exception) {
                                                                        try {
                                                                                val intent =
                                                                                        android.content
                                                                                                .Intent(
                                                                                                        android.provider
                                                                                                                .Settings
                                                                                                                .ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                                                                                )
                                                                                context.startActivity(
                                                                                        intent
                                                                                )
                                                                        } catch (e2: Exception) {}
                                                                }
                                                        }
                                                }
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                                onClick = {
                                        scope.launch {
                                                preferenceManager.clear()
                                                navController.navigate("login") {
                                                        popUpTo(navController.graph.id) {
                                                                inclusive = true
                                                        }
                                                }
                                        }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                        )
                        ) { Text("Logout") }
                }
        }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onClick)
        ) {
                RadioButton(selected = selected, onClick = onClick)
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                )
        }
}

@Composable
private fun PermissionItem(label: String, isGranted: Boolean, onRequestPermission: () -> Unit) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                ) {
                        if (isGranted) {
                                Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                        onClick = onRequestPermission,
                        enabled = !isGranted,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(if (isGranted) "Granted" else "Grant") }
        }
}
