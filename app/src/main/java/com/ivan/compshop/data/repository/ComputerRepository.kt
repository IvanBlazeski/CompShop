package com.ivan.compshop.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.model.Computer
import kotlinx.coroutines.tasks.await

class ComputerRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val computersCollection = firestore.collection("computers")

    suspend fun getAllComputers(): List<Computer> {
        return try {
            val snapshot = computersCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val qty = (doc.getLong("quantity") ?: 0).toInt()
                Computer(
                    id = doc.id,
                    brand = doc.getString("brand") ?: "",
                    model = doc.getString("model") ?: "",
                    processor = doc.getString("processor") ?: "",
                    ram = doc.getString("ram") ?: "",
                    storage = doc.getString("storage") ?: "",
                    graphics = doc.getString("graphics") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    description = doc.getString("description") ?: "",
                    quantity = qty,
                    inStock = qty > 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getComputerById(id: String): Computer? {
        return try {
            val doc = computersCollection.document(id).get().await()
            val qty = (doc.getLong("quantity") ?: 0).toInt()
            Computer(
                id = doc.id,
                brand = doc.getString("brand") ?: "",
                model = doc.getString("model") ?: "",
                processor = doc.getString("processor") ?: "",
                ram = doc.getString("ram") ?: "",
                storage = doc.getString("storage") ?: "",
                graphics = doc.getString("graphics") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                imageUrl = doc.getString("imageUrl") ?: "",
                description = doc.getString("description") ?: "",
                quantity = qty,
                inStock = qty > 0
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchComputers(query: String): List<Computer> {
        return try {
            val snapshot = computersCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Computer::class.java)?.copy(id = doc.id)
            }.filter { computer ->
                computer.brand.contains(query, ignoreCase = true) ||
                        computer.model.contains(query, ignoreCase = true) ||
                        computer.processor.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}