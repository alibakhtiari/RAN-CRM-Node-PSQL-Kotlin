package com.ran.crm.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ran.crm.data.local.CrmDatabase
import com.ran.crm.data.repository.AuthRepository
import com.ran.crm.data.repository.CallLogRepository
import com.ran.crm.data.repository.ContactRepository
import com.ran.crm.ui.screen.*

@Composable
fun NavGraph(
    navController: NavHostController,
    database: CrmDatabase,
    authRepository: AuthRepository,
    preferenceManager: com.ran.crm.data.local.PreferenceManager
) {
    val contactRepository = ContactRepository(database.contactDao(), preferenceManager)
    val callLogRepository = CallLogRepository(database.callLogDao(), preferenceManager)

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Contacts.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                authRepository = authRepository
            )
        }

        composable(Screen.Contacts.route) {
            ContactsScreen(
                onContactClick = { contact ->
                    navController.navigate(Screen.ContactDetail.createRoute(contact.id))
                },
                onCallLogsClick = {
                    navController.navigate(Screen.CallLogs.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAddContactClick = {
                    navController.navigate(Screen.AddEditContact.createRoute())
                },
                contactRepository = contactRepository
            )
        }

        composable(Screen.ContactDetail.route) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(
                contactId = contactId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.AddEditContact.createRoute(id))
                },
                contactRepository = contactRepository,
                callLogRepository = callLogRepository
            )
        }

        composable(Screen.CallLogs.route) {
            CallLogsScreen(
                onBackClick = { navController.popBackStack() },
                callLogRepository = callLogRepository
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                preferenceManager = preferenceManager
            )
        }

        composable(Screen.AddEditContact.route) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")
            AddEditContactScreen(
                contactId = contactId,
                onBackClick = { navController.popBackStack() },
                contactRepository = contactRepository
            )
        }

        composable(Screen.SyncLogs.route) {
            SyncLogsScreen(
                onBackClick = { navController.popBackStack() },
                database = database
            )
        }
    }
}
