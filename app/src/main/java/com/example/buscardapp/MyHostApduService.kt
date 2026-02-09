package com.example.buscardapp

import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class MyHostApduService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val sharedPrefs = getSharedPreferences("card_prefs", Context.MODE_PRIVATE)

        val userId = sharedPrefs.getString("user_id", "0") ?: "0"
        val type = sharedPrefs.getString("card_type", "N/A") ?: "N/A"
        val saldo = sharedPrefs.getFloat("saldo", 0f)

        // Dados enviados: ID|TIPO|SALDO
        val data = "$userId|$type|$saldo"
        val dataBytes = data.toByteArray(Charsets.UTF_8)

        Log.d("HCE", "Enviando dados: $data")
        return dataBytes + byteArrayOf(0x90.toByte(), 0x00.toByte())
    }

    override fun onDeactivated(reason: Int) {
        Log.d("HCE", "Ligação NFC perdida: $reason")
    }
}