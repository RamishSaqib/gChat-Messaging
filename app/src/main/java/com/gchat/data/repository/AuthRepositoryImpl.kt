package com.gchat.data.repository

import com.gchat.data.local.dao.UserDao
import com.gchat.data.mapper.UserMapper
import com.gchat.data.remote.firestore.FirestoreUserDataSource
import com.gchat.domain.model.AuthResult
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val firestoreUserDataSource: FirestoreUserDataSource
) : AuthRepository {
    
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            trySend(firebaseUser?.let {
                User(
                    id = it.uid,
                    displayName = it.displayName ?: "",
                    email = it.email,
                    phoneNumber = it.phoneNumber
                )
            })
        }
        
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    override val isAuthenticated: Flow<Boolean> = currentUser.map { it != null }
    
    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Login failed")
            
            // Fetch user from Firestore and cache locally
            val userResult = firestoreUserDataSource.getUser(firebaseUser.uid)
            userResult.fold(
                onSuccess = { user ->
                    // Update online status
                    firestoreUserDataSource.updateOnlineStatus(firebaseUser.uid, true)
                    
                    // Update local cache with online status
                    val updatedUser = user.copy(isOnline = true, lastSeen = System.currentTimeMillis())
                    userDao.insert(UserMapper.toEntity(updatedUser))
                    AuthResult.Success(updatedUser)
                },
                onFailure = { AuthResult.Error(it.message ?: "Failed to fetch user data") }
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Login failed")
        }
    }
    
    override suspend fun register(email: String, password: String, displayName: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Registration failed")
            
            // Update display name in Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Create user in Firestore
            val user = User(
                id = firebaseUser.uid,
                displayName = displayName,
                email = email,
                phoneNumber = null,
                preferredLanguage = "en",
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            
            val createResult = firestoreUserDataSource.createUser(user)
            createResult.fold(
                onSuccess = {
                    userDao.insert(UserMapper.toEntity(user))
                    AuthResult.Success(user)
                },
                onFailure = { AuthResult.Error(it.message ?: "Failed to create user profile") }
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }
    
    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Google Sign-In failed")
            
            // Check if user exists in Firestore, if not create
            val userResult = firestoreUserDataSource.getUser(firebaseUser.uid)
            userResult.fold(
                onSuccess = { user ->
                    // User exists, update online status
                    firestoreUserDataSource.updateOnlineStatus(firebaseUser.uid, true)
                    
                    // Update local cache with online status
                    val updatedUser = user.copy(isOnline = true, lastSeen = System.currentTimeMillis())
                    userDao.insert(UserMapper.toEntity(updatedUser))
                    AuthResult.Success(updatedUser)
                },
                onFailure = {
                    // User doesn't exist, create new user
                    val newUser = User(
                        id = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email,
                        phoneNumber = firebaseUser.phoneNumber,
                        profilePictureUrl = firebaseUser.photoUrl?.toString(),
                        preferredLanguage = "en",
                        isOnline = true,
                        lastSeen = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val createResult = firestoreUserDataSource.createUser(newUser)
                    createResult.fold(
                        onSuccess = {
                            userDao.insert(UserMapper.toEntity(newUser))
                            AuthResult.Success(newUser)
                        },
                        onFailure = { error -> 
                            AuthResult.Error(error.message ?: "Failed to create user profile") 
                        }
                    )
                }
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google Sign-In failed")
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Update online status before logout
                firestoreUserDataSource.updateOnlineStatus(userId, false)
            }
            
            auth.signOut()
            
            // Clear local cache
            userDao.deleteAll()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    override suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Result<Unit> {
        return firestoreUserDataSource.updateUser(userId, updates)
    }
}

