package com.unlone.app.auth

interface AuthRepository {
    suspend fun signUp(email: String, password: String): AuthResult<Unit>
    suspend fun signUpEmail(email: String): AuthResult<Unit>
    suspend fun signIn(email: String, password: String): AuthResult<Unit>
    suspend fun authenticate(): AuthResult<Unit>
    suspend fun isUserLoggedIn(): Boolean
    fun signOut(): Boolean

}