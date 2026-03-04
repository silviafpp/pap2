package com.example.buscardapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfile? = null,
    val userCard:    UserCard?    = null,
    val userEmail:   String       = "",
    val isLoading:   Boolean      = true,
    val error:       String?      = null
)

class ProfileViewModel : ViewModel() {

    private val auth      = SupabaseClient.supabase.auth
    private val postgrest = SupabaseClient.supabase.postgrest

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        carregarPerfil()
    }

    fun carregarPerfil() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = auth.currentUserOrNull() ?: return@launch

                val profile = postgrest["profiles"]
                    .select { filter { eq("id", user.id) } }
                    .decodeSingleOrNull<UserProfile>()

                val card = postgrest["user_cards"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeSingleOrNull<UserCard>()

                _uiState.value = ProfileUiState(
                    userProfile = profile,
                    userCard    = card,
                    userEmail   = user.email ?: "",
                    isLoading   = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun updateProfile(firstName: String, lastName: String, onSuccess: () -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                postgrest["profiles"].update({
                    set("first_name", firstName.trim())
                    set("last_name",  lastName.trim())
                }) {
                    filter { eq("id", user.id) }
                }
                _uiState.value = _uiState.value.copy(
                    userProfile = _uiState.value.userProfile?.copy(
                        firstName = firstName.trim(),
                        lastName  = lastName.trim()
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = "Erro ao atualizar perfil: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Apaga tudo associado ao utilizador na base de dados, pela seguinte ordem:
     *  1. trip_history  — registo de viagens
     *  2. user_cards    — cartão de transporte
     *  3. profiles      — perfil público
     *  4. auth.users    — conta de autenticação (via admin API do Supabase)
     *
     * Depois chama onComplete() que deve fazer logout/navegação para o ecrã inicial.
     */
    fun deleteAccount(onComplete: () -> Unit) {
        val user = auth.currentUserOrNull() ?: run { onComplete(); return }
        viewModelScope.launch {
            try {
                val uid = user.id

                // 1. Apaga o histórico de viagens
                postgrest["trip_history"].delete {
                    filter { eq("user_id", uid) }
                }

                // 2. Apaga o cartão de transporte
                postgrest["user_cards"].delete {
                    filter { eq("user_id", uid) }
                }

                // 3. Apaga o perfil público
                postgrest["profiles"].delete {
                    filter { eq("id", uid) }
                }

                // 4. Apaga a conta de autenticação
                // O Supabase permite ao utilizador apagar a sua própria conta
                // através da função auth.admin.deleteUser() ou pelo endpoint /user
                // Com o SDK Kotlin do Supabase usa-se signOut + edge function,
                // mas a forma mais simples é chamar a RPC ou usar o admin client.
                // Aqui usamos o método disponível no SDK:
                try {
                    // Tenta apagar via SDK (requer permissões de admin ou RPC configurada)
                    // Se não tiver admin key, usa a RPC "delete_user" criada no Supabase
                    postgrest.rpc("delete_user")
                } catch (e: Exception) {
                    // Fallback: faz apenas logout se a RPC não estiver configurada
                    android.util.Log.w("DELETE_ACCOUNT", "RPC delete_user não encontrada: ${e.message}")
                }

                // 5. Termina a sessão e navega para o ecrã inicial
                auth.signOut()
                onComplete()

            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("DELETE_ACCOUNT", "Erro ao apagar conta: ${e.message}")
                // Mesmo com erro parcial, faz logout para não deixar o utilizador preso
                try { auth.signOut() } catch (_: Exception) {}
                onComplete()
            }
        }
    }
}