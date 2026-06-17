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
    private val previousStatuses = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Permission request овде!
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
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

        dates.forEach { date ->
            firestore.collection("orders")
                .document(date)
                .collection(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

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

                    // Tracking нотификација
                    orders.forEach { order ->
                        val prev = previousStatuses[order.id]
                        if (prev != null && prev != order.trackingStatus) {
                            showTrackingNotification(order)
                        }
                        previousStatuses[order.id] = order.trackingStatus
                    }

                    allOrders.removeAll { it.date == date }
                    allOrders.addAll(orders)
                    allOrders.sortByDescending { it.date }
                    updateUI(allOrders)
                }
        }
    }

    private fun showTrackingNotification(order: OrderItem) {
        val statusText = when (order.trackingStatus) {
            "process" -> "📦 Нарачката е во обработка"
            "shipped" -> "🚚 Нарачката е испратена"
            "delivery" -> "🏠 Нарачката е во достава"
            "done" -> "✅ Нарачката е доставена!"
            else -> return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, "tracking_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📦 Order Update")
            .setContentText(statusText)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(order.id.hashCode(), notification)
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