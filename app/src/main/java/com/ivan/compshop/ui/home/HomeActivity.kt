package com.ivan.compshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.R
import com.ivan.compshop.databinding.ActivityHomeBinding
import com.ivan.compshop.model.CartItem
import com.ivan.compshop.model.Computer
import com.ivan.compshop.ui.cart.CartActivity
import com.ivan.compshop.ui.detail.DetailActivity
import com.ivan.compshop.ui.orders.OrdersActivity
import com.ivan.compshop.ui.profile.ProfileActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: ComputerAdapter
    private val app by lazy { application as CompShopApplication }
    private var allComputers = listOf<Computer>()
    private var selectedBrand = "All"
    private var selectedProcessors = mutableSetOf("All")
    private var selectedPriceRanges = mutableSetOf("All")
    private var selectedSorts = mutableSetOf("Default")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupChips()
        setupBottomNavigation()
        setupCartHeader()
        setupFilterButton()
        loadComputers()
        observeCartCount()
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
        val spanCount = if (resources.getBoolean(R.bool.isTablet)) 3 else 2
        binding.rvComputers.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvComputers.adapter = adapter
    }

    private fun setupSearch() {
        val searchField = findViewById<android.widget.EditText>(R.id.etSearch)
        searchField?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupChips() {
        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipDell = findViewById<TextView>(R.id.chipDell)
        val chipHp = findViewById<TextView>(R.id.chipHp)
        val chipLenovo = findViewById<TextView>(R.id.chipLenovo)
        val chipApple = findViewById<TextView>(R.id.chipApple)

        val allChips = listOf(chipAll, chipDell, chipHp, chipLenovo, chipApple)
        val brands = listOf("All", "Dell", "HP", "Lenovo", "Apple")

        allChips.forEachIndexed { index, chip ->
            chip?.setOnClickListener {
                selectedBrand = brands[index]
                allChips.forEach { c ->
                    c?.setBackgroundResource(R.drawable.btn_social_neon)
                    c?.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
                }
                chip.setBackgroundResource(R.drawable.btn_neon_gradient)
                chip.setTextColor(android.graphics.Color.WHITE)
                val query = findViewById<android.widget.EditText>(R.id.etSearch)?.text.toString() ?: ""
                applyFilters(query)
            }
        }
    }

    private fun setupCartHeader() {
        findViewById<android.widget.FrameLayout>(R.id.btnCartHeader)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

    private fun setupFilterButton() {
        findViewById<android.widget.LinearLayout>(R.id.btnFilter)?.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = FilterBottomSheetFragment(
            selectedProcessors = selectedProcessors,
            selectedPriceRanges = selectedPriceRanges,
            selectedSorts = selectedSorts,
            onApply = { processors, priceRanges, sorts ->
                selectedProcessors = processors.toMutableSet()
                selectedPriceRanges = priceRanges.toMutableSet()
                selectedSorts = sorts.toMutableSet()
                val query = findViewById<android.widget.EditText>(R.id.etSearch)?.text.toString() ?: ""
                applyFilters(query)
            }
        )
        bottomSheet.show(supportFragmentManager, "FilterBottomSheet")
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun observeCartCount() {
        lifecycleScope.launch {
            app.cartRepository.getCartCount().collectLatest { count ->
                val badge = findViewById<TextView>(R.id.tvCartBadge)
                if (count > 0) {
                    badge?.visibility = android.view.View.VISIBLE
                    badge?.text = count.toString()
                } else {
                    badge?.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun loadComputers() {
        lifecycleScope.launch {
            try {
                allComputers = app.computerRepository.getAllComputers()
                applyFilters("")
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Грешка при вчитување", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters(query: String) {
        var filtered = allComputers

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.brand.contains(query, ignoreCase = true) ||
                        it.model.contains(query, ignoreCase = true) ||
                        it.processor.contains(query, ignoreCase = true)
            }
        }

        if (selectedBrand != "All") {
            filtered = filtered.filter {
                it.brand.contains(selectedBrand, ignoreCase = true)
            }
        }

        if (!selectedProcessors.contains("All")) {
            filtered = filtered.filter { computer ->
                selectedProcessors.any { proc ->
                    computer.processor.contains(proc, ignoreCase = true)
                }
            }
        }

        val priceFiltered = mutableListOf<Computer>()
        if (selectedPriceRanges.contains("All")) {
            priceFiltered.addAll(filtered)
        } else {
            if (selectedPriceRanges.contains("Under $1000"))
                priceFiltered.addAll(filtered.filter { it.price < 1000 })
            if (selectedPriceRanges.contains("$1000 - $1500"))
                priceFiltered.addAll(filtered.filter { it.price in 1000.0..1500.0 })
            if (selectedPriceRanges.contains("Over $1500"))
                priceFiltered.addAll(filtered.filter { it.price > 1500 })
        }
        filtered = priceFiltered.distinctBy { it.id }

        filtered = when (selectedSorts.firstOrNull()) {
            "Lowest price" -> filtered.sortedBy { it.price }
            "Highest price" -> filtered.sortedByDescending { it.price }
            else -> filtered
        }

        val emptyView = findViewById<TextView>(R.id.tvEmpty)
        if (filtered.isEmpty()) {
            emptyView?.visibility = android.view.View.VISIBLE
            binding.rvComputers.visibility = android.view.View.GONE
        } else {
            emptyView?.visibility = android.view.View.GONE
            binding.rvComputers.visibility = android.view.View.VISIBLE
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
            Toast.makeText(
                this@HomeActivity,
                getString(R.string.added_to_cart),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}