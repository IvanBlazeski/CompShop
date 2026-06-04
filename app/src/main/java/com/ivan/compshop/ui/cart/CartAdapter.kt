package com.ivan.compshop.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ivan.compshop.data.local.CartItemEntity
import com.ivan.compshop.databinding.ItemCartBinding

class CartAdapter(
    private val onRemoveClick: (CartItemEntity) -> Unit
) : ListAdapter<CartItemEntity, CartAdapter.CartViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItemEntity) {
            binding.tvBrand.text = item.computerBrand
            binding.tvModel.text = item.computerName
            binding.tvPrice.text = "$${item.price}"

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivComputer)
            }

            binding.btnRemove.setOnClickListener { onRemoveClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CartItemEntity>() {
        override fun areItemsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity) =
            oldItem.computerId == newItem.computerId
        override fun areContentsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity) =
            oldItem == newItem
    }
}