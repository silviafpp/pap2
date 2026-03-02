package com.example.buscardapp

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.auth

/* ---------------- SUPABASE CLIENT ---------------- */

object SupabaseClient {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://upnpstfyaewmvllymhes.supabase.co",
        supabaseKey = "sb_publishable_S7MdRy3B-UhFt2Wc1nYPDg_MlS_Ylc7"
    ) {
        install(Postgrest)
        install(Auth) {
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
        }
    }
}