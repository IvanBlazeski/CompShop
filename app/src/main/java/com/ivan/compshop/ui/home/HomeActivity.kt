package com.ivan.compshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.databinding.ActivityHomeBinding
import com.ivan.compshop.model.CartItem
import com.ivan.compshop.model.Computer
import com.ivan.compshop.ui.cart.CartActivity
import com.ivan.compshop.ui.detail.DetailActivity
import com.ivan.compshop.ui.orders.OrdersActivity
import com.ivan.compshop.ui.profile.ProfileActivity
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: ComputerAdapter
    private val app by lazy { application as CompShopApplication }
    private var allComputers = listOf<Computer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupBottomNavigation()
        loadComputers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        adapter = ComputerAdapter(
            onItemClick = { computer ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("computer_id", computer.id)
                startActivity(intent)
            },
            onAddToCart = { computer ->
                addToCart(computer)
            }
        )
        val spanCount = if (resources.getBoolean(com.ivan.compshop.R.bool.isTablet)) 3 else 2
        binding.rvComputers.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvComputers.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterComputers(s.toString())
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.ivan.compshop.R.id.nav_home -> true
                com.ivan.compshop.R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                com.ivan.compshop.R.id.nav_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    true
                }
                com.ivan.compshop.R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = com.ivan.compshop.R.id.nav_home
    }

    private fun loadComputers() {
        lifecycleScope.launch {
            try {
                allComputers = app.computerRepository.getAllComputers()
                adapter.submitList(allComputers)
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Грешка при вчитување", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterComputers(query: String) {
        val filtered = if (query.isEmpty()) {
            allComputers
        } else {
            allComputers.filter {
                it.brand.contains(query, ignoreCase = true) ||
                        it.model.contains(query, ignoreCase = true) ||
                        it.processor.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
    }

    private fun addToCart(computer: Computer) {
        lifecycleScope.launch {
            val cartItem = CartItem(
                computerId = computer.id,
                computerName = computer.model,
                computerBrand = computer.brand,
                price = computer.price,
                quantity = 1,
                imageUrl = computer.imageUrl
            )
            app.cartRepository.addToCart(cartItem)
            Toast.makeText(this@HomeActivity,
                getString(com.ivan.compshop.R.string.added_to_cart),
                Toast.LENGTH_SHORT).show()
        }
    }
}