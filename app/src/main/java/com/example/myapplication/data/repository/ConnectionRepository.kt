package com.example.myapplication.data.repository

import com.example.myapplication.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads a user's connections (the people they're connected with) from Firestore.
 * Reused by News (share picker), Profile, Messages, and the share-receive flow.
 */
@Singleton
class ConnectionRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /** Loads up to [limit] connected member profiles for [uid]. Returns empty on any failure. */
    suspend fun loadConnections(uid: String, limit: Int = 20): List<UserProfile> {
        return try {
            val doc = db.collection("connections").document(uid).get().await()
            val uids = (doc.get("connectedUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            uids.take(limit).mapNotNull { cUid ->
                try {
                    db.collection("users").document(cUid).get().await()
                        .toObject(UserProfile::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
