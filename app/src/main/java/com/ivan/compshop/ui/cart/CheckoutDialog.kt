package com.ivan.compshop.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.ivan.compshop.R

class CheckoutDialog(
    private val orderTotal: Double,
    private val onConfirm: (paymentMethod: String, deliveryMethod: String, finalTotal: Double, address: String) -> Unit
) : DialogFragment() {

    private var selectedPayment = "Card"
    private var selectedDelivery = "Standard"
    private var discountPercent = 0.0

    // Валидни купони
    private val validCoupons = mapOf(
        "COMPSHOP10" to 10.0,
        "SAVE20" to 20.0,
        "WELCOME15" to 15.0
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_checkout, container, false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPayCard = view.findViewById<TextView>(R.id.btnPayCard)
        val btnPayCash = view.findViewById<TextView>(R.id.btnPayCash)
        val btnDeliveryStandard = view.findViewById<ViewGroup>(R.id.btnDeliveryStandard)
        val btnDeliveryExpress = view.findViewById<ViewGroup>(R.id.btnDeliveryExpress)
        val tvOrderTotal = view.findViewById<TextView>(R.id.tvOrderTotal)
        val tvDeliveryFee = view.findViewById<TextView>(R.id.tvDeliveryFee)
        val tvDeliveryDays = view.findViewById<TextView>(R.id.tvDeliveryDays)
        val tvDiscount = view.findViewById<TextView>(R.id.tvDiscount)
        val tvFinalTotal = view.findViewById<TextView>(R.id.tvFinalTotal)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmOrder)
        val etDeliveryAddress = view.findViewById<EditText>(R.id.etDeliveryAddress)
        val etCouponCode = view.findViewById<EditText>(R.id.etCouponCode)
        val btnApplyCoupon = view.findViewById<TextView>(R.id.btnApplyCoupon)
        val etContactPhone = view.findViewById<EditText>(R.id.etContactPhone)

        tvOrderTotal.text = "Order total: $${"%.2f".format(orderTotal)}"

        fun updateTotal() {
            val deliveryFee = if (selectedDelivery == "Express") 9.99 else if (orderTotal >= 1000) 0.0 else 0.0
            val discount = orderTotal * (discountPercent / 100)
            val finalTotal = orderTotal + deliveryFee - discount

            tvDeliveryFee.text = "Delivery: $${"%.2f".format(deliveryFee)}"
            tvDeliveryDays.text = if (selectedDelivery == "Express") "Estimated delivery: 1-2 days"
            else "Estimated delivery: 3-5 days"

            if (discountPercent > 0) {
                tvDiscount.visibility = View.VISIBLE
                tvDiscount.text = "Discount (${discountPercent.toInt()}%): -$${"%.2f".format(discount)}"
            } else {
                tvDiscount.visibility = View.GONE
            }

            tvFinalTotal.text = "Total: $${"%.2f".format(finalTotal)}"
        }

        updateTotal()

        // Payment
        btnPayCard.setOnClickListener {
            selectedPayment = "Card"
            btnPayCard.setBackgroundResource(R.drawable.btn_neon_gradient)
            btnPayCash.setBackgroundResource(R.drawable.btn_social_neon)
        }

        btnPayCash.setOnClickListener {
            selectedPayment = "Cash on delivery"
            btnPayCash.setBackgroundResource(R.drawable.btn_neon_gradient)
            btnPayCard.setBackgroundResource(R.drawable.btn_social_neon)
        }

        // Delivery
        btnDeliveryStandard.setOnClickListener {
            selectedDelivery = "Standard"
            btnDeliveryStandard.setBackgroundResource(R.drawable.btn_neon_gradient)
            btnDeliveryExpress.setBackgroundResource(R.drawable.btn_social_neon)
            updateTotal()
        }

        btnDeliveryExpress.setOnClickListener {
            selectedDelivery = "Express"
            btnDeliveryExpress.setBackgroundResource(R.drawable.btn_neon_gradient)
            btnDeliveryStandard.setBackgroundResource(R.drawable.btn_social_neon)
            updateTotal()
        }

        // Coupon
        btnApplyCoupon.setOnClickListener {
            val code = etCouponCode.text.toString().trim().uppercase()
            val discount = validCoupons[code]
            if (discount != null) {
                discountPercent = discount
                Toast.makeText(requireContext(), getString(R.string.coupon_applied), Toast.LENGTH_SHORT).show()
                updateTotal()
            } else {
                Toast.makeText(requireContext(), getString(R.string.coupon_invalid), Toast.LENGTH_SHORT).show()
            }
        }

        // Confirm
        btnConfirm.setOnClickListener {
            val address = etDeliveryAddress.text.toString().trim()
            val phone = etContactPhone.text.toString().trim()

            if (address.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_address), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_phone), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deliveryFee = if (selectedDelivery == "Express") 9.99 else if (orderTotal >= 1000) 0.0 else 0.0
            val discount = orderTotal * (discountPercent / 100)
            val finalTotal = orderTotal + deliveryFee - discount

            if (selectedPayment == "Card") {
                val cardDialog = CardPaymentDialog(finalTotal) {
                    onConfirm(selectedPayment, selectedDelivery, finalTotal, "$address | $phone")
                    dismiss()
                }
                cardDialog.show(parentFragmentManager, "CardPaymentDialog")
            } else {
                onConfirm(selectedPayment, selectedDelivery, finalTotal, "$address | $phone")
                dismiss()
            }
        }
    }
}