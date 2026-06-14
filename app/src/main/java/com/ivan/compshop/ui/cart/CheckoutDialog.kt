package com.ivan.compshop.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmOrder)
        val btnCard = view.findViewById<TextView>(R.id.btnPayCard)
        val btnCash = view.findViewById<TextView>(R.id.btnPayCash)
        val btnStandard = view.findViewById<LinearLayout>(R.id.btnDeliveryStandard)
        val btnExpress = view.findViewById<LinearLayout>(R.id.btnDeliveryExpress)

        fun getDeliveryFee() = when (selectedDelivery) {
            "Express" -> 9.99
            else -> if (totalPrice >= 1000) 0.0 else 4.99
        }

        fun updateTotals() {
            val fee = getDeliveryFee()
            val days = if (selectedDelivery == "Express") "1-2 days" else "3-5 days"
            tvOrderTotal?.text = "Order total: $${"%.2f".format(totalPrice)}"
            tvDeliveryFee?.text = if (fee == 0.0) "Delivery: FREE 🎉" else "Delivery: $${"%.2f".format(fee)}"
            tvFinalTotal?.text = "Total: $${"%.2f".format(totalPrice + fee)}"
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
            val finalTotal = totalPrice + getDeliveryFee()
            if (selectedPayment == "Card") {
                showCardForm(finalTotal)
            } else {
                onConfirm(selectedPayment, selectedDelivery, finalTotal)
                dismiss()
            }
        }
    }

    private fun showCardForm(finalTotal: Double) {
        val dialog = android.app.AlertDialog.Builder(requireContext()).create()
        val cardView = layoutInflater.inflate(R.layout.dialog_card_payment, null)
        dialog.setView(cardView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnPay = cardView.findViewById<Button>(R.id.btnPay)
        val etCardNumber = cardView.findViewById<EditText>(R.id.etCardNumber)
        val etExpiry = cardView.findViewById<EditText>(R.id.etExpiry)
        val etCvv = cardView.findViewById<EditText>(R.id.etCvv)
        val etCardName = cardView.findViewById<EditText>(R.id.etCardName)

        etExpiry?.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return
                isFormatting = true
                val clean = s.toString().replace("/", "")
                val formatted = if (clean.length >= 2) {
                    "${clean.substring(0, 2)}/${clean.substring(2)}"
                } else clean
                etExpiry?.setText(formatted)
                etExpiry?.setSelection(formatted.length)
                isFormatting = false
            }
        })

        btnPay?.setOnClickListener {
            val cardNumber = etCardNumber?.text.toString().trim()
            val expiry = etExpiry?.text.toString().trim()
            val cvv = etCvv?.text.toString().trim()
            val name = etCardName?.text.toString().trim()

            if (cardNumber.length < 16 || expiry.isEmpty() || cvv.length < 3 || name.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all card details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            onConfirm("Card", selectedDelivery, finalTotal)
            dismiss()
        }

        dialog.show()
    }
}