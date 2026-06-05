package com.ivan.compshop.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ivan.compshop.R

class FilterBottomSheetFragment(
    private val selectedProcessor: String,
    private val selectedPriceRange: String,
    private val selectedSort: String,
    private val onApply: (processor: String, priceRange: String, sort: String) -> Unit
) : BottomSheetDialogFragment() {

    private var currentProcessor = selectedProcessor
    private var currentPriceRange = selectedPriceRange
    private var currentSort = selectedSort

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProcessorFilters(view)
        setupPriceFilters(view)
        setupSortFilters(view)
        setupButtons(view)
    }

    private fun setupProcessorFilters(view: View) {
        val processors = listOf(
            view.findViewById<TextView>(R.id.procAll) to "All",
            view.findViewById<TextView>(R.id.procI5) to "Intel Core i5",
            view.findViewById<TextView>(R.id.procI7) to "Intel Core i7",
            view.findViewById<TextView>(R.id.procRyzen) to "Ryzen 5",
            view.findViewById<TextView>(R.id.procM3) to "Apple M3"
        )
        processors.forEach { (tv, value) ->
            updateFilterStyle(tv, value == currentProcessor)
            tv.setOnClickListener {
                currentProcessor = value
                processors.forEach { (t, v) -> updateFilterStyle(t, v == value) }
            }
        }
    }

    private fun setupPriceFilters(view: View) {
        val prices = listOf(
            view.findViewById<TextView>(R.id.priceAll) to "All",
            view.findViewById<TextView>(R.id.priceUnder1000) to "Under $1000",
            view.findViewById<TextView>(R.id.price1000to1500) to "$1000 - $1500",
            view.findViewById<TextView>(R.id.priceOver1500) to "Over $1500"
        )
        prices.forEach { (tv, value) ->
            updateFilterStyle(tv, value == currentPriceRange)
            tv.setOnClickListener {
                currentPriceRange = value
                prices.forEach { (t, v) -> updateFilterStyle(t, v == value) }
            }
        }
    }

    private fun setupSortFilters(view: View) {
        val sorts = listOf(
            view.findViewById<TextView>(R.id.sortDefault) to "Default",
            view.findViewById<TextView>(R.id.sortLowest) to "Lowest price",
            view.findViewById<TextView>(R.id.sortHighest) to "Highest price"
        )
        sorts.forEach { (tv, value) ->
            updateFilterStyle(tv, value == currentSort)
            tv.setOnClickListener {
                currentSort = value
                sorts.forEach { (t, v) -> updateFilterStyle(t, v == value) }
            }
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<TextView>(R.id.btnClear).setOnClickListener {
            currentProcessor = "All"
            currentPriceRange = "All"
            currentSort = "Default"
            onApply("All", "All", "Default")
            dismiss()
        }

        view.findViewById<TextView>(R.id.btnApply).setOnClickListener {
            onApply(currentProcessor, currentPriceRange, currentSort)
            dismiss()
        }
    }

    private fun updateFilterStyle(tv: TextView, isSelected: Boolean) {
        if (isSelected) {
            tv.setBackgroundResource(R.drawable.btn_neon_gradient)
            tv.setTextColor(android.graphics.Color.WHITE)
        } else {
            tv.setBackgroundResource(R.drawable.btn_social_neon)
            tv.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
        }
    }
}