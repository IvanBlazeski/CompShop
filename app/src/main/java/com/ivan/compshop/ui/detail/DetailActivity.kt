package com.ivan.compshop.ui.detail

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.R
import com.ivan.compshop.databinding.ActivityDetailBinding
import com.ivan.compshop.model.CartItem
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val app by lazy { application as CompShopApplication }
    private var isFavorite = false
    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val computerId = intent.getStringExtra("computer_id") ?: return

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupQuantitySelector()
        loadComputer(computerId)
    }

    private fun setupQuantitySelector() {
        binding.tvQuantity.text = quantity.toString()

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
            }
        }

        binding.btnPlus.setOnClickListener {
            quantity++
            binding.tvQuantity.text = quantity.toString()
        }
    }

    private fun loadComputer(computerId: String) {
        lifecycleScope.launch {
            val computer = app.computerRepository.getComputerById(computerId)

            computer?.let {
                binding.tvBrand.text = it.brand
                binding.tvModel.text = it.model
                binding.tvPrice.text = "$${it.price}"
                binding.tvProcessor.text = it.processor
                binding.tvRam.text = it.ram
                binding.tvStorage.text = it.storage
                binding.tvGraphics.text = it.graphics
                binding.tvDescription.text = it.description

                if (it.imageUrl.isNotEmpty()) {
                    Glide.with(this@DetailActivity)
                        .load(it.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.ivComputer)
                }

                // Favorite
                binding.ivFavoriteDetail.setOnClickListener { _ ->
                    isFavorite = !isFavorite
                    binding.ivFavoriteDetail.setImageResource(
                        if (isFavorite) android.R.drawable.btn_star_big_on
                        else R.drawable.ic_favorite_border
                    )
                    binding.ivFavoriteDetail.setColorFilter(
                        if (isFavorite) android.graphics.Color.parseColor("#FF4081")
                        else android.graphics.Color.parseColor("#80FFFFFF")
                    )
                }

                // Add to Cart
                binding.btnAddToCart.setOnClickListener {
                    lifecycleScope.launch {
                        repeat(quantity) {
                            val cartItem = CartItem(
                                computerId = computer.id,
                                computerName = computer.model,
                                computerBrand = computer.brand,
                                price = computer.price,
                                quantity = 1,
                                imageUrl = computer.imageUrl
                            )
                            app.cartRepository.addToCart(cartItem)
                        }
                        Toast.makeText(
                            this@DetailActivity,
                            "${quantity}x ${computer.model} ${getString(R.string.added_to_cart)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Buy Now
                binding.btnBuyNow.setOnClickListener {
                    Toast.makeText(
                        this@DetailActivity,
                        "Proceeding to checkout...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}