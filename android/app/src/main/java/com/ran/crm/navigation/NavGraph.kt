package com.ran.crm.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ran.crm.data.local.CrmDatabase
import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.data.manager.ContactMigrationManager
import com.ran.crm.data.repository.AuthRepository
import com.ran.crm.data.repository.CallLogRepository
import com.ran.crm.data.repository.ContactRepository
import com.ran.crm.ui.screen.*

@Composable
fun NavGraph(
        navController: NavHostController,
        database: CrmDatabase,
        authRepository: AuthRepository,
        preferenceManager: PreferenceManager,
        contactMigrationManager: ContactMigrationManager
) {
    val contactRepository = ContactRepository(database.contactDao(), preferenceManager)
    val callLogRepository = CallLogRepository(database.callLogDao(), preferenceManager)

    val startDestination =
            if (authRepository.isLoggedIn()) Screen.Contacts.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
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
                    onCallLogsClick = { navController.navigate(Screen.CallLogs.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onAddContactClick = {
                        navController.navigate(Screen.AddEditContact.createRoute("new"))
                    },
                    contactRepository = contactRepository,
                    isUserAdmin = preferenceManager.isAdmin
            )
        }

        composable(Screen.ContactDetail.route) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
            ContactDetailScreen(
                    contactId = contactId,
                    currentUserId = preferenceManager.userId,
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
                    preferenceManager = preferenceManager,
                    onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AddEditContact.route) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")
            AddEditContactScreen(
                    contactId = contactId,
                    currentUserId = preferenceManager.userId,
                    onBackClick = { navController.popBackStack() },
                    contactRepository = contactRepository
            )
        }

        composable(Screen.SyncLogs.route) {
            SyncLogsScreen(onBackClick = { navController.popBackStack() }, database = database)
        }
    }
}
