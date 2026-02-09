package com.example.buscardapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusRoute(
    val id: Int? = null,
    @SerialName("route_number") val route_number: String = "",
    @SerialName("origin") val origin: String = "",
    @SerialName("destination") val destination: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("duration") val duration: String = "45 min"
)