package com.ran.crm.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Contacts : Screen("contacts")
    object ContactDetail : Screen("contact_detail/{contactId}") {
        fun createRoute(contactId: String) = "contact_detail/$contactId"
    }
    object CallLogs : Screen("call_logs")
    object Settings : Screen("settings")
}
