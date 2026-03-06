package com.example.registration_form

data class Donation(
    val id: String = "",
    val foodType: String = "",
    val description: String = "",
    val quantity: Int = 0,
    val status: String = "PENDING",
    val date: Long = System.currentTimeMillis(),
    val restaurantName: String? = null,
    val createdBy: String? = null,
    val acceptedBy: String? = null,
    val deliveryScheduled: Boolean = false,
    val deliveryDate: Long? = null
)

