package com.ivan.compshop.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.databinding.ActivityOrdersBinding
import com.ivan.compshop.databinding.ItemOrderBinding
import com.ivan.compshop.model.Order
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadOrders()
    }

    private fun setupRecyclerView() {
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("orders")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val orders = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }

                if (orders.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvOrders.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvOrders.visibility = View.VISIBLE
                    binding.rvOrders.adapter = OrdersAdapter(orders)
                }

            } catch (e: Exception) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvOrders.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

class OrdersAdapter(
    private val orders: List<Order>
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvOrderId.text = order.id.take(8)
            binding.tvTotal.text = "$${order.totalPrice}"
            binding.tvStatus.text = order.status

            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(order.date))
        }
    }
}