package com.ivan.compshop.ui.cart

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ivan.compshop.R

class CheckoutDialog(
    private val totalPrice: Double,
    private val onConfirm: (paymentMethod: String, deliveryMethod: String, finalTotal: Double) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedPayment = "Card"
    private var selectedDelivery = "Standard"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_checkout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvOrderTotal = view.findViewById<TextView>(R.id.tvOrderTotal)
        val tvDeliveryFee = view.findViewById<TextView>(R.id.tvDeliveryFee)
        val tvFinalTotal = view.findViewById<TextView>(R.id.tvFinalTotal)
        val tvDeliveryDays = view.findViewById<TextView>(R.id.tvDeliveryDays)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirmOrder)
        val btnCard = view.findViewById<TextView>(R.id.btnPayCard)
        val btnCash = view.findViewById<TextView>(R.id.btnPayCash)
        val btnStandard = view.findViewById<android.widget.LinearLayout>(R.id.btnDeliveryStandard)
        val btnExpress = view.findViewById<android.widget.LinearLayout>(R.id.btnDeliveryExpress)

        fun updateTotals() {
            val deliveryFee = when (selectedDelivery) {
                "Express" -> 9.99
                "Standard" -> if (totalPrice >= 1000) 0.0 else 4.99
                else -> 0.0
            }
            val days = if (selectedDelivery == "Express") "1-2 days" else "3-5 days"
            val finalTotal = totalPrice + deliveryFee
            tvOrderTotal?.text = "Order total: $${"%.2f".format(totalPrice)}"
            tvDeliveryFee?.text = if (deliveryFee == 0.0) "Delivery: FREE 🎉" else "Delivery: $${"%.2f".format(deliveryFee)}"
            tvFinalTotal?.text = "Total: $${"%.2f".format(finalTotal)}"
            tvDeliveryDays?.text = "Estimated delivery: $days"
        }

        fun updatePaymentStyle() {
            btnCard?.setBackgroundResource(if (selectedPayment == "Card") R.drawable.btn_neon_gradient else R.drawable.btn_social_neon)
            btnCash?.setBackgroundResource(if (selectedPayment == "Cash on delivery") R.drawable.btn_neon_gradient else R.drawable.btn_social_neon)
        }

        fun updateDeliveryStyle() {
            btnStandard?.setBackgroundResource(if (selectedDelivery == "Standard") R.drawable.btn_neon_gradient else R.drawable.btn_social_neon)
            btnExpress?.setBackgroundResource(if (selectedDelivery == "Express") R.drawable.btn_neon_gradient else R.drawable.btn_social_neon)
        }

        updateTotals()
        updatePaymentStyle()
        updateDeliveryStyle()

        btnCard?.setOnClickListener { selectedPayment = "Card"; updatePaymentStyle() }
        btnCash?.setOnClickListener { selectedPayment = "Cash on delivery"; updatePaymentStyle() }
        btnStandard?.setOnClickListener { selectedDelivery = "Standard"; updateTotals(); updateDeliveryStyle() }
        btnExpress?.setOnClickListener { selectedDelivery = "Express"; updateTotals(); updateDeliveryStyle() }

        btnConfirm?.setOnClickListener {
            val deliveryFee = when (selectedDelivery) {
                "Express" -> 9.99
                "Standard" -> if (totalPrice >= 1000) 0.0 else 4.99
                else -> 0.0
            }
            onConfirm(selectedPayment, selectedDelivery, totalPrice + deliveryFee)
            dismiss()
        }
    }
}