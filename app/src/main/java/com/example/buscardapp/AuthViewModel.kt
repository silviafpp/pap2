package com.example.buscardapp

import android.content.Context
import android.util.Log
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
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<String?>(null)
    val authState: StateFlow<String?> = _authState

    private val _userExists = MutableStateFlow(false)
    val userExists: StateFlow<Boolean> = _userExists

    // Referência correta ao módulo de autenticação do nosso Singleton
    private val auth = SupabaseClient.supabase.auth

    private val WEB_CLIENT_ID = "744647664470-8odukj93lh37a56vdvom0ha3qiefo8fr.apps.googleusercontent.com"

    private fun generateNonce(): Pair<String, String> {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
        return Pair(rawNonce, hashedNonce)
    }

    // --- GOOGLE ---
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _userExists.value = false
            _authState.value = "A validar conta..."
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
                _authState.value = "Bem-vindo!"
            } catch (e: Exception) {
                _authState.value = "Erro Google: ${e.localizedMessage}"
            }
        }
    }

    // --- REGISTO (Envia Código OTP) ---

    fun signUpWithEmail(emailInput: String, passInput: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A criar conta..."
                // O signUpWith inicia o processo e dispara o e-mail conforme o template do Supabase
                auth.signUpWith(Email) {
                    email = emailInput
                    password = passInput
                }
                // Esta mensagem fará o Compose mostrar o campo do código automaticamente
                _authState.value = "Código enviado! Verifique o seu e-mail."
            } catch (e: Exception) {
                _authState.value = "Erro: ${e.localizedMessage}"
            }
        }
    }

    // --- VERIFICAÇÃO (Onde o user digita os 6 dígitos) ---
    fun verifyOtp(emailInput: String, codeInput: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A validar código..."

                // Usamos OtpType.Email.SIGNUP para confirmar novos registos
                auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = emailInput,
                    token = codeInput
                )

                _authState.value = "Conta confirmada com sucesso!"
            } catch (e: Exception) {
                _authState.value = "Código inválido. Verifique o e-mail novamente."
            }
        }
    }

    // --- LOGIN ---
    fun signInWithEmail(emailInput: String, passInput: String) {
        if (emailInput.isBlank() || passInput.isBlank()) {
            _authState.value = "Dados inválidos"
            return
        }
        viewModelScope.launch {
            try {
                _userExists.value = false
                _authState.value = "A entrar..."

                auth.signInWith(Email) {
                    email = emailInput
                    password = passInput
                }

                // Bloqueio na App se não estiver confirmado
                val user = auth.currentUserOrNull()
                if (user?.emailConfirmedAt == null) {
                    _authState.value = "Por favor, confirme o seu e-mail primeiro."
                    auth.signOut()
                } else {
                    _authState.value = "Login efetuado!"
                }
            } catch (e: Exception) {
                _authState.value = "Credenciais incorretas."
            }
        }
    }
}