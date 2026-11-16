package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

class AuthRepository(private val auth: FirebaseAuth = Firebase.auth) {

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun attemptSilentSignIn(context: Context): GoogleSignInResult {
        val existing = auth.currentUser
        if (existing != null) return GoogleSignInResult.Success(existing)

        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
            ?: return GoogleSignInResult.Cancelled

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            handleCredentialResult(result.credential)
        } catch (_: Exception) {
            GoogleSignInResult.Cancelled
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): GoogleSignInResult {
        val clientId = try {
            activityContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return GoogleSignInResult.Error("Google Sign-In configuration missing: default_web_client_id not found")
        }

        val credentialManager = CredentialManager.create(activityContext)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val activity = activityContext as? Activity
                ?: return GoogleSignInResult.Error("Activity context required for Google Sign-In")
            val result = credentialManager.getCredential(activity, request)
            handleCredentialResult(result.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            GoogleSignInResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed")
        }
    }

    private suspend fun handleCredentialResult(credential: androidx.credentials.Credential): GoogleSignInResult {
        return if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            val user = authResult.user
            if (user != null) GoogleSignInResult.Success(user)
            else GoogleSignInResult.Error("Authentication returned null user")
        } else {
            GoogleSignInResult.Error("Unexpected credential type")
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
            }
        }
    }

    suspend fun deleteAccount(context: Context): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No authenticated user")
        user.delete().await()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
