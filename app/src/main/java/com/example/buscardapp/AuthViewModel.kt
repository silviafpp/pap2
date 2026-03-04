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
import io.github.jan.supabase.postgrest.postgrest
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

    private val auth      = SupabaseClient.supabase.auth
    private val postgrest = SupabaseClient.supabase.postgrest
    private val WEB_CLIENT_ID = "674496926042-i3andsfd92nme1akv1cnteiniv269437.apps.googleusercontent.com"

    private fun generateNonce(): Pair<String, String> {
        val rawNonce = UUID.randomUUID().toString()
        val bytes    = rawNonce.toByteArray()
        val digest   = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hashed   = digest.fold("") { str, it -> str + "%02x".format(it) }
        return Pair(rawNonce, hashed)
    }

    // ── Google Sign-In com verificação de email duplicado ─────────────────────
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = "A conectar com Google..."
            val (rawNonce, hashedNonce) = generateNonce()
            try {
                // 1. Obtém o token Google primeiro (sem fazer login no Supabase ainda)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()

                val request    = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result     = CredentialManager.create(context).getCredential(context, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val googleEmail = credential.id  // email da conta Google selecionada

                // 2. Verifica se este email já existe como conta de email/password
                //    Tenta fazer sign-in com email — se der erro de credenciais,
                //    o email existe mas é conta manual → bloqueia o Google login
                val emailExisteComoManual = verificarEmailExisteComoManual(googleEmail)
                if (emailExisteComoManual) {
                    _authState.value = "Erro: Este email já está registado com password. Por favor entre com o seu email e password."
                    return@launch
                }

                // 3. Tudo bem — prossegue com o login Google
                auth.signInWith(IDToken) {
                    idToken  = credential.idToken
                    provider = Google
                    nonce    = rawNonce
                }
                _authState.value = "Login efetuado!"

            } catch (e: Exception) {
                _authState.value = "Erro Google: ${e.localizedMessage}"
            }
        }
    }

    // Verifica se o email existe como conta manual (email+password) no Supabase
    // Faz isso consultando a tabela profiles pelo email
    private suspend fun verificarEmailExisteComoManual(email: String): Boolean {
        return try {
            // Consulta a tabela profiles — se existir com provider = 'email', é conta manual
            val resultado = postgrest["profiles"]
                .select { filter { eq("email", email) } }
                .decodeSingleOrNull<PerfilEmail>()

            // Se encontrou perfil E o provider é "email" (não Google), bloqueia
            resultado != null && (resultado.provider == "email" || resultado.provider == null)
        } catch (e: Exception) {
            // Se der erro na query (ex: coluna não existe), deixa passar para não bloquear
            false
        }
    }

    // ── Registo com Email ─────────────────────────────────────────────────────
    fun signUpWithEmail(emailInput: String, passInput: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A criar conta..."
                auth.signUpWith(Email) {
                    email    = emailInput
                    password = passInput
                    data     = buildJsonObject {
                        put("first_name", firstName)
                        put("last_name",  lastName)
                    }
                }
                _authState.value = "Código enviado! Verifique o seu e-mail."
            } catch (e: Exception) {
                // Se o email já existe no Supabase Auth, mostra erro claro
                val msg = e.localizedMessage ?: ""
                _authState.value = when {
                    msg.contains("already registered", ignoreCase = true) ||
                            msg.contains("already exists",    ignoreCase = true) ||
                            msg.contains("User already",      ignoreCase = true) ->
                        "Erro: Este email já está registado. Por favor entre com a sua conta."
                    else -> "Erro: $msg"
                }
            }
        }
    }

    // ── Verificar OTP ─────────────────────────────────────────────────────────
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

    // ── Login com Email ───────────────────────────────────────────────────────
    fun signInWithEmail(emailInput: String, passInput: String) {
        viewModelScope.launch {
            try {
                _authState.value = "A entrar..."
                auth.signInWith(Email) {
                    email    = emailInput
                    password = passInput
                }
                _authState.value = "Login efetuado!"
            } catch (e: Exception) {
                _authState.value = "Erro nas credenciais."
            }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseClient.supabase.auth.signOut()
                _authState.value = "Logout efetuado"
            } catch (e: Exception) {
                _authState.value = "Erro ao sair: ${e.message}"
            }
        }
    }

    fun signOut() = logout()
}

// Data class auxiliar para verificar o provider do perfil
@kotlinx.serialization.Serializable
private data class PerfilEmail(
    val email: String? = null,
    val provider: String? = null
)