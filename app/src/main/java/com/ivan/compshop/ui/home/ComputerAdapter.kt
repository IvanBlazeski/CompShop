package com.ivan.compshop.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ivan.compshop.R
import com.ivan.compshop.databinding.ItemComputerBinding
import com.ivan.compshop.model.Computer

class ComputerAdapter(
    private val onItemClick: (Computer) -> Unit,
    private val onAddToCart: (Computer) -> Unit,
    private val onFavoriteClick: (Computer) -> Unit
) : ListAdapter<Computer, ComputerAdapter.ComputerViewHolder>(DiffCallback()) {

    private val favorites = mutableSetOf<String>()

    fun setFavorites(ids: Set<String>) {
        favorites.clear()
        favorites.addAll(ids)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComputerViewHolder {
        val binding = ItemComputerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ComputerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ComputerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ComputerViewHolder(
        private val binding: ItemComputerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(computer: Computer) {
            binding.tvBrand.text = computer.brand
            binding.tvModel.text = computer.model
            binding.tvProcessor.text = computer.processor
            binding.tvPrice.text = "$${computer.price}"

            if (computer.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(computer.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivComputer)
            }

            if (!computer.inStock || computer.quantity <= 0) {
                binding.tvOutOfStock.visibility = View.VISIBLE
                binding.btnAddToCart.isEnabled = false
                binding.btnAddToCart.alpha = 0.4f
            } else {
                binding.tvOutOfStock.visibility = View.GONE
                binding.btnAddToCart.isEnabled = true
                binding.btnAddToCart.alpha = 1.0f
            }

            val isFav = favorites.contains(computer.id)
            binding.ivFavorite.setImageResource(
                if (isFav) android.R.drawable.btn_star_big_on
                else R.drawable.ic_favorite_border
            )
            binding.ivFavorite.setColorFilter(
                if (isFav) android.graphics.Color.parseColor("#FF4081")
                else android.graphics.Color.parseColor("#80FFFFFF")
            )
            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(computer)
            }

            binding.root.setOnClickListener { onItemClick(computer) }
            binding.btnAddToCart.setOnClickListener { onAddToCart(computer) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Computer>() {
        override fun areItemsTheSame(oldItem: Computer, newItem: Computer) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Computer, newItem: Computer) = oldItem == newItem
    }
}