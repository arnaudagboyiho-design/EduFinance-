package com.example.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.model.UserRole
import com.example.model.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ServiceAuth : Gestionnaire centralisé d'authentification Firebase pour EduFinance Pro.
 * 
 * Implémente l'authentification moderne sécurisée Firebase Auth via Google Sign-In
 * et Jetpack Credential Manager (GetSignInWithGoogleOption), avec observation
 * réactive des sessions (StateFlow) et passerelle vers UserSession de l'application.
 */
class ServiceAuth(
    private val customAuth: FirebaseAuth? = null
) {

    companion object {
        private const val TAG = "ServiceAuth"

        @Volatile
        private var instance: ServiceAuth? = null

        fun getInstance(auth: FirebaseAuth? = null): ServiceAuth {
            return instance ?: synchronized(this) {
                instance ?: ServiceAuth(auth).also { instance = it }
            }
        }
    }

    private val auth: FirebaseAuth?
        get() = customAuth ?: try {
            Firebase.auth
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth inaccessible : ${e.message}")
            null
        }

    // Flux réactif de l'état utilisateur Firebase
    private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    // Flux réactif de la session applicative EduFinance
    private val _currentSessionState = MutableStateFlow<UserSession?>(null)
    val currentSessionState: StateFlow<UserSession?> = _currentSessionState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUserState.value = user
        _currentSessionState.value = user?.let { mapFirebaseUserToSession(it) }
        Log.d(TAG, "AuthStateChanged: user=${user?.email}, uid=${user?.uid}")
    }

    init {
        try {
            val currentAuth = auth
            _currentUserState.value = currentAuth?.currentUser
            _currentSessionState.value = currentAuth?.currentUser?.let { mapFirebaseUserToSession(it) }
            currentAuth?.addAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.w(TAG, "Initialisation ServiceAuth en mode résilient: ${e.message}")
        }
    }

    /**
     * Utilisateur actuellement connecté (null si déconnecté).
     */
    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    /**
     * Indique si un utilisateur est actuellement authentifié.
     */
    val isUserSignedIn: Boolean
        get() = auth?.currentUser != null

    /**
     * Identifiant unique Firebase (UID) de l'utilisateur actif.
     */
    val currentUserId: String?
        get() = auth?.currentUser?.uid

    /**
     * Adresse e-mail de l'utilisateur actif.
     */
    val currentUserEmail: String?
        get() = auth?.currentUser?.email

    /**
     * Nom complet ou pseudo de l'utilisateur actif.
     */
    val currentUserDisplayName: String?
        get() = auth?.currentUser?.displayName

    /**
     * URL de l'avatar de l'utilisateur actif.
     */
    val currentUserPhotoUrl: Uri?
        get() = auth?.currentUser?.photoUrl

    /**
     * Connexion interactive avec Google via Jetpack Credential Manager.
     * Utilise GetSignInWithGoogleOption pour présenter le sélecteur de compte.
     *
     * @param activity Contexte d'activité requis par CredentialManager pour afficher la boîte de dialogue.
     * @param serverClientId Web Client ID Firebase (optionnel si disponible dans les ressources).
     */
    suspend fun signInWithGoogle(
        activity: Activity,
        serverClientId: String? = null
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val clientId = serverClientId ?: resolveWebClientId(activity)
        if (clientId.isNullOrBlank()) {
            val errorMsg = "Web Client ID introuvable. Configurez Firebase ou passez le serverClientId à ServiceAuth."
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(IllegalStateException(errorMsg))
        }

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                return@withContext signInWithGoogleIdToken(googleIdToken)
            } else {
                val errorMsg = "Type de credential inattendu : ${credential.type}"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Connexion Google annulée ou fermée par l'utilisateur: ${e.message}", e)
            return@withContext Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Erreur CredentialManager lors de la connexion Google: ${e.message}", e)
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Échec imprévu lors de la connexion Google: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Connexion à Firebase à partir d'un ID Token Google déjà obtenu.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth n'est pas initialisé"))
        try {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = currentAuth.signInWithCredential(authCredential).await()
            val user = authResult.user
            if (user != null) {
                Log.i(TAG, "Connexion Firebase réussie pour : ${user.email} (UID: ${user.uid})")
                _currentUserState.value = user
                _currentSessionState.value = mapFirebaseUserToSession(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Utilisateur Firebase introuvable après authentification"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Échec de signInWithCredential : ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Connexion via un Custom Token Firebase (émis par un backend sécurisé).
     */
    suspend fun signInWithCustomToken(customToken: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth n'est pas initialisé"))
        try {
            val authResult = currentAuth.signInWithCustomToken(customToken).await()
            val user = authResult.user
            if (user != null) {
                Log.i(TAG, "Connexion Custom Token réussie pour : ${user.email} (UID: ${user.uid})")
                _currentUserState.value = user
                _currentSessionState.value = mapFirebaseUserToSession(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Utilisateur Firebase introuvable"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Échec de signInWithCustomToken : ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Tente une connexion silencieuse en arrière-plan sans boîte de dialogue intrusive.
     * Pratique au démarrage de l'application si l'utilisateur s'est déjà connecté.
     */
    suspend fun attemptSilentSignIn(
        context: Context,
        serverClientId: String? = null
    ): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        // Si déjà connecté dans l'instance Firebase Auth
        auth?.currentUser?.let { user ->
            Log.d(TAG, "Utilisateur déjà connecté en cache : ${user.email}")
            return@withContext Result.success(user)
        }

        val clientId = serverClientId ?: resolveWebClientId(context)
        if (clientId.isNullOrBlank()) {
            return@withContext Result.success(null)
        }

        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authResult = signInWithGoogleIdToken(googleIdToken)
                return@withContext authResult.map { it as FirebaseUser? }
            }
            Result.success(null)
        } catch (e: NoCredentialException) {
            Log.d(TAG, "Aucun identifiant pré-enregistré pour la connexion silencieuse.")
            Result.success(null)
        } catch (e: Exception) {
            Log.d(TAG, "Connexion silencieuse non disponible : ${e.message}")
            Result.success(null)
        }
    }

    /**
     * Déconnexion complète : termine la session Firebase et réinitialise l'état de CredentialManager.
     */
    suspend fun signOut(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth?.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUserState.value = null
            _currentSessionState.value = null
            Log.i(TAG, "Déconnexion Firebase et nettoyage CredentialManager réussis.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la déconnexion : ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Rafraîchit les informations de l'utilisateur actif (jeton, profil, etc.).
     */
    suspend fun reloadUser(): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val user = auth?.currentUser
            user?.reload()?.await()
            val updatedUser = auth?.currentUser
            _currentUserState.value = updatedUser
            _currentSessionState.value = updatedUser?.let { mapFirebaseUserToSession(it) }
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du rechargement de l'utilisateur : ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime le compte utilisateur Firebase actuellement connecté.
     */
    suspend fun deleteAccount(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = auth?.currentUser ?: return@withContext Result.failure(IllegalStateException("Aucun utilisateur connecté"))
            user.delete().await()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUserState.value = null
            _currentSessionState.value = null
            Log.i(TAG, "Compte utilisateur supprimé avec succès.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression du compte : ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Envoie un e-mail de réinitialisation de mot de passe Firebase Auth.
     * Déclenche l'envoi d'un lien sécurisé permettant à l'utilisateur de redéfinir son mot de passe.
     *
     * @param email L'adresse e-mail associée au compte Firebase Auth.
     * @return Result<Unit> avec succès ou échec détaillé (e-mail invalide, utilisateur introuvable, etc.).
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            val errorMsg = "Veuillez renseigner votre adresse e-mail."
            Log.w(TAG, errorMsg)
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }

        val isEmailPatternValid = try {
            Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
        } catch (e: Throwable) {
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(trimmedEmail)
        }

        if (!isEmailPatternValid) {
            val errorMsg = "Format d'adresse e-mail invalide (ex: utilisateur@ecole.ci)."
            Log.w(TAG, errorMsg)
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }

        val currentAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase Auth n'est pas initialisé ou indisponible.")
        )

        try {
            currentAuth.sendPasswordResetEmail(trimmedEmail).await()
            Log.i(TAG, "E-mail de réinitialisation de mot de passe envoyé avec succès à : $trimmedEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'envoi du mot de passe oublié pour $trimmedEmail: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Convertit un utilisateur Firebase (FirebaseUser) en session applicative EduFinance (UserSession).
     * Attribue automatiquement un rôle (ADMIN, DIRECTEUR, COMPTABLE, SECRETAIRE, ENSEIGNANT)
     * en fonction du profil ou de l'adresse e-mail.
     */
    fun mapFirebaseUserToSession(
        user: FirebaseUser,
        defaultRole: UserRole = UserRole.ENSEIGNANT
    ): UserSession {
        val email = user.email.orEmpty().lowercase()
        val name = user.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").replace(".", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                .ifEmpty { "Utilisateur" }

        // Détermination du rôle d'établissement selon l'e-mail
        val role = when {
            email.startsWith("admin") || email.contains("directeur") -> UserRole.ADMIN
            email.contains("compta") -> UserRole.COMPTABLE
            email.contains("secret") -> UserRole.SECRETAIRE
            else -> defaultRole
        }

        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "US" }

        return UserSession(
            id = user.uid,
            name = name,
            email = email,
            role = role,
            avatarInitials = initials
        )
    }

    /**
     * Résout de manière sécurisée et résiliente le Web Client ID depuis les ressources
     * sans provoquer de crash si google-services.json est temporairement absent.
     */
    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            try {
                context.getString(resId)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
