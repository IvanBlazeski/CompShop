package com.ivan.compshop.ui.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.databinding.ActivityCartBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val app by lazy { application as CompShopApplication }
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        observeCart()
        setupCheckout()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter { item ->
            lifecycleScope.launch {
                app.cartRepository.removeFromCart(item)
                Toast.makeText(this@CartActivity, "Removed!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter
    }

    private fun observeCart() {
        lifecycleScope.launch {
            app.cartRepository.getAllItems().collectLatest { items ->
                adapter.submitList(items)
                if (items.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvCart.visibility = View.GONE
                    binding.layoutCheckout.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvCart.visibility = View.VISIBLE
                    binding.layoutCheckout.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            app.cartRepository.getTotalPrice().collectLatest { total ->
                binding.tvTotal.text = "$${"%.2f".format(total ?: 0.0)}"
            }
        }
    }

    private fun setupCheckout() {
        binding.btnCheckout.setOnClickListener {
            lifecycleScope.launch {
                val items = app.cartRepository.getAllItemsList()
                val total = app.cartRepository.getTotalPriceOnce()

                val checkoutDialog = CheckoutDialog(total) { paymentMethod, deliveryMethod, finalTotal ->
                    placeOrder(items, paymentMethod, deliveryMethod, finalTotal)
                }
                checkoutDialog.show(supportFragmentManager, "CheckoutDialog")
            }
        }
    }

    private fun placeOrder(
        items: List<com.ivan.compshop.data.local.CartItemEntity>,
        paymentMethod: String,
        deliveryMethod: String,
        finalTotal: Double
    ) {
        val userId = auth.currentUser?.let {
            when {
                !it.email.isNullOrEmpty() -> it.email!!
                !it.displayName.isNullOrEmpty() -> it.displayName!!.replace(" ", "_") + "_" + it.uid.take(6)
                else -> it.uid
            }
        } ?: return

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val status = if (paymentMethod == "Card") "paid" else "pending"

        val order = hashMapOf(
            "userId" to userId,
            "userEmail" to userId,
            "items" to items.map { item ->
                hashMapOf(
                    "computerId" to item.computerId,
                    "computerName" to item.computerName,
                    "computerBrand" to item.computerBrand,
                    "price" to item.price,
                    "quantity" to item.quantity
                )
            },
            "totalPrice" to finalTotal,
            "paymentMethod" to paymentMethod,
            "deliveryMethod" to deliveryMethod,
            "status" to status,
            "isPaid" to (paymentMethod == "Card"),
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("orders")
            .document(today)
            .collection(userId)
            .add(order)
            .addOnSuccessListener {
                // Нотификација
                val notifTitle = if (paymentMethod == "Card") "Payment confirmed! 💳✅" else "Order placed! 📦"
                val notifMessage = if (paymentMethod == "Card")
                    "Your payment of $${"%.2f".format(finalTotal)} was successful!"
                else
                    "Your order of $${"%.2f".format(finalTotal)} will be paid on delivery."

                firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .add(hashMapOf(
                        "title" to notifTitle,
                        "message" to notifMessage,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    ))

                Toast.makeText(
                    this@CartActivity,
                    getString(com.ivan.compshop.R.string.order_placed),
                    Toast.LENGTH_SHORT
                ).show()

                lifecycleScope.launch {
                    app.cartRepository.clearCart()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@CartActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}