package com.ivan.compshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.R
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
    private val firestore = FirebaseFirestore.getInstance()

    private var allComputers = listOf<Computer>()
    private var selectedBrand = "All"
    private var selectedProcessors = mutableSetOf("All")
    private var selectedPriceRanges = mutableSetOf("All")
    private var selectedSorts = mutableSetOf("Default")
    private var selectedBrandChip: TextView? = null
    private var personalUnreadCount = 0
    private var globalUnreadCount = 0
    private var favoritedIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupBrandChips()
        setupBottomNavigation()
        setupCartHeader()
        setupFilterButton()
        loadComputers()
        loadFavorites()
        observeNotificationCount()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun setupRecyclerView() {
        adapter = ComputerAdapter(
            onItemClick = { computer ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("computer_id", computer.id)
                startActivity(intent)
            },
            onAddToCart = { computer -> addToCart(computer) },
            onFavoriteClick = { computer -> toggleFavorite(computer) }
        )
        val spanCount = if (resources.getBoolean(R.bool.isTablet)) 3 else 2
        binding.rvComputers.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvComputers.adapter = adapter
    }

    private fun setupSearch() {
        findViewById<android.widget.EditText>(R.id.etSearch)
            ?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    applyFilters(s.toString())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
    }

    private fun setupBrandChips() {
        val chipAll = binding.chipAll ?: return
        selectedBrandChip = chipAll

        chipAll.setOnClickListener {
            selectedBrand = "All"
            selectBrandChip(chipAll)
            applyFilters(getCurrentQuery())
        }

        firestore.collection("computers")
            .addSnapshotListener { snapshot, _ ->
                val chipGroup = binding.chipGroup ?: return@addSnapshotListener
                val brands = snapshot?.documents.orEmpty()
                    .mapNotNull { it.getString("brand") }
                    .distinct().sorted()

                if (chipGroup.childCount > 1)
                    chipGroup.removeViews(1, chipGroup.childCount - 1)

                brands.forEach { brand ->
                    val chip = TextView(this)
                    chip.text = brand
                    chip.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
                    chip.textSize = 13f
                    chip.gravity = android.view.Gravity.CENTER
                    chip.setBackgroundResource(R.drawable.btn_social_neon)
                    chip.isClickable = true
                    chip.isFocusable = true
                    val params = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(36)
                    )
                    params.marginEnd = dpToPx(8)
                    chip.layoutParams = params
                    chip.setPadding(dpToPx(16), 0, dpToPx(16), 0)
                    chip.setOnClickListener {
                        selectedBrand = brand
                        selectBrandChip(chip)
                        applyFilters(getCurrentQuery())
                    }
                    chipGroup.addView(chip)
                }
            }
    }

    private fun selectBrandChip(chip: TextView) {
        selectedBrandChip?.setBackgroundResource(R.drawable.btn_social_neon)
        selectedBrandChip?.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
        chip.setBackgroundResource(R.drawable.btn_neon_gradient)
        chip.setTextColor(android.graphics.Color.WHITE)
        selectedBrandChip = chip
    }

    private fun setupCartHeader() {
        findViewById<android.widget.FrameLayout>(R.id.btnNotification)?.setOnClickListener {
            com.ivan.compshop.ui.notifications.NotificationsBottomSheet()
                .show(supportFragmentManager, "NotificationsBottomSheet")
        }

        binding.chipAll?.setOnLongClickListener {
            showFavoritesDialog()
            true
        }
    }

    private fun showFavoritesDialog() {
        val favorites = allComputers.filter { favoritedIds.contains(it.id) }
        if (favorites.isEmpty()) {
            Toast.makeText(this, "No favorites yet ❤️", Toast.LENGTH_SHORT).show()
            return
        }
        val names = favorites.map { "❤️ ${it.brand} ${it.model} — $${it.price}" }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("❤️ Favorites")
            .setItems(names) { _, index ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("computer_id", favorites[index].id)
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun setupFilterButton() {
        findViewById<android.widget.LinearLayout>(R.id.btnFilter)?.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        FilterBottomSheetFragment(
            selectedProcessors = selectedProcessors,
            selectedPriceRanges = selectedPriceRanges,
            selectedSorts = selectedSorts,
            onApply = { processors, priceRanges, sorts ->
                selectedProcessors = processors.toMutableSet()
                selectedPriceRanges = priceRanges.toMutableSet()
                selectedSorts = sorts.toMutableSet()
                applyFilters(getCurrentQuery())
            }
        ).show(supportFragmentManager, "FilterBottomSheet")
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_cart -> { startActivity(Intent(this, CartActivity::class.java)); true }
                R.id.nav_orders -> { startActivity(Intent(this, OrdersActivity::class.java)); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun loadFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.let {
            if (!it.email.isNullOrEmpty()) it.email else it.uid
        } ?: return

        firestore.collection("users").document(userId).collection("favorites")
            .addSnapshotListener { snapshot, _ ->
                favoritedIds = snapshot?.documents?.map { it.id }?.toMutableSet() ?: mutableSetOf()
                adapter.setFavorites(favoritedIds)
            }
    }

    private fun toggleFavorite(computer: Computer) {
        val userId = FirebaseAuth.getInstance().currentUser?.let {
            if (!it.email.isNullOrEmpty()) it.email else it.uid
        } ?: return

        val favRef = firestore.collection("users").document(userId)
            .collection("favorites").document(computer.id)

        if (favoritedIds.contains(computer.id)) {
            favRef.delete()
        } else {
            favRef.set(mapOf(
                "brand" to computer.brand,
                "model" to computer.model,
                "price" to computer.price,
                "imageUrl" to computer.imageUrl,
                "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
        }
    }

    private fun observeNotificationCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.let {
            if (!it.email.isNullOrEmpty()) it.email else it.uid
        } ?: return

        firestore.collection("users").document(userId).collection("notifications")
            .addSnapshotListener { snapshot, _ ->
                personalUnreadCount = snapshot?.documents?.count {
                    it.getBoolean("isRead") == false
                } ?: 0
                updateBadge(personalUnreadCount + globalUnreadCount)
            }

        firestore.collection("globalNotifications")
            .addSnapshotListener { globalSnapshot, _ ->
                val globalIds = globalSnapshot?.documents?.map { it.id } ?: emptyList()
                firestore.collection("users").document(userId)
                    .collection("readGlobalNotifications")
                    .addSnapshotListener { readSnapshot, _ ->
                        val readIds = readSnapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                        globalUnreadCount = globalIds.count { !readIds.contains(it) }
                        updateBadge(personalUnreadCount + globalUnreadCount)
                    }
            }
    }

    private fun updateBadge(count: Int) {
        val badge = findViewById<TextView>(R.id.tvNotificationBadge)
        if (count > 0) {
            badge?.visibility = android.view.View.VISIBLE
            badge?.text = count.toString()
        } else {
            badge?.visibility = android.view.View.GONE
        }
    }

    private fun loadComputers() {
        firestore.collection("computers")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                allComputers = snapshot.documents.mapNotNull { doc ->
                    try {
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
                    } catch (e: Exception) { null }
                }
                applyFilters(getCurrentQuery())
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
                    when (proc) {
                        "Intel" -> computer.processor.contains("Intel", ignoreCase = true)
                        "Ryzen" -> computer.processor.contains("Ryzen", ignoreCase = true)
                        "Apple M" -> computer.processor.contains("Apple M", ignoreCase = true)
                        else -> computer.processor.contains(proc, ignoreCase = true)
                    }
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

    private fun getCurrentQuery(): String {
        return findViewById<android.widget.EditText>(R.id.etSearch)?.text?.toString() ?: ""
    }

    private fun addToCart(computer: Computer) {
        if (!computer.inStock || computer.quantity <= 0) {
            Toast.makeText(this, "⛔ Out of Stock", Toast.LENGTH_SHORT).show()
            return
        }
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
            Toast.makeText(this@HomeActivity, getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}