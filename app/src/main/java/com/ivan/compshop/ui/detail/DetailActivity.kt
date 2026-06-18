package com.ivan.compshop.ui.detail

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.R
import com.ivan.compshop.databinding.ActivityDetailBinding
import com.ivan.compshop.model.CartItem
import com.ivan.compshop.model.Computer
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val app by lazy { application as CompShopApplication }
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isFavorite = false
    private var currentQuantity = 1
    private var maxQuantity = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val computerId = intent.getStringExtra("computer_id") ?: return

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadComputer(computerId)
    }

    private fun loadComputer(computerId: String) {
        firestore.collection("computers")
            .document(computerId)
            .addSnapshotListener { doc, _ ->
                if (doc == null) return@addSnapshotListener

                val qty = (doc.getLong("quantity") ?: 0).toInt()
                val computer = Computer(
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

                binding.tvBrand.text = computer.brand
                binding.tvModel.text = computer.model
                binding.tvPrice.text = "$${computer.price}"
                binding.tvProcessor.text = computer.processor
                binding.tvRam.text = computer.ram
                binding.tvStorage.text = computer.storage
                binding.tvGraphics.text = computer.graphics
                binding.tvDescription.text = computer.description

                if (computer.imageUrl.isNotEmpty()) {
                    Glide.with(this@DetailActivity)
                        .load(computer.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.ivComputer)
                }

                // Favorites
                val userId = auth.currentUser?.let {
                    if (!it.email.isNullOrEmpty()) it.email else it.uid
                } ?: ""

                if (userId.isNotEmpty()) {
                    firestore.collection("users").document(userId)
                        .collection("favorites").document(computer.id)
                        .addSnapshotListener { favDoc, _ ->
                            isFavorite = favDoc?.exists() == true
                            binding.ivFavoriteDetail.setImageResource(
                                if (isFavorite) android.R.drawable.btn_star_big_on
                                else R.drawable.ic_favorite_border
                            )
                            binding.ivFavoriteDetail.setColorFilter(
                                if (isFavorite) android.graphics.Color.parseColor("#FF4081")
                                else android.graphics.Color.parseColor("#80FFFFFF")
                            )
                        }

                    binding.ivFavoriteDetail.setOnClickListener {
                        val favRef = firestore.collection("users").document(userId)
                            .collection("favorites").document(computer.id)
                        if (isFavorite) favRef.delete()
                        else favRef.set(mapOf(
                            "brand" to computer.brand,
                            "model" to computer.model,
                            "price" to computer.price,
                            "imageUrl" to computer.imageUrl,
                            "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ))
                    }
                }

                // Stock
                maxQuantity = qty
                if (qty <= 0) {
                    binding.btnAddToCart.isEnabled = false
                    binding.btnBuyNow.isEnabled = false
                    binding.btnAddToCart.text = "⛔ Out of Stock"
                    binding.btnAddToCart.alpha = 0.5f
                    binding.btnBuyNow.alpha = 0.5f
                    binding.btnMinus.isEnabled = false
                    binding.btnPlus.isEnabled = false
                } else {
                    binding.btnAddToCart.text = getString(R.string.add_to_cart)
                    binding.btnAddToCart.isEnabled = true
                    binding.btnBuyNow.isEnabled = true
                    binding.btnMinus.isEnabled = true
                    binding.btnPlus.isEnabled = true
                }

                binding.tvQuantity.text = currentQuantity.toString()

                binding.btnPlus.setOnClickListener {
                    if (currentQuantity < maxQuantity) {
                        currentQuantity++
                        binding.tvQuantity.text = currentQuantity.toString()
                    } else {
                        Toast.makeText(this@DetailActivity, "Max: $maxQuantity", Toast.LENGTH_SHORT).show()
                    }
                }

                binding.btnMinus.setOnClickListener {
                    if (currentQuantity > 1) {
                        currentQuantity--
                        binding.tvQuantity.text = currentQuantity.toString()
                    }
                }

                // Add to Cart со Guest check
                binding.btnAddToCart.setOnClickListener {
                    if (auth.currentUser?.isAnonymous == true) {
                        android.app.AlertDialog.Builder(this@DetailActivity)
                            .setTitle("Login Required")
                            .setMessage("You need to login or register to add items to cart.")
                            .setPositiveButton("Login") { _, _ ->
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                finishAffinity()
                                startActivity(android.content.Intent(this@DetailActivity, com.ivan.compshop.ui.auth.LoginActivity::class.java))
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                        return@setOnClickListener
                    }
                    lifecycleScope.launch {
                        val cartItem = CartItem(
                            computerId = computer.id,
                            computerName = computer.model,
                            computerBrand = computer.brand,
                            price = computer.price,
                            quantity = currentQuantity,
                            imageUrl = computer.imageUrl
                        )
                        app.cartRepository.addToCart(cartItem)
                        Toast.makeText(
                            this@DetailActivity,
                            "${currentQuantity}x ${computer.model} ${getString(R.string.added_to_cart)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                binding.btnBuyNow.setOnClickListener {
                    Toast.makeText(this@DetailActivity, "Proceeding to checkout...", Toast.LENGTH_SHORT).show()
                }
            }
    }
}