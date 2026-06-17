package com.ivan.compshop.model

data class Computer(
    val id: String = "",
    val brand: String = "",
    val model: String = "",
    val processor: String = "",
    val ram: String = "",
    val storage: String = "",
    val graphics: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val inStock: Boolean = true,
    val description: String = "",
    val quantity: Int = 0
)