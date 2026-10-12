package com.example.taskflow.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AccountSession {
    val uid: StateFlow<String?>
    fun currentUid(): String? = uid.value
}

@Singleton
class FirebaseAccountSession @Inject constructor(private val auth: FirebaseAuth) : AccountSession {
    private val state = MutableStateFlow(auth.currentUser?.uid)
    override val uid: StateFlow<String?> = state
    private val listener = FirebaseAuth.AuthStateListener { state.value = it.currentUser?.uid }
    init { auth.addAuthStateListener(listener) }
    override fun currentUid(): String? = auth.currentUser?.uid
}

interface SyncScheduler { fun requestSync(uid: String) }

internal fun AccountSession.requireUid(): String = requireNotNull(currentUid()?.takeIf { it.isNotBlank() && it != "USER" }) { "Se necesita una sesión" }
internal fun nextTimestamp(previous: Long): Long = maxOf(System.currentTimeMillis(), Math.addExact(previous, 1))
