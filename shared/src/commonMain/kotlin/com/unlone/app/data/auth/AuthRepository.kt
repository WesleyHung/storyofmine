package com.unlone.app.data.auth


interface AuthRepository {
    suspend fun signUpEmail(email: String): AuthResult<Unit>
    suspend fun signUp(email: String, password: String): AuthResult<Unit>
    suspend fun signInEmail(email: String): AuthResult<Unit>
    suspend fun signIn(email: String, password: String): AuthResult<Unit>
    suspend fun authenticate(): AuthResult<Unit>
    fun signOut()
}