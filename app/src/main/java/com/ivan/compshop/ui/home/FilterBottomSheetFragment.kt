package com.ivan.compshop.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ivan.compshop.R

class FilterBottomSheetFragment(
    private val selectedProcessors: Set<String>,
    private val selectedPriceRanges: Set<String>,
    private val selectedSorts: Set<String>,
    private val onApply: (processors: Set<String>, priceRanges: Set<String>, sorts: Set<String>) -> Unit
) : BottomSheetDialogFragment() {

    private val currentProcessors = selectedProcessors.toMutableSet()
    private val currentPriceRanges = selectedPriceRanges.toMutableSet()
    private val currentSorts = selectedSorts.toMutableSet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false)

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
            view.findViewById<TextView>(R.id.procIntel) to "Intel",
            view.findViewById<TextView>(R.id.procRyzen) to "Ryzen",
            view.findViewById<TextView>(R.id.procApple) to "Apple M"
        )

        processors.forEach { (tv, value) ->
            updateFilterStyle(tv, currentProcessors.contains(value))
            tv?.setOnClickListener {
                if (value == "All") {
                    currentProcessors.clear()
                    currentProcessors.add("All")
                    processors.forEach { (t, v) -> updateFilterStyle(t, v == "All") }
                } else {
                    currentProcessors.remove("All")
                    if (currentProcessors.contains(value)) {
                        currentProcessors.remove(value)
                        if (currentProcessors.isEmpty()) currentProcessors.add("All")
                    } else {
                        currentProcessors.add(value)
                    }
                    processors.forEach { (t, v) ->
                        updateFilterStyle(t, currentProcessors.contains(v))
                    }
                }
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
            updateFilterStyle(tv, currentPriceRanges.contains(value))
            tv?.setOnClickListener {
                if (value == "All") {
                    currentPriceRanges.clear()
                    currentPriceRanges.add("All")
                    prices.forEach { (t, v) -> updateFilterStyle(t, v == "All") }
                } else {
                    currentPriceRanges.remove("All")
                    if (currentPriceRanges.contains(value)) {
                        currentPriceRanges.remove(value)
                        if (currentPriceRanges.isEmpty()) currentPriceRanges.add("All")
                    } else {
                        currentPriceRanges.add(value)
                    }
                    prices.forEach { (t, v) ->
                        updateFilterStyle(t, currentPriceRanges.contains(v))
                    }
                }
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
            updateFilterStyle(tv, currentSorts.contains(value))
            tv?.setOnClickListener {
                currentSorts.clear()
                currentSorts.add(value)
                sorts.forEach { (t, v) -> updateFilterStyle(t, v == value) }
            }
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<TextView>(R.id.btnClear)?.setOnClickListener {
            onApply(setOf("All"), setOf("All"), setOf("Default"))
            dismiss()
        }
        view.findViewById<TextView>(R.id.btnApply)?.setOnClickListener {
            onApply(currentProcessors, currentPriceRanges, currentSorts)
            dismiss()
        }
    }

    private fun updateFilterStyle(tv: TextView?, isSelected: Boolean) {
        if (isSelected) {
            tv?.setBackgroundResource(R.drawable.btn_neon_gradient)
            tv?.setTextColor(android.graphics.Color.WHITE)
        } else {
            tv?.setBackgroundResource(R.drawable.btn_social_neon)
            tv?.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
        }
    }
}