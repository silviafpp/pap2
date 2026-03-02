package com.example.buscardapp

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buscardapp.ui.theme.BusCardAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {

    private var physicalCardUid by mutableStateOf<String?>(null)
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            // ── Modo Escuro persistido via DataStore ──────────────────────────
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val scope      = rememberCoroutineScope()

            // Valor inicial: lê do DataStore antes de mostrar UI
            var isDarkMode by remember { mutableStateOf(false) }
            var themeLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isDarkMode  = themePrefs.isDarkMode.first()
                themeLoaded = true
            }

            // Só renderiza quando o tema estiver carregado (evita flash)
            if (!themeLoaded) return@setContent

            var showNfcSheet by remember { mutableStateOf(false) }

            BusCardAppTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = viewModel()

                Box {
                    NavGraph(
                        authViewModel          = authViewModel,
                        isDarkMode             = isDarkMode,
                        onThemeToggle          = { newValue ->
                            isDarkMode = newValue
                            scope.launch { themePrefs.setDarkMode(newValue) }
                        },
                        onCardClick            = { showNfcSheet = true },
                        physicalCardUid        = physicalCardUid,
                        onPhysicalCardConsumed = { physicalCardUid = null }
                    )

                    if (showNfcSheet) {
                        NfcPaymentOverlay(onDismiss = { showNfcSheet = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            val intent  = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            adapter.enableForegroundDispatch(this, pending, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                physicalCardUid = it.id.joinToString("") { byte -> "%02X".format(byte) }
            }
        }
    }
}