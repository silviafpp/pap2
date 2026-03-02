package com.example.buscardapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("has_card") val hasCard: Boolean = false,
    @SerialName("card_type") val cardType: String? = null
)