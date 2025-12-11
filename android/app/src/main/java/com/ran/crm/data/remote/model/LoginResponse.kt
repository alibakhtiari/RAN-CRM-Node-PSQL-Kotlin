package com.ran.crm.data.remote.model

import com.ran.crm.data.local.entity.User

data class LoginResponse(
    val token: String,
    val user: User
)
