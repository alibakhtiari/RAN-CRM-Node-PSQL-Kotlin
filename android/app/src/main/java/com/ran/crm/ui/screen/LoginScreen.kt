package com.ran.crm.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ran.crm.R
import com.ran.crm.data.repository.AuthRepository
import com.ran.crm.work.SyncWorker
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, authRepository: AuthRepository) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val focusManager = LocalFocusManager.current

        Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions =
                                KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions =
                                KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                ),
                        keyboardActions =
                                KeyboardActions(
                                        onDone = {
                                                focusManager.clearFocus()
                                                if (username.isNotBlank() && password.isNotBlank()
                                                ) {
                                                        scope.launch {
                                                                isLoading = true
                                                                errorMessage = null

                                                                val result =
                                                                        authRepository.login(
                                                                                username,
                                                                                password
                                                                        )
                                                                result.fold(
                                                                        onSuccess = {
                                                                                SyncWorker
                                                                                        .scheduleOneTimeSync(
                                                                                                context,
                                                                                                forceFullSync =
                                                                                                        true
                                                                                        )
                                                                                onLoginSuccess()
                                                                        },
                                                                        onFailure = { exception ->
                                                                                errorMessage =
                                                                                        exception
                                                                                                .message
                                                                                                ?: context.getString(
                                                                                                        R.string
                                                                                                                .login_failed
                                                                                                )
                                                                        }
                                                                )

                                                                isLoading = false
                                                        }
                                                }
                                        }
                                )
                )

                Spacer(modifier = Modifier.height(16.dp))

                errorMessage?.let {
                        Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                        onClick = {
                                focusManager.clearFocus()
                                if (username.isNotBlank() && password.isNotBlank()) {
                                        scope.launch {
                                                isLoading = true
                                                errorMessage = null

                                                val result =
                                                        authRepository.login(username, password)
                                                result.fold(
                                                        onSuccess = {
                                                                SyncWorker.scheduleOneTimeSync(
                                                                        context,
                                                                        forceFullSync = true
                                                                )
                                                                onLoginSuccess()
                                                        },
                                                        onFailure = { exception ->
                                                                errorMessage =
                                                                        exception.message
                                                                                ?: context.getString(
                                                                                        R.string
                                                                                                .login_failed
                                                                                )
                                                        }
                                                )

                                                isLoading = false
                                        }
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                        if (isLoading) {
                                // This CircularProgressIndicator is inside the Button's composable
                                // lambda
                                // which is a valid place for it.
                                CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                )
                        } else {
                                Text(stringResource(R.string.login))
                        }
                }
        }
}
