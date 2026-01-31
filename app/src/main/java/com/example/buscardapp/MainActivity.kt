package com.example.buscardapp

import android.R.attr.onClick
import android.content.ContentValues.TAG
import android.credentials.GetCredentialException
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.buscardapp.ui.theme.BusCardAppTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.ktor.websocket.WebSocketDeflateExtension.Companion.install
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.UUID

/* ---------------- SUPABASE CLIENT ---------------- */

val supabase = createSupabaseClient(
    supabaseUrl = "https://upnpstfyaewmvllymhes.supabase.co",
    supabaseKey = "sb_publishable_S7MdRy3B-UhFt2Wc1nYPDg_MlS_Ylc7"
) {
    install(Postgrest)
    install(Auth)
}

/* ---------------- MAIN ACTIVITY ---------------- */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            BusCardAppTheme {
                Column (
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InsertButton()
                    GoogleSignInButton()
                }
            }
        }
    }
}

@Serializable
data class User(
    @SerialName("id")
    val id: Int,

    @SerialName("first_name")
    val first_name: String,

    @SerialName("last_name")
    val last_name: String,

    @SerialName("email")
    val email: String
)
@Composable
fun InsertButton() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current


    Button(onClick = {
        coroutineScope.launch {
            try {
                supabase.from("users").insert(mapOf("content" to "Hello World"))

                Toast.makeText(context, "Row inserted", Toast.LENGTH_SHORT).show()
            }catch (e: RestException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }) {
        Text("Insert a new row")
    }
}

@Composable
fun GoogleSignInButton() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val onClick: () -> Unit = {
        val credentialManager = CredentialManager.create(context)

        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val nd = MessageDigest.getInstance("SHA-256")
        val digest = nd.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId("744647664470-8odukj93lh37a56vdvom0ha3qiefo8fr.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(false)
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                val credential = result.credential

                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)

                val googleIdToken = googleIdTokenCredential.idToken

                Log.i("TAG", googleIdToken)

                supabase.auth.signInWith(IDToken) {
                    idToken = googleIdToken
                    provider = Google
                    nonce = rawNonce

                }


                Toast.makeText(context, "Signed in with Google", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // O Botão deve ficar fora do bloco onClick
    Button(onClick = onClick) {
        Text("Sign in with Google")
    }
}


















