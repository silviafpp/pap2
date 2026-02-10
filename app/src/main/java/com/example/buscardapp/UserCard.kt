package com.example.buscardapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserCard(
    @SerialName("user_id") val userId: String,
    val saldo: Double = 0.0,
    @SerialName("trips_left") val tripsLeft: Int = 0,
    @SerialName("card_type") val type: String = "Normal" // Adiciona este campo
)