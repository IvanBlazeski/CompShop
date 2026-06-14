package com.ivan.compshop.ui.orders

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.databinding.ActivityOrdersBinding

class OrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersBinding
    private lateinit var adapter: OrdersAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        loadOrders()
    }

    private fun setupRecyclerView() {
        adapter = OrdersAdapter()
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.let {
            when {
                !it.email.isNullOrEmpty() -> it.email!!
                !it.displayName.isNullOrEmpty() -> it.displayName!!.replace(" ", "_") + "_" + it.uid.take(6)
                else -> it.uid
            }
        } ?: return

        android.util.Log.d("ORDERS", "Loading for: $userId")

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        // Директно читај само денешните нарачки
        firestore.collection("orders")
            .document(today)
            .collection(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.d("ORDERS", "Found: ${snapshot.size()}")

                val orders = snapshot.documents.mapNotNull { doc ->
                    val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val itemsText = itemsList.joinToString(", ") { item ->
                        "${item["computerBrand"]} ${item["computerName"]} x${item["quantity"]}"
                    }
                    OrderItem(
                        id = doc.id,
                        date = today,
                        items = itemsText,
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                        paymentMethod = doc.getString("paymentMethod") ?: "",
                        deliveryMethod = doc.getString("deliveryMethod") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                }

                if (orders.isEmpty()) showEmpty()
                else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvOrders.visibility = View.VISIBLE
                    adapter.submitList(orders)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ORDERS", "Error: ${e.message}")
                showEmpty()
            }
    }

    private fun loadOrdersForUser(userId: String) {
        android.util.Log.d("ORDERS", "Direct load for: $userId")
        showEmpty()
    }

    private fun showEmpty() {
        binding.tvEmpty.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
    }
}