package com.example.buscardapp

import kotlinx.serialization.Serializable

@Serializable
data class UserCard(
    val user_id: String,
    val card_type: String,
    val trips_left: Int = 0,
    val saldo: Double = 0.0,
    val total_trips_month: Int = 0,      // Total de viagens no mês
    val total_duration_month: Int = 0    // Duração total em minutos
)