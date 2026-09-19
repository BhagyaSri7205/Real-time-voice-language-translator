package com.voxtranslate.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voxtranslate.app.data.db.AppDatabase
import com.voxtranslate.app.data.db.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mindrot.jbcrypt.BCrypt

private val Context.authDataStore by preferencesDataStore(name = "vox_auth")

sealed class AuthResult {
    data class Success(val username: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Local username/password accounts, stored on-device only — mirrors the
 * original desktop app's `backend/auth.py` (bcrypt-hashed passwords in a
 * local SQLite `users` table). There's no server, so accounts don't sync
 * across devices; this just keeps the app single-user and private like the
 * original did. The signed-in username is remembered between app launches
 * via DataStore, so people aren't asked to log in every time they open
 * the app — only after they explicitly log out.
 */
class AuthRepository(private val context: Context, private val database: AppDatabase) {

    private object Keys {
        val CURRENT_USERNAME = stringPreferencesKey("current_username")
    }

    val currentUsernameFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USERNAME]
    }

    suspend fun signUp(usernameInput: String, password: String): AuthResult {
        val username = usernameInput.trim()

        if (username.isEmpty() || password.isEmpty()) {
            return AuthResult.Failure("Username and password are required.")
        }
        if (password.length < 6) {
            return AuthResult.Failure("Password must be at least 6 characters.")
        }
        if (database.userDao().getByUsername(username) != null) {
            return AuthResult.Failure("Username already exists.")
        }

        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        database.userDao().insert(
            UserEntity(username = username, passwordHash = hash, createdAt = System.currentTimeMillis())
        )
        rememberSignedIn(username)
        return AuthResult.Success(username)
    }

    suspend fun logIn(usernameInput: String, password: String): AuthResult {
        val username = usernameInput.trim()

        if (username.isEmpty() || password.isEmpty()) {
            return AuthResult.Failure("Username and password are required.")
        }

        val user = database.userDao().getByUsername(username)
            ?: return AuthResult.Failure("No account found with that username.")

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return AuthResult.Failure("Incorrect password.")
        }

        rememberSignedIn(username)
        return AuthResult.Success(username)
    }

    suspend fun logOut() {
        context.authDataStore.edit { it.remove(Keys.CURRENT_USERNAME) }
    }

    private suspend fun rememberSignedIn(username: String) {
        context.authDataStore.edit { it[Keys.CURRENT_USERNAME] = username }
    }
}
