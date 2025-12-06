package com.company.employeetracker.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun isValidEmail(e: String) =
        android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = null
                message = null
            },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        if (message != null) {
            Text(message!!, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank()) {
                    error = "Please enter your email."
                    return@Button
                }
                if (!isValidEmail(email)) {
                    error = "Please enter a valid email address."
                    return@Button
                }
                loading = true
                error = null
                message = null

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        loading = false
                        if (task.isSuccessful) {
                            message = "Password reset email sent. Check your inbox (and spam)."
                        } else {
                            val exc = task.exception
                            // Customize messages
                            message = null
                            error = when (exc) {
                                is FirebaseAuthInvalidUserException -> "No user found with that email."
                                is FirebaseAuthInvalidCredentialsException -> "Email address appears invalid."
                                else -> "Failed to send reset email. Please try again."
                            }
                        }
                    }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("Send reset email")
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Back to sign in")
        }
    }
}