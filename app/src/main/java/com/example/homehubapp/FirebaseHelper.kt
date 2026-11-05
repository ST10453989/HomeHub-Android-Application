package com.example.homehubapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

// Makes sure there is a Firebase session before we touch Firestore or Storage.
// Our security rules require request.auth != null, so without a session every call fails.
object FirebaseHelper {

    fun ensureSignedIn(onReady: () -> Unit, onError: (Exception) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onReady()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.i("HomeHub", "Firebase anonymous session started")
                onReady()
            }
            .addOnFailureListener {
                Log.e("HomeHub", "Firebase anonymous sign-in failed", it)
                onError(it)
            }
    }
}