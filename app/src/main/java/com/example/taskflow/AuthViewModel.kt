package com.example.taskflow

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: FirebaseAuth): ViewModel() {
    private val _msg = MutableStateFlow<String?>(null)
    private val _isLogged = MutableStateFlow<Boolean>(auth.currentUser != null)
    val msg: StateFlow<String?> = _msg
    val isLogged: StateFlow<Boolean> = _isLogged

    fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "createUserWithEmail:success")
                    _msg.value = "Usuario creado exitosamente"
                    _isLogged.value = true
                } else {
                    Log.w("AuthViewModel", "createUserWithEmail:failure", task.exception)
                    _msg.value = "Usuario no creado. Error: ${task.exception}"
                }
            }
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "signInWithEmailAndPassword:success")
                    _msg.value = "Login successful"
                    _isLogged.value = true
                } else {
                    Log.w("AuthViewModel", "signInWithEmailAndPassword:failure")
                    _msg.value = "Login not successful. Error: ${task.exception}"
                }
            }
    }

    fun signOut(user: FirebaseUser? = auth.currentUser) {
        auth.signOut()
        _isLogged.value = false
        _msg.value = "Usuario no autenticado"
    }
}