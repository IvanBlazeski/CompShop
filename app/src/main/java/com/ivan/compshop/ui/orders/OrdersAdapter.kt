package com.ivan.compshop.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivan.compshop.databinding.ItemOrderBinding

data class OrderItem(
    val id: String = "",
    val date: String = "",
    val items: String = "",
    val totalPrice: Double = 0.0,
    val paymentMethod: String = "",
    val deliveryMethod: String = "",
    val status: String = "pending"
)

class OrdersAdapter : ListAdapter<OrderItem, OrdersAdapter.OrderViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderItem) {
            binding.tvOrderDate.text = order.date
            binding.tvOrderItems.text = order.items
            binding.tvOrderTotal.text = "$${"%.2f".format(order.totalPrice)}"
            binding.tvPaymentMethod.text = if (order.paymentMethod == "Card") "💳 Card" else "💵 Cash on delivery"

            binding.tvOrderStatus.text = order.status.uppercase()
            binding.tvOrderStatus.setBackgroundResource(
                if (order.status == "paid") com.ivan.compshop.R.drawable.btn_neon_gradient
                else com.ivan.compshop.R.drawable.btn_social_neon
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem) = oldItem == newItem
    }
}