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
            Toast.makeText(this, getString(com.ivan.compshop.R.string.order_placed), Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                app.cartRepository.clearCart()
            }
        }
    }
}