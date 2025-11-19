package com.ran.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies
        database = (application as CrmApplication).database
        authRepository = AuthRepository()
        preferenceManager = com.ran.crm.data.local.PreferenceManager(this)

        enableEdgeToEdge()
        setContent {
            RANCRMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrmApp(database, authRepository, preferenceManager)
                }
            }
        }
    }
}

@Composable
fun CrmApp(
    database: CrmDatabase,
    authRepository: AuthRepository,
    preferenceManager: com.ran.crm.data.local.PreferenceManager
) {
    val navController = rememberNavController()

    NavGraph(
        navController = navController,
        database = database,
        authRepository = authRepository,
        preferenceManager = preferenceManager
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RANCRMTheme {
        // TODO: Add preview content
    }
}
