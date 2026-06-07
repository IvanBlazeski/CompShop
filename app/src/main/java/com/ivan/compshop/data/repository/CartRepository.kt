package com.ivan.compshop.data.repository

import com.ivan.compshop.data.local.AppDatabase
import com.ivan.compshop.data.local.CartItemEntity
import com.ivan.compshop.model.CartItem
import kotlinx.coroutines.flow.Flow

class CartRepository(database: AppDatabase) {

    private val cartDao = database.cartDao()

    fun getAllItems(): Flow<List<CartItemEntity>> = cartDao.getAllItems()

    fun getCartCount(): Flow<Int> = cartDao.getCartCount()

    fun getTotalPrice(): Flow<Double> = cartDao.getTotalPrice()

    suspend fun addToCart(cartItem: CartItem) {
        val entity = CartItemEntity(
            computerId = cartItem.computerId,
            computerName = cartItem.computerName,
            computerBrand = cartItem.computerBrand,
            price = cartItem.price,
            quantity = cartItem.quantity,
            imageUrl = cartItem.imageUrl
        )
        cartDao.insertItem(entity)
    }
    suspend fun getAllItemsList(): List<CartItemEntity> {
        return cartDao.getAllItemsList()
    }

    suspend fun getTotalPriceOnce(): Double {
        return cartDao.getTotalPriceOnce() ?: 0.0
    }
    suspend fun removeFromCart(cartItem: CartItemEntity) {
        cartDao.deleteItem(cartItem)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }
}