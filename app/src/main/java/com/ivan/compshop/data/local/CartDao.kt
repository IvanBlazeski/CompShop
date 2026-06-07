package com.ivan.compshop.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items")
    fun getAllItems(): Flow<List<CartItemEntity>>
    @Query("SELECT * FROM cart_items")
    suspend fun getAllItemsList(): List<CartItemEntity>

    @Query("SELECT SUM(price * quantity) FROM cart_items")
    suspend fun getTotalPriceOnce(): Double?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CartItemEntity)

    @Delete
    suspend fun deleteItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCartCount(): Flow<Int>

    @Query("SELECT SUM(price * quantity) FROM cart_items")
    fun getTotalPrice(): Flow<Double>
}