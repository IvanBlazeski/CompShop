package com.ivan.compshop.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val computerId: String,
    val computerName: String,
    val computerBrand: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String
)