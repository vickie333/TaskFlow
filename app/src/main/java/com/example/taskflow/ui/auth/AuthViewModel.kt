package com.example.taskflow.ui.auth

import androidx.lifecycle.ViewModel
import com.example.taskflow.data.AccountSession
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    session: AccountSession
) : ViewModel() {
    val uid: StateFlow<String?> = session.uid
    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg

    fun register(email: String, password: String) {
        _msg.value = null
        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { result ->
            _msg.value = if (result.isSuccessful) null else result.exception?.localizedMessage ?: "No se pudo crear la cuenta"
        }
    }
    fun login(email: String, password: String) {
        _msg.value = null
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { result ->
            _msg.value = if (result.isSuccessful) null else result.exception?.localizedMessage ?: "No se pudo iniciar sesión"
        }
    }
    fun signOut() { auth.signOut(); _msg.value = null }
}
