package com.example.buscardapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

class UserViewModel : ViewModel() {

    private val auth      = SupabaseClient.supabase.auth
    private val postgrest = SupabaseClient.supabase.postgrest

    private val _userEmail = MutableStateFlow(auth.currentUserOrNull()?.email ?: "")
    val userEmail: StateFlow<String> = _userEmail

    private val _cardState = MutableStateFlow<CardState>(CardState.Idle)
    val cardState: StateFlow<CardState> = _cardState

    // ── Criar Cartão Digital ───────────────────────────────────────────────────
    fun criarCartao(tipoPasse: String, onSucesso: () -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            try {
                val hoje          = LocalDate.now().toString()
                val expirySemanal = LocalDate.now().plusDays(7).toString()

                postgrest["user_cards"].delete { filter { eq("user_id", user.id) } }

                when (tipoPasse) {
                    "MENSAL" -> postgrest["user_cards"].insert(
                        NovoCartaoMensal(userId = user.id, cardType = tipoPasse, isActive = true, saldo = 0.0, tripsLeft = 0, totalTrips = 0, renewalDate = hoje)
                    )
                    "SEMANAL" -> postgrest["user_cards"].insert(
                        NovoCartaoSemanal(userId = user.id, cardType = tipoPasse, isActive = true, saldo = 0.0, tripsLeft = 10, totalTrips = 0, expiryDate = expirySemanal)
                    )
                    "DIARIO" -> postgrest["user_cards"].insert(
                        NovoCartaoDiario(userId = user.id, cardType = tipoPasse, isActive = true, saldo = 0.0, tripsLeft = 0, totalTrips = 0)
                    )
                    else -> { _cardState.value = CardState.Error("Tipo de passe inválido."); return@launch }
                }

                postgrest["profiles"].update({
                    set("has_card",  true)
                    set("card_type", tipoPasse)
                }) { filter { eq("id", user.id) } }

                _cardState.value = CardState.Success("Cartão $tipoPasse criado com sucesso!")
                onSucesso()

            } catch (e: Exception) {
                e.printStackTrace()
                _cardState.value = CardState.Error("Erro: ${e.localizedMessage}")
            }
        }
    }

    // ── Registar Cartão Físico (guarda nome do titular) ───────────────────────
    fun registarCartaoFisico(uid: String, cardHolderName: String = "", onSucesso: () -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            try {
                val cartaoExistente = postgrest["user_cards"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeSingleOrNull<UserCard>()

                if (cartaoExistente != null) {
                    // Já tem cartão digital — associa o UID físico e guarda o nome
                    postgrest["user_cards"].update({
                        set("physical_card_uid",  uid)
                        set("has_physical_card",  true)
                        set("is_active",          true)
                        set("card_holder_name",   cardHolderName)
                    }) { filter { eq("user_id", user.id) } }
                } else {
                    // Não tem cartão — cria novo com cartão físico
                    postgrest["user_cards"].insert(
                        NovoCartaoFisico(
                            userId          = user.id,
                            cardType        = "FISICO",
                            isActive        = true,
                            saldo           = 0.0,
                            tripsLeft       = 0,
                            totalTrips      = 0,
                            physicalCardUid = uid,
                            hasPhysicalCard = true,
                            cardHolderName  = cardHolderName
                        )
                    )
                    postgrest["profiles"].update({
                        set("has_card",  true)
                        set("card_type", "FISICO")
                    }) { filter { eq("id", user.id) } }
                }

                _cardState.value = CardState.Success("Cartão físico associado com sucesso!")
                onSucesso()

            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("NFC_ERRO", "Erro: ${e.message}")
                _cardState.value = CardState.Error("Erro ao associar cartão: ${e.localizedMessage}")
            }
        }
    }

    // ── Eliminar Cartão (apaga da BD e reseta o perfil) ───────────────────────
    fun eliminarCartao(onSucesso: () -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            try {
                // 1. Apaga o cartão da tabela user_cards
                postgrest["user_cards"].delete {
                    filter { eq("user_id", user.id) }
                }

                // 2. Reseta o perfil: sem cartão, sem tipo
                postgrest["profiles"].update({
                    set("has_card",  false)
                    set("card_type", null as String?)
                }) { filter { eq("id", user.id) } }

                _cardState.value = CardState.Success("Cartão eliminado com sucesso!")
                onSucesso()

            } catch (e: Exception) {
                e.printStackTrace()
                _cardState.value = CardState.Error("Erro ao eliminar cartão: ${e.localizedMessage}")
            }
        }
    }

    // ── Carregar Saldo (Diário) ────────────────────────────────────────────────
    fun carregarSaldo(valor: Double, onSucesso: () -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            try {
                val cartaoAtual = postgrest["user_cards"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeSingleOrNull<UserCard>()

                val novoSaldo = (cartaoAtual?.saldo ?: 0.0) + valor
                postgrest["user_cards"].update({
                    set("saldo",     novoSaldo)
                    set("is_active", true)
                }) { filter { eq("user_id", user.id) } }

                _cardState.value = CardState.Success("Saldo carregado: +€${String.format("%.2f", valor)}")
                onSucesso()

            } catch (e: Exception) {
                _cardState.value = CardState.Error("Erro ao carregar saldo: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    // ── Renovar Passe Mensal ───────────────────────────────────────────────────
    fun renovarPasseMensal(onSucesso: () -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            try {
                postgrest["user_cards"].update({
                    set("renewal_date", LocalDate.now().toString())
                    set("is_active",    true)
                }) { filter { eq("user_id", user.id) } }
                _cardState.value = CardState.Success("Passe renovado!")
                onSucesso()
            } catch (e: Exception) {
                _cardState.value = CardState.Error("Erro ao renovar passe: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    // ── Validar Cartão ─────────────────────────────────────────────────────────
    fun isCardValid(card: UserCard?, cardType: String?): Boolean {
        if (card == null || !card.isActive) return false
        return when (cardType) {
            "MENSAL" -> {
                val renewalDate = card.renewalDate?.let { LocalDate.parse(it) } ?: return false
                renewalDate.month == LocalDate.now().month && renewalDate.year == LocalDate.now().year
            }
            "SEMANAL" -> {
                val expiry = card.expiryDate?.let { LocalDate.parse(it) } ?: return false
                card.tripsLeft > 0 && LocalDate.now().isBefore(expiry.plusDays(1))
            }
            "DIARIO" -> card.saldo > 0.0
            "FISICO" -> card.hasPhysicalCard == true
            else     -> false
        }
    }

    // ── Registar Viagem (NFC) ──────────────────────────────────────────────────
    fun registarViagem(routeId: Int, farePaid: Double, cardType: String?, onSucesso: () -> Unit, onErro: (String) -> Unit) {
        val user = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                val cartao = postgrest["user_cards"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeSingleOrNull<UserCard>()

                if (!isCardValid(cartao, cardType)) {
                    onErro(when (cardType) {
                        "MENSAL"  -> "Passe mensal expirado. Por favor renove."
                        "SEMANAL" -> if ((cartao?.tripsLeft ?: 0) == 0) "Sem viagens disponíveis." else "Passe semanal expirado."
                        "DIARIO"  -> "Saldo insuficiente. Por favor carregue o cartão."
                        "FISICO"  -> "Cartão físico não reconhecido."
                        else      -> "Cartão inválido."
                    })
                    return@launch
                }

                when (cardType) {
                    "SEMANAL" -> postgrest["user_cards"].update({ set("trips_left", cartao!!.tripsLeft - 1); set("total_trips", cartao.totalTrips + 1) }) { filter { eq("user_id", user.id) } }
                    "DIARIO"  -> postgrest["user_cards"].update({ set("saldo", cartao!!.saldo - farePaid); set("total_trips", cartao.totalTrips + 1) }) { filter { eq("user_id", user.id) } }
                    "MENSAL", "FISICO" -> postgrest["user_cards"].update({ set("total_trips", (cartao?.totalTrips ?: 0) + 1) }) { filter { eq("user_id", user.id) } }
                }

                postgrest["trip_history"].insert(NovaViagem(userId = user.id, routeId = routeId, farePaid = farePaid, tripDate = java.time.Instant.now().toString()))
                onSucesso()

            } catch (e: Exception) {
                onErro("Erro ao registar viagem: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    fun clearState() { _cardState.value = CardState.Idle }
}

// ── Estados ────────────────────────────────────────────────────────────────────
sealed class CardState {
    object Idle    : CardState()
    object Loading : CardState()
    data class Success(val message: String) : CardState()
    data class Error(val message: String)   : CardState()
}

// ── Data Classes para INSERT ───────────────────────────────────────────────────
@Serializable
data class NovoCartaoMensal(
    @SerialName("user_id")      val userId: String,
    @SerialName("card_type")    val cardType: String,
    @SerialName("is_active")    val isActive: Boolean,
    val saldo: Double,
    @SerialName("trips_left")   val tripsLeft: Int,
    @SerialName("total_trips")  val totalTrips: Int,
    @SerialName("renewal_date") val renewalDate: String
)

@Serializable
data class NovoCartaoSemanal(
    @SerialName("user_id")     val userId: String,
    @SerialName("card_type")   val cardType: String,
    @SerialName("is_active")   val isActive: Boolean,
    val saldo: Double,
    @SerialName("trips_left")  val tripsLeft: Int,
    @SerialName("total_trips") val totalTrips: Int,
    @SerialName("expiry_date") val expiryDate: String
)

@Serializable
data class NovoCartaoDiario(
    @SerialName("user_id")     val userId: String,
    @SerialName("card_type")   val cardType: String,
    @SerialName("is_active")   val isActive: Boolean,
    val saldo: Double,
    @SerialName("trips_left")  val tripsLeft: Int,
    @SerialName("total_trips") val totalTrips: Int
)

@Serializable
data class NovoCartaoFisico(
    @SerialName("user_id")           val userId: String,
    @SerialName("card_type")         val cardType: String,
    @SerialName("is_active")         val isActive: Boolean,
    val saldo: Double,
    @SerialName("trips_left")        val tripsLeft: Int,
    @SerialName("total_trips")       val totalTrips: Int,
    @SerialName("physical_card_uid") val physicalCardUid: String,
    @SerialName("has_physical_card") val hasPhysicalCard: Boolean,
    @SerialName("card_holder_name")  val cardHolderName: String = ""
)

@Serializable
data class NovaViagem(
    @SerialName("user_id")   val userId: String,
    @SerialName("route_id")  val routeId: Int,
    @SerialName("fare_paid") val farePaid: Double,
    @SerialName("trip_date") val tripDate: String
)