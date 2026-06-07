package com.ivan.compshop.ui.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.databinding.ActivityCartBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val app by lazy { application as CompShopApplication }

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
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.let {
                if (!it.email.isNullOrEmpty()) it.email else it.uid
            } ?: return@setOnClickListener

            lifecycleScope.launch {
                val items = app.cartRepository.getAllItemsList()
                val total = app.cartRepository.getTotalPriceOnce()

                val dialog = CheckoutDialog(total) { paymentMethod, deliveryMethod, finalTotal ->
                    lifecycleScope.launch {
                        // Зачувај нарачка
                        val order = hashMapOf(
                            "userId" to userId,
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
                            "status" to "pending",
                            "isPaid" to (paymentMethod == "Card"),
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("orders")
                            .document(userId)
                            .collection("userOrders")
                            .add(order)
                            .addOnSuccessListener { documentRef ->
                                android.util.Log.d("CHECKOUT", "Order saved: ${documentRef.id}")
                                // Нотификација
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .collection("notifications")
                                    .add(hashMapOf(
                                        "title" to "Order confirmed! 🎉",
                                        "message" to "Your order of $${"%.2f".format(finalTotal)} via $paymentMethod is confirmed!",
                                        "isRead" to false,
                                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
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
                                android.util.Log.e("CHECKOUT", "Error saving order: ${e.message}")
                                Toast.makeText(this@CartActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                dialog.show(supportFragmentManager, "CheckoutDialog")
            }
        }
    }
}