package com.ivan.compshop.ui.detail

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.databinding.ActivityDetailBinding
import com.ivan.compshop.model.CartItem
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val app by lazy { application as CompShopApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back копче
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val computerId = intent.getStringExtra("computer_id") ?: return

        loadComputer(computerId)
    }

    private fun loadComputer(computerId: String) {
        lifecycleScope.launch {
            val computer = app.computerRepository.getComputerById(computerId)

            computer?.let {
                supportActionBar?.title = it.model

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

                binding.btnAddToCart.setOnClickListener {
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
                            this@DetailActivity,
                            getString(com.ivan.compshop.R.string.added_to_cart),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}