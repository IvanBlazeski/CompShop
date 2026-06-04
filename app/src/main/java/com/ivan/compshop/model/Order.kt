package com.ivan.compshop.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val computers: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val status: String = "pending",
    val date: Long = System.currentTimeMillis(),
    val address: String = ""
)