package com.example.taskflow.data.sync

import android.database.sqlite.SQLiteException
import android.util.Log
import com.example.taskflow.data.AccountSession
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data object Obsolete : SyncOutcome
    data class Retry(val message: String) : SyncOutcome
    data class Failed(val message: String) : SyncOutcome
}

/** A worker owns one account, even if Firebase switches users while awaiting the network. */
internal suspend fun runAccountSync(
    uid: String,
    session: AccountSession,
    timeoutMillis: Long = 60_000,
    sync: suspend () -> Unit
): SyncOutcome {
    if (uid.isBlank() || uid == "USER" || session.currentUid() != uid) return SyncOutcome.Obsolete
    return try {
        coroutineScope {
            val operation = async {
                withTimeoutOrNull(timeoutMillis) { sync(); SyncOutcome.Success }
                    ?: SyncOutcome.Retry("La sincronización tardó demasiado. Se reintentará automáticamente.")
            }
            val accountChanged = async { session.uid.first { it != uid } }
            try {
                select {
                    operation.onAwait { if (session.currentUid() == uid) it else SyncOutcome.Obsolete }
                    accountChanged.onAwait { SyncOutcome.Obsolete }
                }
            } finally {
                operation.cancel()
                accountChanged.cancel()
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (session.currentUid() != uid) return SyncOutcome.Obsolete
        Log.e("SyncRunner", "Sync failed for account", error)
        val retryable = error is java.io.IOException || error is SQLiteException ||
            (error is FirebaseFirestoreException && error.code in setOf(
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.CANCELLED,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.INTERNAL,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.UNKNOWN
            ))
        when {
            retryable -> SyncOutcome.Retry("No se pudo conectar. Se reintentará automáticamente.")
            error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                SyncOutcome.Failed("Firestore rechazó el acceso. Revisa las reglas y vuelve a intentar.")
            error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                SyncOutcome.Failed("La sesión no es válida. Vuelve a iniciar sesión.")
            else -> SyncOutcome.Failed("No se pudo sincronizar: ${error.message ?: "error de datos"}")
        }
    }
}
