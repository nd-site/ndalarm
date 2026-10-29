package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.firebase.model.CloudAlarm
import com.example.data.firebase.model.CloudRingingHistory
import com.example.data.firebase.model.CloudTrustedNdid
import com.example.data.firebase.model.CloudUserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

import com.google.firebase.auth.FirebaseAuth

class FirestoreAlarmRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { it.toObject(CloudUserProfile::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun saveUserProfile(profile: CloudUserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.set(profile.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Alarms ---
    fun observeAlarms(userId: String): Flow<List<CloudAlarm>> {
        val colRef = db.collection("users").document(userId).collection("alarms")
        return colRef.snapshots()
            .map { snapshot -> snapshot.toObjects(CloudAlarm::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                throw error
            }
    }

    suspend fun saveAlarm(alarm: CloudAlarm): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("alarms")
        val docRef = if (alarm.id.isNotBlank()) colRef.document(alarm.id) else colRef.document()
        try {
            val payload = alarm.copy(id = docRef.id, userId = uid, updatedAt = null)
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAlarm(alarmId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("alarms").document(alarmId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Trusted NDIDs ---
    fun observeTrustedNdids(userId: String): Flow<List<CloudTrustedNdid>> {
        val colRef = db.collection("users").document(userId).collection("trusted_ndids")
        return colRef.snapshots()
            .map { snapshot -> snapshot.toObjects(CloudTrustedNdid::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                throw error
            }
    }

    suspend fun saveTrustedNdid(trustedNdid: CloudTrustedNdid): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("trusted_ndids")
        val docRef = if (trustedNdid.id.isNotBlank()) colRef.document(trustedNdid.id) else colRef.document()
        try {
            val payload = trustedNdid.copy(id = docRef.id, userId = uid)
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTrustedNdid(ndidId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("trusted_ndids").document(ndidId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Ringing History ---
    fun observeRingingHistory(userId: String): Flow<List<CloudRingingHistory>> {
        val colRef = db.collection("users").document(userId).collection("ringing_history")
        return colRef.snapshots()
            .map { snapshot -> snapshot.toObjects(CloudRingingHistory::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                throw error
            }
    }

    suspend fun recordHistory(history: CloudRingingHistory): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("ringing_history")
        val docRef = colRef.document()
        try {
            val payload = history.copy(id = docRef.id, userId = uid)
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteHistory(historyId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("ringing_history").document(historyId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
