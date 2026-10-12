package com.example.taskflow.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncPhase { IDLE, WAITING, RUNNING, SUCCESS, ERROR }
data class SyncStatus(val phase: SyncPhase = SyncPhase.IDLE, val message: String? = null)

@Singleton
class SyncStatusStore @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("sync-status", Context.MODE_PRIVATE)
    private val states = MutableStateFlow<Map<String, SyncStatus>>(emptyMap())
    fun observe(uid: String) = states.map { it[uid] ?: restored(uid) }.distinctUntilChanged()
    fun isBlocked(uid: String) = preferences.contains("blocked:$uid")
    private fun restored(uid: String): SyncStatus = preferences.getString("blocked:$uid", null)?.let {
        SyncStatus(SyncPhase.ERROR, it)
    } ?: SyncStatus()
    fun update(uid: String, status: SyncStatus) { states.update { it + (uid to status) } }
    fun queued(uid: String) {
        states.update {
            if (it[uid]?.phase == SyncPhase.RUNNING) it else it + (uid to SyncStatus(SyncPhase.WAITING))
        }
    }
    fun block(uid: String, message: String) {
        preferences.edit().putString("blocked:$uid", message).apply()
        update(uid, SyncStatus(SyncPhase.ERROR, message))
    }
    fun unblock(uid: String) {
        preferences.edit().remove("blocked:$uid").apply()
        update(uid, SyncStatus())
    }
}
