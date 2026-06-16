package com.ivan.compshop.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.ivan.compshop.R

class CardPaymentDialog(
    private val amount: Double,
    private val onPaymentSuccess: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_card_payment, container, false)

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

        val etCardNumber = view.findViewById<EditText>(R.id.etCardNumber)
        val etCardName = view.findViewById<EditText>(R.id.etCardName)
        val etExpiry = view.findViewById<EditText>(R.id.etExpiry)
        val etCvv = view.findViewById<EditText>(R.id.etCvv)
        val btnPay = view.findViewById<Button>(R.id.btnPay)

        etExpiry.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val input = s.toString().replace("/", "")
                if (input.length == 2 && !s.toString().contains("/")) {
                    s.append("/")
                }

                isFormatting = false
            }
        })

        btnPay.setOnClickListener {
            val cardNum = etCardNumber.text.toString().trim()
            val cardName = etCardName.text.toString().trim()
            val expiry = etExpiry.text.toString().trim()
            val cvv = etCvv.text.toString().trim()

            if (cardNum.length < 16 || cardName.isEmpty() || expiry.length < 5 || cvv.length < 3) {
                Toast.makeText(requireContext(), "Please fill all card details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Payment of $${"%.2f".format(amount)} successful! ✅", Toast.LENGTH_SHORT).show()
            onPaymentSuccess()
            dismiss()
        }
    }
}