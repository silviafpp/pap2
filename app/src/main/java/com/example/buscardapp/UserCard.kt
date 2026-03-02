package com.example.buscardapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserCard(
    val id: String? = null,
    @SerialName("user_id")           val userId: String? = null,
    @SerialName("card_type")         val cardType: String? = null,
    @SerialName("is_active")         val isActive: Boolean = false,
    @SerialName("created_at")        val createdAt: String? = null,
    val saldo: Double = 0.0,
    @SerialName("trips_left")        val tripsLeft: Int = 0,
    @SerialName("total_trips")       val totalTrips: Int = 0,
    @SerialName("last_renewal_date") val lastRenewalDate: String? = null,
    @SerialName("expiry_date")       val expiryDate: String? = null,
    @SerialName("renewal_date")      val renewalDate: String? = null,
    @SerialName("physical_card_uid") val physicalCardUid: String? = null,
    @SerialName("has_physical_card") val hasPhysicalCard: Boolean? = false
)