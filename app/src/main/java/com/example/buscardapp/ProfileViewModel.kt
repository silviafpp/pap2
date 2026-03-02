package com.example.buscardapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val userCard: UserCard? = null,
    val userEmail: String = "",
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _deleteState = MutableStateFlow<String?>(null)
    val deleteState: StateFlow<String?> = _deleteState

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = SupabaseClient.supabase.auth.currentUserOrNull()
                if (user != null) {
                    val email = user.email ?: ""
                    val uid   = user.id

                    val profile = SupabaseClient.supabase.postgrest["profiles"]
                        .select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<UserProfile>()

                    val card = SupabaseClient.supabase.postgrest["user_cards"]
                        .select { filter { eq("user_id", uid) } }
                        .decodeSingleOrNull<UserCard>()

                    _uiState.value = ProfileUiState(
                        isLoading   = false,
                        userProfile = profile,
                        userCard    = card,
                        userEmail   = email
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateProfile(firstName: String, lastName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val uid = SupabaseClient.supabase.auth.currentUserOrNull()?.id ?: return@launch
                SupabaseClient.supabase.postgrest["profiles"].update(
                    { set("first_name", firstName); set("last_name", lastName) }
                ) { filter { eq("id", uid) } }

                _uiState.value = _uiState.value.copy(
                    userProfile = _uiState.value.userProfile?.copy(
                        firstName = firstName,
                        lastName  = lastName
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erro ao guardar: ${e.message}")
            }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _deleteState.value = "A eliminar conta..."
            try {
                val uid = SupabaseClient.supabase.auth.currentUserOrNull()?.id ?: return@launch
                // Apaga dados do perfil e cartão
                SupabaseClient.supabase.postgrest["user_cards"]
                    .delete { filter { eq("user_id", uid) } }
                SupabaseClient.supabase.postgrest["profiles"]
                    .delete { filter { eq("id", uid) } }
                // Apaga o utilizador de autenticação
                SupabaseClient.supabase.auth.signOut()
                _deleteState.value = "Conta eliminada."
                onDeleted()
            } catch (e: Exception) {
                _deleteState.value = "Erro: ${e.message}"
            }
        }
    }
}