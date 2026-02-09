package com.example.buscardapp

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<String?>(null)
    val authState: StateFlow<String?> = _authState

    private val auth = SupabaseClient.supabase.auth
    private val WEB_CLIENT_ID = "744647664470-8odukj93lh37a56vdvom0ha3qiefo8fr.apps.googleusercontent.com"

    // Função auxiliar para o Nonce do Google
    private fun generateNonce(): Pair<String, String> {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
        return Pair(rawNonce, hashedNonce)
    }

    // --- NOVA FUNÇÃO GOOGLE ---
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = "A conectar com Google..."
            val (rawNonce, hashedNonce) = generateNonce()
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

                auth.signInWith(IDToken) {
                    idToken = credential.idToken
                    provider = Google
                    nonce = rawNonce
                }
                _authState.value = "Login efetuado!"
            } catch (e: Exception) {
                _authState.value = "Erro Google: ${e.localizedMessage}"
            }
        }
    }

    // --- CÓDIGO ORIGINAL MANTIDO ---
    fun signUpWithEmail(emailInput: String, passInput: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A criar conta..."
                auth.signUpWith(Email) {
                    email = emailInput
                    password = passInput
                    data = buildJsonObject {
                        put("first_name", firstName)
                        put("last_name", lastName)
                    }
                }
                _authState.value = "Código enviado! Verifique o seu e-mail."
            } catch (e: Exception) {
                _authState.value = "Erro: ${e.localizedMessage}"
            }
        }
    }

    fun verifyOtp(emailInput: String, codeInput: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A validar código..."
                auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = emailInput, token = codeInput)
                _authState.value = "Login efetuado!"
            } catch (e: Exception) {
                _authState.value = "Código inválido."
            }
        }
    }

    fun signInWithEmail(emailInput: String, passInput: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A entrar..."
                auth.signInWith(Email) {
                    email = emailInput
                    password = passInput
                }
                _authState.value = "Login efetuado!"
            } catch (e: Exception) {
                _authState.value = "Erro nas credenciais."
            }
        }
    }

    // No AuthViewModel.kt
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = "Logout efetuado" // Isto fará o NavGraph voltar ao ecrã de Login
            } catch (e: Exception) {
                _authState.value = "Erro ao sair: ${e.localizedMessage}"
            }
        }
    }
}