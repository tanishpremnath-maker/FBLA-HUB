package com.example.myapplication.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the current authentication state.
 * Screens/ViewModels ask this instead of calling FirebaseAuth.getInstance() directly.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid
    val currentUserName: String get() = auth.currentUser?.displayName ?: ""

    fun signOut() = auth.signOut()
}
