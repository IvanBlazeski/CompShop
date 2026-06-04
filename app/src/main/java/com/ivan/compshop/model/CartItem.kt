package com.ivan.compshop.model

data class CartItem(
    val id: String = "",
    val computerId: String = "",
    val computerName: String = "",
    val computerBrand: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
)