package org.expensetracker

import kotlinx.serialization.ExperimentalSerializationApi

object ExpenseSummaryService {

    fun getSummary(id: Long) = FirebaseRepository.getSummaryById(id)

    @ExperimentalSerializationApi
    fun addAmount(id: Long, amountToAdd: Long): Summary? =
        FirebaseRepository.getSummaryById(id)
            ?.addAmount(amountToAdd)
            ?.let { FirebaseRepository.updateSummary(id, it) }

    private fun Summary.addAmount(amount: Long) = copy(
        amountInCents = this.amountInCents + amount
    )
}
