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
import com.ran.crm.ui.screen.LoginScreen
import com.ran.crm.ui.theme.RANCRMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RANCRMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrmApp()
                }
            }
        }
    }
}

@Composable
fun CrmApp() {
    // TODO: Implement navigation and main app structure
    // For now, show login screen
    LoginScreen(onLoginSuccess = {
        // TODO: Navigate to main screen
    })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RANCRMTheme {
        // TODO: Add preview content
    }
}
