package com.ran.crm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ran.crm.data.repository.AuthRepository
import com.ran.crm.navigation.NavGraph
import com.ran.crm.ui.theme.RANCRMTheme
import com.ran.crm.data.local.CrmDatabase

class MainActivity : ComponentActivity() {

    private lateinit var database: CrmDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var preferenceManager: com.ran.crm.data.local.PreferenceManager
    private lateinit var contactMigrationManager: com.ran.crm.data.manager.ContactMigrationManager

    private val logoutReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.ran.crm.ACTION_LOGOUT") {
                android.util.Log.d("MainActivity", "Logout broadcast received")
                
                // Clear session
                val preferenceManager = com.ran.crm.data.local.PreferenceManager(applicationContext)
                preferenceManager.clearSession()
                com.ran.crm.data.remote.ApiClient.setAuthToken(null)
                
                // Navigate to Login
                runOnUiThread {
                    try {
                        // Restart activity to ensure clean state
                        val restartIntent = Intent(this@MainActivity, MainActivity::class.java)
                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(restartIntent)
                        finish()
                        
                        android.widget.Toast.makeText(this@MainActivity, "Session expired. Please login again.", android.widget.Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to handle logout navigation", e)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies
        database = (application as CrmApplication).database
        preferenceManager = com.ran.crm.data.local.PreferenceManager(this)
        
        // Restore token
        preferenceManager.authToken?.let { token ->
            com.ran.crm.data.remote.ApiClient.setAuthToken(token)
        }

        authRepository = AuthRepository(preferenceManager)
        val contactRepository = com.ran.crm.data.repository.ContactRepository(database.contactDao(), preferenceManager)
        contactMigrationManager = com.ran.crm.data.manager.ContactMigrationManager(this, contactRepository)

        // Register logout receiver
        val filter = android.content.IntentFilter("com.ran.crm.ACTION_LOGOUT")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logoutReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logoutReceiver, filter)
        }

        // Ensure Sync Account Exists
        com.ran.crm.sync.AccountHelper.ensureAccountExists(this)

        enableEdgeToEdge()
        setContent {
            RANCRMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrmApp(database, authRepository, preferenceManager, contactMigrationManager)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
}

@Composable
fun CrmApp(
    database: CrmDatabase,
    authRepository: AuthRepository,
    preferenceManager: com.ran.crm.data.local.PreferenceManager,
    contactMigrationManager: com.ran.crm.data.manager.ContactMigrationManager
) {
    val navController = rememberNavController()
    
    // Permission Handling
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissions = remember {
        mutableListOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Handle permission results if needed
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        launcher.launch(permissions.toTypedArray())
    }

    NavGraph(
        navController = navController,
        database = database,
        authRepository = authRepository,
        preferenceManager = preferenceManager,
        contactMigrationManager = contactMigrationManager
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RANCRMTheme {
        // TODO: Add preview content
    }
}
