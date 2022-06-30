package com.unlone.app.data.auth

import co.touchlab.kermit.Logger
import com.unlone.app.utils.KMMPreference
import io.ktor.client.call.*
import io.ktor.client.plugins.*

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val prefs: KMMPreference,
) : AuthRepository {

    override suspend fun signUp(
        email: String,
        password: String
    ): AuthResult<Unit> {
        return try {
            api.signUp(
                request = AuthRequest(
                    email = email,
                    password = password,
                )
            )
            signIn(email, password)
        } catch (e: RedirectResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ClientRequestException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ServerResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: Exception) {
            Logger.e(e.toString())
            AuthResult.UnknownError()
        }
    }

    override suspend fun signInEmail(email: String): AuthResult<Unit> {
        return try {
            api.validateEmail(
                request = AuthEmailRequest(
                    email = email,
                )
            )
            AuthResult.Authorized()
        } catch (e: RedirectResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ClientRequestException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ServerResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: Exception) {
            Logger.e(e.toString())
            AuthResult.UnknownError()
        }
    }

    override suspend fun signUpEmail(email: String): AuthResult<Unit> {
        return try {
            api.checkEmailExisted(
                request = AuthEmailRequest(
                    email = email,
                )
            )
            AuthResult.Authorized()
        } catch (e: RedirectResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ClientRequestException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ServerResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: Exception) {
            Logger.e(e.toString())
            AuthResult.UnknownError()
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> {
        return try {
            val response = api.signIn(
                request = AuthRequest(
                    email = email,
                    password = password,
                )
            )
            prefs.put(JWT_SP_KEY, response.token)
            AuthResult.Authorized()
        } catch (e: RedirectResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ClientRequestException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ServerResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: Exception) {
            Logger.e(e.toString())
            AuthResult.UnknownError()
        }
    }

    override suspend fun authenticate(): AuthResult<Unit> {
        return try {
            val token = prefs.getString(JWT_SP_KEY)
                ?: return AuthResult.Unauthorized(null)
            api.authenticate("Bearer $token")
            AuthResult.Authorized()
        } catch (e: RedirectResponseException) {
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ClientRequestException) {
            prefs.remove(JWT_SP_KEY)
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: ServerResponseException) {
            prefs.remove(JWT_SP_KEY)
            AuthResult.Unauthorized(errorMsg = e.response.body<String>())
            // todo
        } catch (e: Exception) {
            AuthResult.UnknownError()
        }
    }


    override fun signOut() {
        prefs.remove(JWT_SP_KEY)
    }

    companion object {
        private const val JWT_SP_KEY = "jwt"
    }

}