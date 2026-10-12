package com.example.taskflow.data.sync

import com.example.taskflow.data.AccountSession
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncRunnerTest {
    private val session = object : AccountSession {
        override val uid = MutableStateFlow<String?>("alice")
    }

    @Test fun staleWorkerDoesNotSyncAnotherAccount() = runTest {
        var ran = false
        val result = runAccountSync("bob", session) { ran = true }
        assertEquals(SyncOutcome.Obsolete, result)
        assertFalse(ran)
    }
    @Test fun networkFailureRetriesButPermissionDeniedStops() = runTest {
        assertTrue(runAccountSync("alice", session) { throw java.io.IOException("Offline") } is SyncOutcome.Retry)
        assertTrue(runAccountSync("alice", session) {
            throw FirebaseFirestoreException("Denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        } is SyncOutcome.Failed)
    }
    @Test fun externalCancellationPropagates() = runTest {
        try {
            runAccountSync("alice", session) { throw CancellationException("Stopped") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
    @Test fun ownTimeoutRetries() = runTest {
        assertTrue(runAccountSync("alice", session, timeoutMillis = 10) { awaitCancellation() } is SyncOutcome.Retry)
    }
    @Test fun changingAccountCancelsNetworkOperation() = runTest {
        var cancelled = false
        val result = runAccountSync("alice", session) {
            try { session.uid.value = "bob"; awaitCancellation() }
            finally { cancelled = true }
        }
        assertEquals(SyncOutcome.Obsolete, result)
        assertTrue(cancelled)
    }
}
