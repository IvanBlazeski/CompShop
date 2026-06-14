package com.ivan.compshop.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ivan.compshop.databinding.ItemOrderBinding
import com.ivan.compshop.R

data class OrderItem(
    val id: String = "",
    val date: String = "",
    val items: String = "",
    val totalPrice: Double = 0.0,
    val paymentMethod: String = "",
    val deliveryMethod: String = "",
    val status: String = "pending",
    val trackingStatus: String = "order_placed"
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
            binding.tvPaymentMethod.text = if (order.paymentMethod == "Card") "💳 Card" else "💵 Cash"
            binding.tvDeliveryMethod.text = if (order.deliveryMethod == "Express") "⚡ Express" else "📦 Standard"

            binding.tvOrderStatus.text = order.status.uppercase()
            binding.tvOrderStatus.setBackgroundResource(
                if (order.status == "paid") R.drawable.btn_neon_gradient
                else R.drawable.btn_social_neon
            )

            val steps = listOf(
                binding.step1Dot, binding.step2Dot, binding.step3Dot,
                binding.step4Dot, binding.step5Dot
            )
            val lines = listOf(
                binding.line1, binding.line2, binding.line3, binding.line4
            )

            val currentStep = when (order.trackingStatus) {
                "placed" -> 0
                "process" -> 1
                "shipped" -> 2
                "delivery" -> 3
                "done" -> 4
                else -> 0
            }

            steps.forEachIndexed { index, dot ->
                if (index <= currentStep) {
                    dot.setBackgroundResource(R.drawable.btn_neon_gradient)
                    dot.text = "✓"
                    dot.setTextColor(android.graphics.Color.WHITE)
                } else {
                    dot.setBackgroundResource(R.drawable.btn_social_neon)
                    dot.text = ""
                }
            }

            lines.forEachIndexed { index, line ->
                line.setBackgroundColor(
                    if (index < currentStep) android.graphics.Color.parseColor("#00D4FF")
                    else android.graphics.Color.parseColor("#1A00D4FF")
                )
            }

            // Delivered banner
            if (order.trackingStatus == "done") {
                binding.tvDelivered.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDelivered.visibility = android.view.View.GONE
            }
        }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem) = oldItem == newItem
    }
