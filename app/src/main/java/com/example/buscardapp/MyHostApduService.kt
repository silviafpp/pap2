package com.example.buscardapp

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking

class MyHCEService : HostApduService() {

    companion object {
        // AID configurado no apduservice.xml — tem de coincidir com o que o leitor envia
        private const val SELECT_AID_COMMAND = "00A4040007F001020304050600"

        // Resposta de sucesso NFC (SW1=90, SW2=00)
        private val SW_OK    = byteArrayOf(0x90.toByte(), 0x00.toByte())
        // Resposta de erro genérico
        private val SW_ERROR = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        // Resposta de cartão inválido/sem saldo
        private val SW_DENIED = byteArrayOf(0x69.toByte(), 0x85.toByte())
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val command = commandApdu?.joinToString("") { b: Byte -> "%02X".format(b) } ?: ""

        return if (command == SELECT_AID_COMMAND) {
            // Leitor identificou a app — responde com os dados do cartão
            buildCardResponse()
        } else {
            SW_ERROR
        }
    }

    private fun buildCardResponse(): ByteArray {
        return try {
            // Lê o cartão do utilizador atual de forma síncrona
            val user = SupabaseClient.supabase.auth.currentUserOrNull()
                ?: return "ERRO:SEM_SESSAO".toByteArray() + SW_DENIED

            val card = runBlocking {
                SupabaseClient.supabase.postgrest["user_cards"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeSingleOrNull<UserCard>()
            } ?: return "ERRO:SEM_CARTAO".toByteArray() + SW_DENIED

            // Formato da resposta: TIPO:VALOR
            // O leitor NFC interpreta esta string para validar a viagem
            val resposta = when (card.cardType) {
                "DIARIO"  -> if (card.saldo > 0)       "DIARIO:${String.format("%.2f", card.saldo)}"   else null
                "SEMANAL" -> if ((card.tripsLeft) > 0)  "SEMANAL:${card.tripsLeft}"                     else null
                "MENSAL"  -> if (card.isActive)         "MENSAL:ATIVO"                                  else null
                "FISICO"  -> if (card.hasPhysicalCard == true) "FISICO:${card.physicalCardUid ?: ""}"   else null
                else      -> null
            }

            if (resposta != null) {
                resposta.toByteArray(Charsets.UTF_8) + SW_OK
            } else {
                "ERRO:CARTAO_INVALIDO".toByteArray() + SW_DENIED
            }

        } catch (e: Exception) {
            "ERRO:${e.message?.take(20)}".toByteArray() + SW_ERROR
        }
    }

    override fun onDeactivated(reason: Int) {
        // Chamado quando a ligação NFC termina
        // reason: DEACTIVATION_LINK_LOSS ou DEACTIVATION_DESELECTED
    }
}