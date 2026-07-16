package com.example.taskflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel(), onLoginClick: () -> Unit) {
    var password by remember() { mutableStateOf("") }
    var email by rememberSaveable() { mutableStateOf("") }
    val msg by viewModel.msg.collectAsState()
    val isLogged by viewModel.isLogged.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(email, label = { Text("Email") }, onValueChange = {
            text -> email = text
        })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, label = { Text("Password") }, onValueChange = {
            text -> password = text
        }, visualTransformation = PasswordVisualTransformation())

        Row(modifier = Modifier.padding(16.dp)) {
            Button(onClick = {
                viewModel.login(email, password)
            }) {
                Text("Entrar")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.register(email, password)
            }) {
                Text("Registrarse")
            }
        }

        if (msg != null) {
            Text("$msg")
        }
        if (isLogged) {
            onLoginClick()
        }
    }
}