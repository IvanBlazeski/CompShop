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

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        val dates = (0..30).map {
            val date = sdf.format(calendar.time)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            date
        }

        val allOrders = mutableListOf<OrderItem>()
        var completed = 0

        dates.forEach { date ->
            firestore.collection("orders")
                .document(date)
                .collection(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        completed++
                        if (completed == dates.size) updateUI(allOrders)
                        return@addSnapshotListener
                    }

                    val orders = snapshot.documents.mapNotNull { doc ->
                        val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                        val itemsText = itemsList.joinToString(", ") { item ->
                            "${item["computerBrand"]} ${item["computerName"]} x${item["quantity"]}"
                        }
                        OrderItem(
                            id = doc.id,
                            date = date,
                            items = itemsText,
                            totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            deliveryMethod = doc.getString("deliveryMethod") ?: "",
                            status = doc.getString("status") ?: "pending",
                            trackingStatus = doc.getString("trackingStatus")?.lowercase() ?: "placed"
                        )
                    }

                    // Отстрани стари нарачки од овој датум и додај нови
                    allOrders.removeAll { it.date == date }
                    allOrders.addAll(orders)
                    allOrders.sortByDescending { it.date }
                    updateUI(allOrders)
                }
        }
    }

    private fun updateUI(orders: List<OrderItem>) {
        if (orders.isEmpty()) showEmpty()
        else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvOrders.visibility = View.VISIBLE
            adapter.submitList(orders.toList())
        }
    }

    private fun showEmpty() {
        binding.tvEmpty.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
    }
}